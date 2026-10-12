import axios, {type AxiosError, type InternalAxiosRequestConfig} from 'axios';
import {getServerErrorCode} from '@/shared/services/errors';

interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
    _retry?: boolean;
}

// Relative on purpose, and deliberately not configurable. The API is reached
// through whatever origin served this page: nginx reverse-proxies /api/ to the
// backend in production (client/nginx.conf), and vite proxies the same prefix in
// development (vite.config.ts). An absolute URL baked in at build time would ship
// the developer's machine to every visitor, and pointing the SPA straight at the
// backend would also break the rate limiter's IP trust, which holds only while
// nginx is the single path in.
const API_BASE_URL = "/api";
export const SESSION_ENDED_EVENT = 'krino:session-ended';

const XSRF_COOKIE_NAME = 'XSRF-TOKEN';
const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    // Echo the XSRF-TOKEN cookie back in the X-XSRF-TOKEN header. Requests are
    // same-origin now that the API is proxied under /api, so axios would do this
    // on its own; stated explicitly so a future split-origin deployment does not
    // silently stop sending the header.
    withXSRFToken: true,
    xsrfCookieName: XSRF_COOKIE_NAME,
    xsrfHeaderName: XSRF_HEADER_NAME,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

const readCookie = (name: string): string | null => {
    const pattern = new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') + '=([^;]*)');
    const match = document.cookie.match(pattern);
    return match ? decodeURIComponent(match[1]) : null;
};

let csrfPromise: Promise<void> | null = null;

// The server seeds the XSRF-TOKEN cookie on GET /auth/csrf. Until that cookie
// exists there is nothing for axios to echo in the header, so make sure it is
// present before issuing any state-changing request (login, register, ...).
const ensureCsrfToken = async (): Promise<void> => {
    if (readCookie(XSRF_COOKIE_NAME)) return;
    if (!csrfPromise) {
        csrfPromise = axios
            .get(`${API_BASE_URL}/auth/csrf`, {withCredentials: true})
            .then(() => undefined)
            .finally(() => {
                csrfPromise = null;
            });
    }
    await csrfPromise;
};

const MUTATING_METHODS = new Set(['post', 'put', 'patch', 'delete']);

// 'ok'           -> refreshed successfully
// 'unauthorized' -> the refresh token itself is invalid/expired: session is over
// 'error'        -> transient failure (network blip, timeout, 5xx, waking from
//                   sleep): the session is still valid and must NOT be ended
type RefreshResult = 'ok' | 'unauthorized' | 'error';

let refreshTimer: ReturnType<typeof setTimeout> | null = null;
let refreshPromise: Promise<RefreshResult> | null = null;

// Refresh shortly before the 15-minute access token expires.
const REFRESH_TIME = 14 * 60 * 1000 + 500;
// After a transient failure, retry soon instead of giving up on the session.
const RETRY_TIME = 30 * 1000;

const refreshTokens = async (): Promise<RefreshResult> => {
    try {
        await ensureCsrfToken();
        await axios.post(`${API_BASE_URL}/auth/refresh`, {}, {
            withCredentials: true,
            withXSRFToken: true,
            xsrfCookieName: XSRF_COOKIE_NAME,
            xsrfHeaderName: XSRF_HEADER_NAME,
        });
        return 'ok';
    } catch (error) {
        const status = axios.isAxiosError(error) ? error.response?.status : undefined;
        return status === 401 || status === 403 ? 'unauthorized' : 'error';
    }
};

// Share a single in-flight refresh so concurrent callers (the proactive timer
// and the 401 interceptor) can't rotate the single-use refresh token twice.
const runRefresh = (): Promise<RefreshResult> => {
    refreshPromise ??= refreshTokens().finally(() => {
        refreshPromise = null;
    });
    return refreshPromise;
};

export const clearTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }
};

// Routes meant to be viewed while logged out: the login/registration screens, the
// emailed password-reset and email-verification links, and the public catalogue —
// both the job listing and the department directory beside it.
// The on-load session probe (GET /users/me -> refresh) 401s for a logged-out visitor,
// but on these pages that must NOT bounce them to /login. The verify-email page in
// particular is only ever reached by users who are, by definition, not signed in yet,
// so the bounce made email verification impossible to complete. Protected pages are
// still guarded by ProtectedRoute, so scoping the redirect out here loses no protection.
const PUBLIC_PATHS = new Set(['/', '/login', '/register', '/forgot-password', '/reset-password',
    '/verify-email', '/departments']);

// The catalogue is public at /offers and /offers/{id}, but the staff pages nested
// under the same prefix (/offers/new, /offers/{id}/edit) are not: a session that dies
// there should still send the user to the login screen, so they are matched out
// rather than swept in by a prefix test.
const isPublicOfferPath = (pathname: string): boolean => {
    if (pathname === '/offers') return true;
    const detail = /^\/offers\/([^/]+)$/.exec(pathname);
    return detail !== null && detail[1] !== 'new';
};

const isPublicPath = (pathname: string): boolean =>
    PUBLIC_PATHS.has(pathname) || isPublicOfferPath(pathname);

const redirectToLogin = () => {
    clearTokenRefresh();
    window.dispatchEvent(new Event(SESSION_ENDED_EVENT));
    if (!isPublicPath(window.location.pathname)) {
        window.location.href = '/login';
    }
};

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as ExtendedAxiosRequestConfig | undefined;

        // An incorrect current password is a form error, not an expired session. Refreshing
        // it would revoke a restricted session before the user can correct their input.
        if (error.response?.status === 401 && getServerErrorCode(error) !== 'INVALID_CREDENTIALS'
            && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            const result = await runRefresh();
            // A query cancelled during logout must not retry or end the next session.
            if (originalRequest.signal?.aborted) {
                return Promise.reject(error);
            }
            if (result === 'ok') {
                return api(originalRequest);
            }
            // Only end the session when the refresh token itself is rejected;
            // transient failures bubble up without logging the user out.
            if (result === 'unauthorized') {
                redirectToLogin();
            }
        }

        return Promise.reject(error);
    }
);

api.interceptors.request.use(
    async (config) => {
        if (config.method && MUTATING_METHODS.has(config.method.toLowerCase())) {
            await ensureCsrfToken();
        }
        return config;
    },
    (error) => Promise.reject(error)
);

export const setupTokenRefresh = () => {
    clearTokenRefresh();

    const performProactiveRefresh = async () => {
        const result = await runRefresh();

        if (result === 'unauthorized') {
            // The refresh token is gone — the session genuinely ended.
            redirectToLogin();
            return;
        }

        // 'ok' -> normal cadence; 'error' -> transient, keep the session and
        // retry shortly. We never proactively log the user out on a blip.
        refreshTimer = setTimeout(performProactiveRefresh, result === 'ok' ? REFRESH_TIME : RETRY_TIME);
    };

    refreshTimer = setTimeout(performProactiveRefresh, REFRESH_TIME);
};

export default api;
export {API_BASE_URL};
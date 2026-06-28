import axios, {type AxiosError, type InternalAxiosRequestConfig} from 'axios';

interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
    _retry?: boolean;
}

const API_BASE_URL = "http://localhost:8080/api";

const XSRF_COOKIE_NAME = 'XSRF-TOKEN';
const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    // Echo the XSRF-TOKEN cookie back in the X-XSRF-TOKEN header. axios only does
    // this automatically for same-origin requests; the API is cross-origin
    // (localhost:5000 -> localhost:8080), so we force it on with withXSRFToken.
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

const MUTATING_METHODS = ['post', 'put', 'patch', 'delete'];

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
    if (!refreshPromise) {
        refreshPromise = refreshTokens().finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
};

export const clearTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }
};

const redirectToLogin = () => {
    clearTokenRefresh();
    if (window.location.pathname !== '/login') {
        window.location.href = '/login';
    }
};

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as ExtendedAxiosRequestConfig | undefined;

        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            const result = await runRefresh();
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
        if (config.method && MUTATING_METHODS.includes(config.method.toLowerCase())) {
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
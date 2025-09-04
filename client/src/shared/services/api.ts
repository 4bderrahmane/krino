import axios, {type AxiosError, type InternalAxiosRequestConfig} from 'axios';

interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
    _retry?: boolean;
}

const API_BASE_URL = "http://localhost:8080/api";

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

const REFRESH_TIME = 14 * 60 * 1000;

const refreshTokens = async (): Promise<boolean> => {
    try {
        await axios.post(`${API_BASE_URL}/auth/refresh`, {}, {
            withCredentials: true
        });
        console.log("Token refreshed successfully via API client");
        return true;
    } catch (error) {
        console.error("Token refresh failed in API client:", error);
        return false;
    }
};

api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as ExtendedAxiosRequestConfig;

        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            if (isRefreshing) {
                try {
                    await refreshPromise;
                    return api(originalRequest);
                } catch (refreshError) {
                    console.error("Failed to refresh token during retry:", refreshError);
                    return Promise.reject(error);
                }
            }

            isRefreshing = true;
            refreshPromise = refreshTokens();

            try {
                const refreshSuccess = await refreshPromise;

                if (refreshSuccess) {
                    return api(originalRequest);
                } else {
                    window.location.href = '/login';
                    return Promise.reject(error);
                }
            } catch (refreshError) {
                console.error("Failed to refresh token during retry:", refreshError);
                window.location.href = '/login';
                return Promise.reject(error);
            } finally {
                isRefreshing = false;
                refreshPromise = null;
            }
        }

        return Promise.reject(error);
    }
);

api.interceptors.request.use(
    (config) => {

        console.log(`Making ${config.method?.toUpperCase()} request to ${config.url}`);
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export const setupTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
    }

    const performProactiveRefresh = async () => {
        try {
            await api.post('/auth/refresh');
            console.log("Proactive token refresh successful");
            refreshTimer = setTimeout(performProactiveRefresh, REFRESH_TIME);
        } catch (error) {
            console.error('Proactive token refresh failed:', error);
            refreshTimer = null;
            window.location.href = '/login';
        }
    };

    refreshTimer = setTimeout(performProactiveRefresh, REFRESH_TIME);
    console.log("Token refresh timer started - will refresh in 14 minutes");
};

export const clearTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
        console.log("Token refresh timer cleared");
    }
};

export default api;
export {API_BASE_URL};
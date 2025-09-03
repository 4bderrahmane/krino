import api from '../../../shared/services/api.ts';
import axios from 'axios';
import type {
    UserLoginDTO,
    UserRegistrationDTO,
    UserResponseDTO,
    BackendErrorResponse,
    AuthErrorCode,
    EnhancedError,
    AuthResponse
} from "../types/api.types";

const AUTH_ENDPOINTS = {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    ME: '/auth/me'
} as const;

let refreshTimer: ReturnType<typeof setTimeout> | null = null;

export const setupTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
    }

    const refreshTime = 14 * 60 * 1000;

    const performProactiveRefresh = async () => {
        try {
            await api.post(AUTH_ENDPOINTS.REFRESH);
            console.log("Proactive token refresh successful");
            refreshTimer = setTimeout(performProactiveRefresh, refreshTime);
        } catch (error) {
            console.error('Proactive token refresh failed:', error);
            refreshTimer = null;
            window.location.href = '/login';
        }
    };

    refreshTimer = setTimeout(performProactiveRefresh, refreshTime);
    console.log("Token refresh timer started - will refresh in 14 minutes");
};

export const clearTokenRefresh = () => {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
        console.log("Token refresh timer cleared");
    }
};

const extractErrorCode = (error: unknown): AuthErrorCode => {
    if (axios.isAxiosError(error) && error.response?.data) {
        const errorData = error.response.data as BackendErrorResponse;
        if (errorData.errorCode) {
            return errorData.errorCode as AuthErrorCode;
        }
    }
    return 'UNEXPECTED_ERROR';
};

export const login = async (credentials: UserLoginDTO): Promise<AuthResponse> => {
    try {
        const response = await api.post<AuthResponse>(AUTH_ENDPOINTS.LOGIN, credentials);
        setupTokenRefresh();
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Login failed:", error.response?.data || error.message);

            const errorCode = extractErrorCode(error);
            const backendError = error.response?.data as BackendErrorResponse;

            const enhancedError = new Error('Login failed') as EnhancedError;
            enhancedError.errorCode = errorCode;
            enhancedError.backendError = backendError;
            throw enhancedError;
        } else {
            console.error("An unexpected error occurred during login:", error);
        }
        throw error;
    }
};

export const register = async (userData: UserRegistrationDTO): Promise<UserResponseDTO> => {
    try {
        const response = await api.post<UserResponseDTO>(AUTH_ENDPOINTS.REGISTER, userData);
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Registration failed:", error.response?.data || error.message);

            const errorCode = extractErrorCode(error);
            const backendError = error.response?.data as BackendErrorResponse;

            const enhancedError = new Error('Registration failed') as EnhancedError;
            enhancedError.errorCode = errorCode;
            enhancedError.backendError = backendError;
            throw enhancedError;
        } else {
            console.error("An unexpected error occurred during registration:", error);
        }
        throw error;
    }
};

export const logout = async (): Promise<void> => {
    try {
        clearTokenRefresh();
        await api.post(AUTH_ENDPOINTS.LOGOUT);
        console.log("Logged out successfully.");
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Logout failed:", error.response?.data || error.message);
        } else {
            console.error("An unexpected error occurred during logout:", error);
        }
        throw error;
    }
};

export const checkAuthStatus = async (): Promise<UserResponseDTO | null> => {
    try {
        const response = await api.get<{ user: UserResponseDTO }>(AUTH_ENDPOINTS.ME);
        return response.data.user;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Auth status check failed:", error.response?.data || error.message);
        } else {
            console.error("An unexpected error occurred during auth status check:", error);
        }
        return null;
    }
};

export {api};
export default api;

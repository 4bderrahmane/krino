import api, { setupTokenRefresh, clearTokenRefresh } from '@/shared/services/api.ts';
import axios from 'axios';
import {getServerErrorCode} from '@/shared/services/errors.ts';
import type {
    UserLoginDTO,
    UserRegistrationDTO,
    UserResponseDTO,
    BackendErrorResponse,
    EnhancedError,
    AuthResponse
} from "@/features/authentication/types/api.types";

const AUTH_ENDPOINTS = {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    ME: '/users/me'
} as const;

// Wrap an axios failure in an Error that carries the backend's error code, so
// callers keep the code (and raw ProblemDetail) after the axios error is gone.
const enhanceError = (message: string, error: unknown): EnhancedError => {
    const enhancedError = new Error(message) as EnhancedError;
    enhancedError.errorCode = getServerErrorCode(error) ?? undefined;
    if (axios.isAxiosError(error)) {
        enhancedError.backendError = error.response?.data as BackendErrorResponse;
    }
    return enhancedError;
};

export const login = async (credentials: UserLoginDTO): Promise<AuthResponse> => {
    try {
        const response = await api.post<AuthResponse>(AUTH_ENDPOINTS.LOGIN, credentials);
        setupTokenRefresh();
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Login failed:", error.response?.data || error.message);
            throw enhanceError('Login failed', error);
        }
        console.error("An unexpected error occurred during login:", error);
        throw error;
    }
};

export const register = async (userData: UserRegistrationDTO, resume: File): Promise<UserResponseDTO> => {
    try {
        // The register endpoint is multipart: a JSON `data` part plus the required CV PDF.
        // The `data` part must be application/json so the backend binds it to the DTO.
        const formData = new FormData();
        formData.append('data', new Blob([JSON.stringify(userData)], {type: 'application/json'}));
        formData.append('resume', resume);

        // Override the api instance's default application/json header so axios sets the
        // multipart/form-data content-type with the correct boundary.
        const response = await api.post<UserResponseDTO>(AUTH_ENDPOINTS.REGISTER, formData, {
            headers: {'Content-Type': undefined},
        });
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Registration failed:", error.response?.data || error.message);
            throw enhanceError('Registration failed', error);
        }
        console.error("An unexpected error occurred during registration:", error);
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
        const response = await api.get<UserResponseDTO>(AUTH_ENDPOINTS.ME);
        return response.data;
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

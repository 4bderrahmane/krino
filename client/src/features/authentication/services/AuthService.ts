import axios, {type AxiosResponse, type AxiosError} from 'axios';
import type {
    UserLoginDTO, UserRegistrationDTO, UserResponseDTO, LoginResponse, BackendErrorResponse, AuthErrorCode,
    EnhancedError
} from "../types/api.types";

const API_URL = "http://localhost:8080/api/auth";


const extractErrorCode = (error: AxiosError | Error): AuthErrorCode => {
    if (axios.isAxiosError(error) && error.response?.data) {
        const errorData = error.response.data as BackendErrorResponse;

        if (errorData.errorCode) {
            return errorData.errorCode as AuthErrorCode;
        }
    }

    return 'UNEXPECTED_ERROR';
};

export const login = async (credentials: UserLoginDTO): Promise<LoginResponse> => {
    try {
        const response: AxiosResponse<LoginResponse> = await axios.post(`${API_URL}/login`, credentials);
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
        const response: AxiosResponse<UserResponseDTO> = await axios.post(`${API_URL}/register`, userData);
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
        await axios.post(`${API_URL}/logout`);
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

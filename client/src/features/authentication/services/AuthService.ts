import axios, {type AxiosResponse} from 'axios';
import type {UserLoginDTO, UserRegistrationDTO,UserResponseDTO ,LoginResponse} from "../types/api.types";

const API_URL = "http://localhost:8080/api/auth";

export const login = async (credentials: UserLoginDTO): Promise<LoginResponse> => {
    try {
        const response: AxiosResponse<LoginResponse> = await axios.post(`${API_URL}/login`, credentials);
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error("Login failed:", error.response?.data || error.message);
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

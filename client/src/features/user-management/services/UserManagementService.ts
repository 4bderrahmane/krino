import axios from "axios";
import type {UserResponseDTO} from "../../authentication/types/api.types.ts";

const API_URL = "http://localhost:8080/api/users";

export const login = async (): Promise<UserResponseDTO[]> => {
    try {
        const response = await axios.get(`${API_URL}/login`);
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)) {
            console.error('Error fetching users:', error.message);
            throw new Error(error.response?.data || 'Failed to fetch users');
        } else {
            console.error('An unexpected error occurred:', error);
            throw new Error('An unexpected error occurred');
        }
    }
};

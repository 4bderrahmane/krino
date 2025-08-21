import axios from "axios";
import type {UserLoginDTO, UserRegistrationDTO,UserResponseDTO ,LoginResponse} from "../types/api.types";

const API_URL = "http://localhost:8080/api/auth";

export const login = async (credentials: UserLoginDTO): Promise<LoginResponse> => {
    const response = await axios.post(`${API_URL}/login`, credentials);
    return response.data;
};

export const register = async (userData: UserRegistrationDTO): Promise<UserResponseDTO> => {
    const response = await axios.post(`${API_URL}/register`, userData);
    return response.data;
};

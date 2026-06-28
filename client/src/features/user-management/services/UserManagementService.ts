import type {UserResponseDTO} from "@/features/authentication/types/api.types.ts";
import api from "@/shared/services/api.ts";
import type {UserUpdateDTO, UserUpdatePasswordDTO} from "@/features/user-management/types/types.ts";

const ENDPOINTS = {
    profile: '/users/me',
    password: '/users/me/password',
} as const;

export const getCurrentUser = async (): Promise<UserResponseDTO> => {
    const {data} = await api.get<UserResponseDTO>(ENDPOINTS.profile);
    return data;
};

export const updatePartialProfile = async (updateData: UserUpdateDTO): Promise<UserResponseDTO> => {
    const {data} = await api.patch<UserResponseDTO>(ENDPOINTS.profile, updateData);
    return data;
};

export const updateFullProfile = async (updateData: Partial<UserUpdateDTO>): Promise<UserResponseDTO> => {
    const {data} = await api.put<UserResponseDTO>(ENDPOINTS.profile, updateData);
    return data;
};

export const changePassword = async (passwordData: UserUpdatePasswordDTO): Promise<void> => {
    await api.put(ENDPOINTS.password, passwordData);
};

export const deleteAccount = async (password: string): Promise<void> => {
    await api.delete(ENDPOINTS.profile, {data: {password}});
};

import type {UserResponseDTO} from "../../authentication/types/api.types.ts";
import api from "../../../shared/services/api.ts";
import type {UserUpdateDTO, UserUpdatePasswordDTO} from "../types/types.ts";
import type {User} from "../../../shared/types/types.ts";

const ENDPOINTS = {
    base: '/users',
    profile: '/users/me',
    changePassword: '/users/change-password',
    updateEmail: '/users/update-email',
    avatar: '/users/avatar',
    preferences: '/users/preferences',
    verifyEmail: '/users/verify-email',
    requestVerification: '/users/request-verification',
    checkEmail: '/users/check-email',
    exportData: '/users/export-data',
    activity: '/users/activity',
    twoFA: {
        enable: '/users/2fa/enable',
        disable: '/users/2fa/disable',
        verify: '/users/2fa/verify'
    }
} as const;

export const getCurrentUser = async (): Promise<UserResponseDTO> => {
    const {data} = await api.get<UserResponseDTO>(ENDPOINTS.profile);
    return data;
};

export const updatePartialProfile = async (updateData: UserUpdateDTO): Promise<string> => {
    const {data} = await api.patch<string>(ENDPOINTS.profile, updateData);
    return data;
};

export const updateFullProfile = async (updateData: Partial<UserUpdateDTO>): Promise<string> => {
    const {data} = await api.put<string>(ENDPOINTS.profile, updateData);
    return data;
};

export const changePassword = async (passwordData: UserUpdatePasswordDTO): Promise<{ message: string }> => {
    const {data} = await api.put<{ message: string }>(ENDPOINTS.changePassword, passwordData);
    return data;
};

export const deleteAccount = async (password: string): Promise<{ message: string }> => {
    const {data} = await api.delete<{ message: string }>(ENDPOINTS.profile, {
        data: {password}
    });
    return data;
};

export const getUserById = async (userId: string): Promise<UserResponseDTO> => {
    const {data} = await api.get<UserResponseDTO>(`${ENDPOINTS.base}/${userId}`);
    return data;
};

export const getUsers = async (): Promise<UserResponseDTO[]> => {
    const {data} = await api.get<UserResponseDTO[]>(ENDPOINTS.base);
    return data;
};

export const updateUserFully = async (userId: string, updateData: UserUpdateDTO): Promise<UserResponseDTO> => {
    const {data} = await api.put<UserResponseDTO>(`${ENDPOINTS.base}/${userId}`, updateData);
    return data;
};

export const updateUserPartially = async (userId: string, updateData: Partial<UserUpdateDTO>): Promise<UserResponseDTO> => {
    const {data} = await api.patch<UserResponseDTO>(`${ENDPOINTS.base}/${userId}`, updateData);
    return data;
}

export const deleteUser = async (userId: string): Promise<void> => {
    await api.delete<void>(`${ENDPOINTS.base}/${userId}`);
};

// export const toggleUserStatus = async (userId: string): Promise<User> => {
//     const {data} = await api.patch<User>(`${ENDPOINTS.base}/${userId}/toggle-status`);
//     return data;
// };

export const updateUserRole = async (userId: string, role: string): Promise<User> => {
    const {data} = await api.patch<User>(`${ENDPOINTS.base}/${userId}/role`, {role});
    return data;
};


export const verifyEmail = async (token: string): Promise<{ message: string; user: User }> => {
    const {data} = await api.post<{ message: string; user: User }>(ENDPOINTS.verifyEmail, {token});
    return data;
};

export const requestEmailVerification = async (): Promise<{ message: string }> => {
    const {data} = await api.post<{ message: string }>(ENDPOINTS.requestVerification);
    return data;
};


export const exportUserData = async (): Promise<Blob> => {
    const {data} = await api.get<Blob>(ENDPOINTS.exportData, {
        responseType: 'blob'
    });
    return data;
};

export const enable2FA = async (): Promise<{ qrCode: string; secret: string }> => {
    const {data} = await api.post<{ qrCode: string; secret: string }>(ENDPOINTS.twoFA.enable);
    return data;
};

export const disable2FA = async (password: string): Promise<{ message: string }> => {
    const {data} = await api.post<{ message: string }>(ENDPOINTS.twoFA.disable, {password});
    return data;
};

export const verify2FA = async (token: string): Promise<{ message: string; backupCodes?: string[] }> => {
    const {data} = await api.post<{ message: string; backupCodes?: string[] }>(
        ENDPOINTS.twoFA.verify,
        {token}
    );
    return data;
};

export const userProfile = {
    getCurrentUser,
    updateFullProfile,
    updatePartialProfile,
    changePassword,
    deleteAccount,
} as const;

export const userAdmin = {
    getUserById,
    getUsers,
    updateUserFully,
    updateUserPartially,
    deleteUser,
    updateUserRole,
} as const;

export const userSettings = {
    enable2FA,
    disable2FA,
    verify2FA,
} as const;

export const userValidation = {
    verifyEmail,
    requestEmailVerification,
} as const;

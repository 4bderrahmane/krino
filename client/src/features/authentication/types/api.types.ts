export interface User {
    email: string;
    password: string;
    name: string;
}

export interface UserLoginDTO {
    email: string;
    password: string;
}

export interface LoginResponse {
    user: UserResponseDTO;
    token: string;
    expiresIn: number;
}

export type Role = 'ADMIN' | 'CANDIDATE' | 'INTERVIEWER' | 'HR_MANAGER';

export interface UserRegistrationDTO
{
    email: string;
    password: string;
    name: string;
}

export interface UserResponseDTO {
    id: number;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    roles: Set<Role>;
}

export interface UserUpdateDTO {
    username?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phoneNumber?: string;
}

export interface UserUpdatePasswordDTO {
    currentPassword: string;
    newPassword: string;
    confirmNewPassword: string;
}

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    message?: string;
}

export interface ApiError {
    success: false;
    error: string;
    details?: string[];
    statusCode: number;
}
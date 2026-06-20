export interface User {
    email: string;
    password: string;
    name: string;
}

export interface UserLoginDTO {
    email: string;
    password: string;
}

export type Role = 'ADMIN' | 'CANDIDATE' | 'INTERVIEWER' | 'HR_MANAGER';

export interface UserRegistrationDTO {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
}

export interface UserResponseDTO {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    roles: Set<Role>;
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

export interface LoginComponentProps {
    onLoginSuccess: (credentials: UserLoginDTO) => void;
}

export interface RegistrationComponentProps {
    onRegistrationSuccess: (credentials: UserRegistrationDTO) => void;
}

export interface AuthResponse {
    user: UserResponseDTO;
    message?: string;
}

/** RFC 9457 problem detail (application/problem+json) with our extensions. */
export interface BackendErrorResponse {
    type?: string;
    title?: string;
    status: number;
    detail?: string;
    instance?: string;
    timestamp?: string;
    errorCode: string;
    details?: Record<string, unknown>;
}

export interface EnhancedError extends Error {
    errorCode?: AuthErrorCode;
    backendError?: BackendErrorResponse;
}

export type AuthErrorCode =
    | 'UNEXPECTED_ERROR'
    | 'INVALID_REQUEST_BODY'
    | 'RESOURCE_NOT_FOUND'
    | 'INVALID_TOKEN'
    | 'ACCESS_DENIED'
    | 'EMAIL_ALREADY_IN_USE'
    | 'EMAIL_ALREADY_EXISTS'
    | 'PASSWORD_TOO_WEAK'
    | 'INVALID_EMAIL_FORMAT'
    | 'INVALID_CREDENTIALS'
    | 'ACCOUNT_DISABLED'
    | 'ACCOUNT_LOCKED'
    | 'ACCOUNT_NOT_VERIFIED'
    | 'PASSWORD_CHANGE_FAILED'
    | 'PASSWORD_RESET_TOKEN_EXPIRED'
    | 'INVALID_PASSWORD_RESET_TOKEN'
    | 'NEW_PASSWORD_SAME_AS_OLD'
    | 'AUTHENTICATION_REQUIRED'
    | 'AUTHENTICATION_FAILED'
    | 'USER_NOT_FOUND'
    | 'USER_ALREADY_EXISTS'
    | 'VALIDATION_FAILED'
    | 'INTERNAL_SERVER_ERROR';

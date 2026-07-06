import type {ServerErrorCode} from '@/shared/services/errors';

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
    // Public UUID (the API exposes publicId as `id`), not a numeric id.
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    // The API serialises roles as a JSON array, not a Set.
    roles: Role[];
    // Base-CV metadata; null when the user has no CV on file (e.g. admin-created staff).
    resumeFilename?: string | null;
    resumeUploadedAt?: string | null;
    // True while the user is still on the admin-generated initial password
    // (staff accounts). Drives the "change your password" reminder.
    mustChangePassword?: boolean;
    // Whether the account is approved (enabled). Drives the admin approval toggle.
    approved?: boolean;
    // Whether the user has proven ownership of their email address. Login is
    // blocked server-side until this is true.
    emailVerified?: boolean;
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

export interface ForgotPasswordDTO {
    email: string;
}

export interface ResetPasswordDTO {
    token: string;
    newPassword: string;
    confirmNewPassword: string;
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
    errorCode?: ServerErrorCode;
    backendError?: BackendErrorResponse;
}

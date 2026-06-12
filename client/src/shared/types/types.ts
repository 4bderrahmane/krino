import type {UserResponseDTO} from "../../features/authentication/types/api.types.ts";

export type Language = {
    code: string;
    name: string;
    flag: string;
};

export interface User {
    email: string;
    firstName: string;
    lastName: string;
    phoneNumber: number;
    roles: Set<string>;
}

export interface SuccessToastProps {
    message: string;
    isVisible: boolean;
    onClose: () => void;
    duration?: number;
}

export interface ToastState {
    key: number;
    message: string;
    duration: number;
}
export interface AuthContextType {
    user: UserResponseDTO | null;
    login: (user: UserResponseDTO) => void;
    logout: () => void;
    isLoading: boolean;
    justLoggedIn: boolean;
    clearJustLoggedIn: () => void;
}

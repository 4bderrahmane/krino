import type {UserResponseDTO} from "../../features/authentication/types/api.types.ts";

export type Language = {
    code: string;
    name: string;
    flag: string;
};

export interface NavbarProps {
    username?: string;
    onLogout: () => void;
}

export interface DashboardProps {
    currentUser: Partial<User>;
}

export interface User {
    username: string;
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
    isAuthenticated: boolean;
    isLoading: boolean; // Add loading state for initial auth check
    justLoggedIn: boolean; // Add flag to track fresh logins
    clearJustLoggedIn: () => void; // Function to clear the flag
}

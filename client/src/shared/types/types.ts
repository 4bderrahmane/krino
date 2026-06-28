import type {UserResponseDTO} from "@/features/authentication/types/api.types.ts";

export type Language = {
    code: string;
    name: string;
    flag: string;
};

export interface AuthContextType {
    user: UserResponseDTO | null;
    login: (user: UserResponseDTO) => void;
    logout: () => void;
    isLoading: boolean;
    justLoggedIn: boolean;
    clearJustLoggedIn: () => void;
    // Re-fetches the current user from the server (e.g. after a password change,
    // so flags like mustChangePassword reflect the new state).
    refreshUser: () => Promise<void>;
}

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

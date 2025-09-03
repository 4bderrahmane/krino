import React, {useState, useEffect} from 'react';
import type {UserResponseDTO} from '../../features/authentication/types/api.types';
import type {AuthContextType} from "../types/types.ts";
import {checkAuthStatus} from '../../features/authentication/services/AuthenticationService.ts';
import { AuthContext } from './authContext';


interface AuthProviderProps {
    children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children}) => {
    const [user, setUser] = useState<UserResponseDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [justLoggedIn, setJustLoggedIn] = useState(false);

    useEffect(() => {
        const checkAuth = async () => {
            try {
                const userData = await checkAuthStatus();
                if (userData) {
                    setUser(userData);
                    // Don't set justLoggedIn to true for existing sessions
                }
            } catch (error) {
                console.error('Failed to check auth status:', error);
                // This is expected since /auth/me doesn't exist - just continue
            } finally {
                setIsLoading(false);
            }
        };

        checkAuth();
    }, []);

    const login = (userData: UserResponseDTO) => {
        console.log('AuthContext login called with:', userData);
        setUser(userData);
        setJustLoggedIn(true); // Set flag when user logs in
        console.log('AuthContext - justLoggedIn set to true');
    };

    const logout = () => {
        setUser(null);
        setJustLoggedIn(false);
    };

    const clearJustLoggedIn = () => {
        console.log('Clearing justLoggedIn flag');
        setJustLoggedIn(false);
    };

    const value: AuthContextType = {
        user,
        login,
        logout,
        isAuthenticated: !!user,
        isLoading,
        justLoggedIn,
        clearJustLoggedIn,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

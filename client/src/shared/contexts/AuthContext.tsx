import React, {useState, useEffect} from 'react';
import type {UserResponseDTO} from '@/features/authentication/types/api.types';
import type {AuthContextType} from '@/shared/types/types.ts';
import {checkAuthStatus} from '@/features/authentication/services/AuthenticationService.ts';
import {setupTokenRefresh, clearTokenRefresh} from '@/shared/services/api.ts';
import {AuthContext} from './authContext.ts';

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
                    // Resume proactive refresh for the restored session so it
                    // stays alive across reloads (not only right after login).
                    setupTokenRefresh();
                    // Don't set justLoggedIn to true for existing sessions
                }
            } catch (error) {
                console.error('Failed to check auth status:', error);
            } finally {
                setIsLoading(false);
            }
        };

        checkAuth();
    }, []);

    const login = (userData: UserResponseDTO) => {
        setUser(userData);
        setJustLoggedIn(true); // Set flag when user logs in
    };

    const logout = () => {
        clearTokenRefresh();
        setUser(null);
        setJustLoggedIn(false);
    };

    const clearJustLoggedIn = () => {
        setJustLoggedIn(false);
    };

    // Pull the latest user from /users/me. Used after mutations that change user
    // state server-side (e.g. a password change clearing mustChangePassword).
    const refreshUser = async () => {
        const userData = await checkAuthStatus();
        if (userData) {
            setUser(userData);
        }
    };

    const value: AuthContextType = {
        user,
        login,
        logout,
        isLoading,
        justLoggedIn,
        clearJustLoggedIn,
        refreshUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

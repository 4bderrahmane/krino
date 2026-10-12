import React, {useCallback, useRef, useState, useEffect} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import type {UserResponseDTO} from '@/features/authentication/types/api.types';
import type {AuthContextType} from '@/shared/types/types.ts';
import {checkAuthStatus} from '@/features/authentication/services/AuthenticationService.ts';
import {setupTokenRefresh, clearTokenRefresh, SESSION_ENDED_EVENT} from '@/shared/services/api.ts';
import {AuthContext} from './authContext.ts';

interface AuthProviderProps {
    children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children}) => {
    const [user, setUser] = useState<UserResponseDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [justLoggedIn, setJustLoggedIn] = useState(false);
    const queryClient = useQueryClient();
    const sessionScope = useRef<string | null>(null);
    const authRevision = useRef(0);

    const setSessionUser = useCallback((nextUser: UserResponseDTO | null, newLogin = false) => {
        const nextScope = nextUser
            ? `${nextUser.id}:${[...nextUser.roles].sort().join(',')}:${nextUser.approved}:${nextUser.emailVerified}`
            : null;
        if (sessionScope.current !== nextScope || newLogin) {
            sessionScope.current = nextScope;
            // cancelQueries aborts signal-aware requests and prevents late results
            // from restoring private data after the cache is cleared.
            void queryClient.cancelQueries();
            queryClient.clear();
        }
        setUser(nextUser);
    }, [queryClient]);

    useEffect(() => {
        let active = true;
        const revision = authRevision.current;
        const checkAuth = async () => {
            try {
                const userData = await checkAuthStatus();
                if (active && authRevision.current === revision && userData) {
                    setSessionUser(userData);
                    // Resume proactive refresh for a restored session.
                    setupTokenRefresh();
                }
            } catch (error) {
                console.error('Failed to check auth status:', error);
            } finally {
                if (active) setIsLoading(false);
            }
        };

        void checkAuth();
        return () => { active = false; };
    }, [setSessionUser]);

    const login = (userData: UserResponseDTO) => {
        authRevision.current += 1;
        setSessionUser(userData, true);
        setIsLoading(false);
        setJustLoggedIn(true);
    };

    const logout = useCallback(() => {
        authRevision.current += 1;
        clearTokenRefresh();
        setSessionUser(null);
        setIsLoading(false);
        setJustLoggedIn(false);
    }, [setSessionUser]);

    useEffect(() => {
        window.addEventListener(SESSION_ENDED_EVENT, logout);
        return () => window.removeEventListener(SESSION_ENDED_EVENT, logout);
    }, [logout]);

    const clearJustLoggedIn = () => {
        setJustLoggedIn(false);
    };

    // Pull the latest user from /users/me. Used after mutations that change user
    // state server-side (e.g. a password change clearing mustChangePassword).
    const refreshUser = async () => {
        const revision = authRevision.current;
        const userData = await checkAuthStatus();
        if (userData && authRevision.current === revision) {
            setSessionUser(userData);
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

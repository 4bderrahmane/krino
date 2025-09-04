import React, { useState, useEffect } from 'react';
import type { UserResponseDTO } from '../../features/authentication/types/api.types';
import type { AuthContextType } from '../types/types.ts';
import { checkAuthStatus } from '../../features/authentication/services/AuthenticationService.ts';
import { AuthContext } from './authContext.ts';

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
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
  };

  const logout = () => {
    setUser(null);
    setJustLoggedIn(false);
  };

  const clearJustLoggedIn = () => {
    setJustLoggedIn(false);
  };

  const value: AuthContextType = {
    user,
    login,
    logout,
    isLoading,
    justLoggedIn,
    clearJustLoggedIn,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

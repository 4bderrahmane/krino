import { createContext } from 'react';
import type { AuthContextType } from '../types/types';

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

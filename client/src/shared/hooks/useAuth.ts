import {useContext} from 'react';
import {AuthContext} from '@/shared/contexts/authContext.ts';
import type {AuthContextType} from '@/shared/types/types';

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
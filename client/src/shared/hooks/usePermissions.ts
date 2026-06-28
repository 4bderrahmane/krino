import {useMemo} from 'react';
import type {Role} from '@/features/authentication/types/api.types';
import {useAuth} from '@/shared/hooks/useAuth';

// Roles considered "staff" for list/scope decisions (see backend authorities).
const STAFF_ROLES: Role[] = ['ADMIN', 'HR_MANAGER'];

export interface Permissions {
    roles: Role[];
    hasRole: (role: Role) => boolean;
    hasAnyRole: (...roles: Role[]) => boolean;
    isAdmin: boolean;
    isStaff: boolean;
    isCandidate: boolean;
    isInterviewer: boolean;
}

/**
 * Single source of truth for role checks. Replaces the ad-hoc
 * `Array.from(user.roles as Iterable<string>).includes(...)` scattered across components.
 */
export const usePermissions = (): Permissions => {
    const {user} = useAuth();

    return useMemo(() => {
        const roles = user?.roles ?? [];
        const hasRole = (role: Role) => roles.includes(role);
        return {
            roles,
            hasRole,
            hasAnyRole: (...candidates: Role[]) => candidates.some(hasRole),
            isAdmin: hasRole('ADMIN'),
            isStaff: roles.some((role) => STAFF_ROLES.includes(role)),
            isCandidate: hasRole('CANDIDATE'),
            isInterviewer: hasRole('INTERVIEWER'),
        };
    }, [user]);
};

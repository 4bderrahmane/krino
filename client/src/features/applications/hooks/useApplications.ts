import {useQuery} from '@tanstack/react-query';
import {useAuth} from '@/shared/hooks/useAuth';
import {
    getApplicationById,
    getApplications,
    getMyApplications,
} from '@/features/applications/services/ApplicationService.ts';

// 'all'  -> every application (staff view, ADMIN / HR_MANAGER)
// 'mine' -> only the current user's applications (candidate view)
export type ApplicationScope = 'all' | 'mine';

export const useApplications = (scope: ApplicationScope, page = 0, size = 20) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['applications', scope, user?.id, page, size],
        queryFn: ({signal}) => scope === 'mine'
            ? getMyApplications(page, size, signal)
            : getApplications(page, size, signal),
        enabled: !!user,
    });
};

export const useApplication = (id: string | undefined) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['application', id, user?.id],
        queryFn: ({signal}) => getApplicationById(id as string, signal),
        enabled: !!id && !!user,
    });
};

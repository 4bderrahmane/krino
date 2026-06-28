import {useQuery} from '@tanstack/react-query';
import {
    getApplicationById,
    getApplications,
    getMyApplications,
} from '@/features/applications/services/ApplicationService.ts';

// 'all'  -> every application (staff view, ADMIN / HR_MANAGER)
// 'mine' -> only the current user's applications (candidate view)
export type ApplicationScope = 'all' | 'mine';

export const useApplications = (scope: ApplicationScope, page = 0, size = 20) =>
    useQuery({
        queryKey: ['applications', scope, page, size],
        queryFn: () => (scope === 'mine' ? getMyApplications(page, size) : getApplications(page, size)),
    });

export const useApplication = (id: string | undefined) =>
    useQuery({
        queryKey: ['application', id],
        queryFn: () => getApplicationById(id as string),
        enabled: !!id,
    });

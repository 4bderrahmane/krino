import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
    createDepartment,
    getManagedDepartments,
    getPublicDepartments,
} from '@/features/departments/services/DepartmentService.ts';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useAuth} from '@/shared/hooks/useAuth';

/**
 * The directory, from whichever endpoint the caller is entitled to.
 *
 * Staff and interviewers read the whole thing; everyone else, signed in or not, reads
 * the departments that actually have an opening. The audience is part of the query key
 * so a visitor's cached list is never handed to a staff member who signs in on the same
 * tab: the two lists differ, and the shorter one would silently hide departments from
 * the offer form's picker.
 *
 * Unpaged, so the directory page and that picker share one cache entry and the picker
 * costs no extra request.
 */
export const useDepartments = () => {
    const {isStaff, isInterviewer} = usePermissions();
    const {user} = useAuth();
    const seesWholeDirectory = isStaff || isInterviewer;

    return useQuery({
        queryKey: ['departments', seesWholeDirectory ? 'managed' : 'public', seesWholeDirectory ? user?.id : null],
        queryFn: ({signal}) => seesWholeDirectory
            ? getManagedDepartments(signal)
            : getPublicDepartments(signal),
    });
};

// Creates a department, then invalidates every departments query so both audiences
// reflect the new entry.
export const useCreateDepartment = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createDepartment,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['departments']});
        },
    });
};

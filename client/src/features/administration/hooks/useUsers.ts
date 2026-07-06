import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {getAllUsers, setUserApproval} from '@/features/administration/services/AdminService.ts';

// The full user directory (staff only). Used by the approval-management table.
export const useUsers = () =>
    useQuery({
        queryKey: ['users', 'all'],
        queryFn: getAllUsers,
    });

// Approves or revokes a user, then refreshes every users query (directory + the
// interviewer picker) so the new state shows everywhere.
export const useSetUserApproval = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({id, approved}: {id: string; approved: boolean}) => setUserApproval(id, approved),
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['users']}),
    });
};

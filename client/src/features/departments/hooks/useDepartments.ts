import {keepPreviousData, useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
    createDepartment,
    getAllDepartments,
    getDepartments,
} from '@/features/departments/services/DepartmentService.ts';

export const useDepartments = (page: number) =>
    useQuery({
        queryKey: ['departments', page],
        queryFn: () => getDepartments(page),
        // Keep the current page visible while the next one loads, avoiding a
        // full-page spinner flash on every pagination click.
        placeholderData: keepPreviousData,
    });

// The complete, unpaginated list — for pickers (e.g. the create-offer form).
export const useAllDepartments = () =>
    useQuery({
        queryKey: ['departments', 'all'],
        queryFn: getAllDepartments,
    });

// Creates a department, then invalidates every departments query (the paginated
// list and the "all" picker) so both reflect the new entry.
export const useCreateDepartment = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createDepartment,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['departments']});
        },
    });
};

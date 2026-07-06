import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
    createInterview,
    deleteInterview,
    getInterview,
    getInterviews,
    getMyInterviews,
    updateInterview,
} from '@/features/interviews/services/InterviewService.ts';
import type {InterviewFormValues} from '@/features/interviews/types/interview.types.ts';

// 'all'  -> every interview (staff view, ADMIN / HR_MANAGER)
// 'mine' -> interviews the current user takes part in (candidate or interviewer)
export type InterviewScope = 'all' | 'mine';

export const useInterviews = (scope: InterviewScope, page = 0, size = 20) =>
    useQuery({
        queryKey: ['interviews', scope, page, size],
        queryFn: () => (scope === 'mine' ? getMyInterviews(page, size) : getInterviews(page, size)),
    });

export const useInterview = (id: string | undefined) =>
    useQuery({
        queryKey: ['interview', id],
        queryFn: () => getInterview(id as string),
        enabled: !!id,
    });

// Any interview write touches the slot availability (booking/freeing a slot) and
// the lists, so refresh both families.
const useInvalidateInterviews = () => {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({queryKey: ['interviews']});
        queryClient.invalidateQueries({queryKey: ['slots']});
    };
};

export const useCreateInterview = () => {
    const invalidate = useInvalidateInterviews();
    return useMutation({
        mutationFn: createInterview,
        onSuccess: invalidate,
    });
};

export const useUpdateInterview = () => {
    const invalidate = useInvalidateInterviews();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({id, values}: {id: string; values: InterviewFormValues}) => updateInterview(id, values),
        onSuccess: (interview) => {
            invalidate();
            queryClient.invalidateQueries({queryKey: ['interview', interview.id]});
        },
    });
};

export const useDeleteInterview = () => {
    const invalidate = useInvalidateInterviews();
    return useMutation({
        mutationFn: deleteInterview,
        onSuccess: invalidate,
    });
};

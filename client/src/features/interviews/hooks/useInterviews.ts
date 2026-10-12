import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {useAuth} from '@/shared/hooks/useAuth';
import {
    createInterview,
    deleteInterview,
    getInterview,
    getInterviews,
    getMyInterviews,
    recordInterviewOutcome,
    updateInterview,
} from '@/features/interviews/services/InterviewService.ts';
import type {InterviewFormValues, InterviewOutcomeValues} from '@/features/interviews/types/interview.types.ts';

// 'all'  -> every interview (staff view, ADMIN / HR_MANAGER)
// 'mine' -> interviews the current user takes part in (candidate or interviewer)
export type InterviewScope = 'all' | 'mine';

export const useInterviews = (scope: InterviewScope, page = 0, size = 20) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['interviews', scope, user?.id, page, size],
        queryFn: ({signal}) => scope === 'mine'
            ? getMyInterviews(page, size, signal)
            : getInterviews(page, size, signal),
        enabled: !!user,
    });
};

export const useInterview = (id: string | undefined) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['interview', id, user?.id],
        queryFn: ({signal}) => getInterview(id as string, signal),
        enabled: !!id && !!user,
    });
};

// Any interview write touches the slot availability (booking/freeing a slot) and
// the lists, so refresh both families.
const useInvalidateInterviews = () => {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({queryKey: ['interviews']});
        queryClient.invalidateQueries({queryKey: ['slots']});
        queryClient.invalidateQueries({queryKey: ['applications']});
        queryClient.invalidateQueries({queryKey: ['application']});
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

export const useRecordInterviewOutcome = () => {
    const invalidate = useInvalidateInterviews();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({id, values}: {id: string; values: InterviewOutcomeValues}) =>
            recordInterviewOutcome(id, values),
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

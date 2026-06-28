import {useQuery} from '@tanstack/react-query';
import {getInterviews, getMyInterviews} from '@/features/interviews/services/InterviewService.ts';

// 'all'  -> every interview (staff view, ADMIN / HR_MANAGER)
// 'mine' -> interviews the current user takes part in (candidate or interviewer)
export type InterviewScope = 'all' | 'mine';

export const useInterviews = (scope: InterviewScope, page = 0, size = 20) =>
    useQuery({
        queryKey: ['interviews', scope, page, size],
        queryFn: () => (scope === 'mine' ? getMyInterviews(page, size) : getInterviews(page, size)),
    });

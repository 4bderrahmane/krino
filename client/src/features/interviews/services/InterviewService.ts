import api from '@/shared/services/api.ts';
import type {Interview, InterviewPage, InterviewParticipant, InterviewStatus} from '@/features/interviews/types/interview.types.ts';

// ---------------------------------------------------------------------------
// Raw backend shapes, kept private to this module. (The backend still calls the
// offer a "job"; we normalise it to `offer` here — the single translation point.)
// ---------------------------------------------------------------------------
interface UserDTO {
    id: string;
    firstName?: string | null;
    lastName?: string | null;
    email?: string | null;
}

interface DepartmentDTO {
    name: string;
}

interface JobDTO {
    id: string;
    title: string;
    department?: DepartmentDTO | null;
}

interface SlotDTO {
    interviewDate?: string | null;
    startTime?: string | null;
    endTime?: string | null;
}

interface InterviewResponseDTO {
    id: string;
    interviewer?: UserDTO | null;
    candidate?: UserDTO | null;
    job?: JobDTO | null;
    slot?: SlotDTO | null;
    status?: InterviewStatus | null;
    notes?: string | null;
    isOnline?: boolean | null;
    meetingUrl?: string | null;
}

interface PageResponse<T> {
    content: T[];
    page: {
        number: number;
        size: number;
        totalElements: number;
        totalPages: number;
    };
}

const toParticipant = (user: UserDTO | null | undefined): InterviewParticipant => ({
    id: user?.id ?? '',
    fullName: [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim() || '—',
    email: user?.email ?? '—',
});

const toInterview = (dto: InterviewResponseDTO): Interview => ({
    id: dto.id,
    offer: dto.job
        ? {id: dto.job.id, title: dto.job.title, department: dto.job.department?.name ?? null}
        : null,
    candidate: toParticipant(dto.candidate),
    interviewer: toParticipant(dto.interviewer),
    schedule: dto.slot
        ? {
              date: dto.slot.interviewDate ?? null,
              startTime: dto.slot.startTime ?? null,
              endTime: dto.slot.endTime ?? null,
          }
        : null,
    status: dto.status ?? 'SCHEDULED',
    isOnline: dto.isOnline ?? false,
    meetingUrl: dto.meetingUrl ?? null,
    notes: dto.notes ?? null,
});

const toInterviewPage = (data: PageResponse<InterviewResponseDTO>): InterviewPage => ({
    interviews: data.content.map(toInterview),
    page: data.page,
});

const INTERVIEWS_ENDPOINT = '/interviews';

/** All interviews — staff only (ADMIN / HR_MANAGER). */
export const getInterviews = async (page = 0, size = 20): Promise<InterviewPage> => {
    const {data} = await api.get<PageResponse<InterviewResponseDTO>>(INTERVIEWS_ENDPOINT, {
        params: {page, size},
    });
    return toInterviewPage(data);
};

/** Interviews the current user takes part in (as candidate or interviewer). */
export const getMyInterviews = async (page = 0, size = 20): Promise<InterviewPage> => {
    const {data} = await api.get<PageResponse<InterviewResponseDTO>>(`${INTERVIEWS_ENDPOINT}/me`, {
        params: {page, size},
    });
    return toInterviewPage(data);
};

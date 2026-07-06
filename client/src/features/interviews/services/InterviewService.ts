import api from '@/shared/services/api.ts';
import type {
    Interview,
    InterviewFormValues,
    InterviewPage,
    InterviewParticipant,
    InterviewRecommendation,
    InterviewStatus,
} from '@/features/interviews/types/interview.types.ts';

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
    id?: string | null;
    interviewDate?: string | null;
    startTime?: string | null;
    endTime?: string | null;
}

interface InterviewResponseDTO {
    id: string;
    applicationId?: string | null;
    interviewer?: UserDTO | null;
    candidate?: UserDTO | null;
    job?: JobDTO | null;
    slot?: SlotDTO | null;
    status?: InterviewStatus | null;
    recommendation?: InterviewRecommendation | null;
    notes?: string | null;
    isOnline?: boolean | null;
    meetingUrl?: string | null;
}

// Mirrors InterviewRequestDTO (POST / PUT / PATCH).
interface InterviewRequestDTO {
    applicationId: string;
    slotId: string;
    status: InterviewStatus;
    recommendation: InterviewRecommendation | null;
    notes: string | null;
    isOnline: boolean;
    meetingUrl: string | null;
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
    applicationId: dto.applicationId ?? '',
    slotId: dto.slot?.id ?? null,
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
    recommendation: dto.recommendation ?? null,
    isOnline: dto.isOnline ?? false,
    meetingUrl: dto.meetingUrl ?? null,
    notes: dto.notes ?? null,
});

// The recommendation is only valid on a COMPLETED interview; the backend rejects
// it for any other status, so we drop it unless the interview is being completed.
const toRequestDTO = (values: InterviewFormValues): InterviewRequestDTO => ({
    applicationId: values.applicationId,
    slotId: values.slotId,
    status: values.status,
    recommendation: values.status === 'COMPLETED' ? values.recommendation : null,
    notes: values.notes?.trim() || null,
    isOnline: values.isOnline,
    meetingUrl: values.meetingUrl?.trim() || null,
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

/** A single interview by id. Requires CAN_READ_INTERVIEW. */
export const getInterview = async (id: string): Promise<Interview> => {
    const {data} = await api.get<InterviewResponseDTO>(`${INTERVIEWS_ENDPOINT}/${id}`);
    return toInterview(data);
};

/** Schedule an interview (books a slot for an application). Requires CAN_CREATE_INTERVIEW. */
export const createInterview = async (values: InterviewFormValues): Promise<Interview> => {
    const {data} = await api.post<InterviewResponseDTO>(INTERVIEWS_ENDPOINT, toRequestDTO(values));
    return toInterview(data);
};

/** Full update (PUT) — reschedule / change status, outcome, mode. Requires CAN_UPDATE_INTERVIEW. */
export const updateInterview = async (id: string, values: InterviewFormValues): Promise<Interview> => {
    const {data} = await api.put<InterviewResponseDTO>(`${INTERVIEWS_ENDPOINT}/${id}`, toRequestDTO(values));
    return toInterview(data);
};

/** Cancel/remove an interview. Requires CAN_DELETE_INTERVIEW. */
export const deleteInterview = async (id: string): Promise<void> => {
    await api.delete(`${INTERVIEWS_ENDPOINT}/${id}`);
};

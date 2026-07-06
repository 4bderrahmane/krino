// Domain types for the Interviews feature.
//
// Raw backend DTOs stay private to the service layer (InterviewService); the rest
// of the client consumes only these clean domain types.

export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

// The interviewer's hiring signal, recorded only on a COMPLETED interview.
export type InterviewRecommendation = 'STRONG_YES' | 'YES' | 'NO' | 'STRONG_NO';

export interface InterviewParticipant {
    id: string;
    fullName: string;
    email: string;
}

export interface InterviewOffer {
    id: string;
    title: string;
    department: string | null;
}

export interface InterviewSchedule {
    date: string | null;       // ISO date, e.g. 2026-06-25
    startTime: string | null;  // HH:mm:ss
    endTime: string | null;    // HH:mm:ss
}

export interface Interview {
    id: string;
    // The application being interviewed (immutable once set; determines candidate + offer).
    applicationId: string;
    // The booked slot (drives interviewer + schedule); null only if data is incomplete.
    slotId: string | null;
    offer: InterviewOffer | null;
    candidate: InterviewParticipant;
    interviewer: InterviewParticipant;
    schedule: InterviewSchedule | null;
    status: InterviewStatus;
    recommendation: InterviewRecommendation | null;
    isOnline: boolean;
    meetingUrl: string | null;
    notes: string | null;
}

// What the schedule/edit form hands back to the service.
export interface InterviewFormValues {
    applicationId: string;
    slotId: string;
    status: InterviewStatus;
    recommendation: InterviewRecommendation | null;
    notes: string | null;
    isOnline: boolean;
    meetingUrl: string | null;
}

export interface InterviewPageMeta {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface InterviewPage {
    interviews: Interview[];
    page: InterviewPageMeta;
}

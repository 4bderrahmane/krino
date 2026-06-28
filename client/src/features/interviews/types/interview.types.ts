// Domain types for the Interviews feature.
//
// Raw backend DTOs stay private to the service layer (InterviewService); the rest
// of the client consumes only these clean domain types.

export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

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
    offer: InterviewOffer | null;
    candidate: InterviewParticipant;
    interviewer: InterviewParticipant;
    schedule: InterviewSchedule | null;
    status: InterviewStatus;
    isOnline: boolean;
    meetingUrl: string | null;
    notes: string | null;
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

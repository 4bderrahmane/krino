// Domain types for the Slots (interviewer availability) feature.
//
// Raw backend DTOs stay private to the service layer (SlotService); the rest of
// the client consumes only these clean domain types.

export interface SlotInterviewer {
    id: string;
    fullName: string;
    email: string;
}

export interface Slot {
    id: string;
    interviewer: SlotInterviewer;
    date: string | null;       // ISO date, e.g. 2026-06-25
    startTime: string | null;  // HH:mm:ss
    endTime: string | null;    // HH:mm:ss
    durationInMinutes: number | null;
    // False once an interview is booked into the slot (backend-derived, read-only).
    available: boolean;
}

export interface SlotPageMeta {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface SlotPage {
    slots: Slot[];
    page: SlotPageMeta;
}

// What the create/edit forms hand back to the service. interviewerId is only set
// on create (the backend SlotUpdateDTO has no interviewer field, so an existing
// slot's owner can't be reassigned — only its window is editable).
export interface SlotFormValues {
    interviewerId?: string;
    date: string;       // YYYY-MM-DD
    startTime: string;  // HH:mm
    endTime: string;    // HH:mm
}

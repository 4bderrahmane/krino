// Domain types for the Applications feature.
//
// As with every feature, the raw backend DTOs stay private to the service layer
// (ApplicationService). Components and hooks only ever consume these clean
// domain types.

export type ApplicationStatus =
    | 'PENDING'
    | 'UNDER_REVIEW'
    | 'ACCEPTED'
    | 'REJECTED'
    | 'INTERVIEW_SCHEDULED';

export interface ApplicationCandidate {
    id: string;
    firstName: string;
    lastName: string;
    fullName: string;
    email: string;
}

// Resume metadata. There is no public URL — the PDF is streamed by the
// authenticated endpoint GET /applications/{id}/resume (see ApplicationService).
export interface ApplicationResume {
    filename: string | null;
    contentType: string | null;
    sizeBytes: number | null;
    uploadedAt: string | null;
}

export interface Application {
    id: string;
    offerId: string;
    offerTitle: string;
    offerDepartment: string | null;
    candidate: ApplicationCandidate;
    status: ApplicationStatus;
    resume: ApplicationResume | null;
    appliedAt: string | null;
}

export interface ApplicationPageMeta {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface ApplicationPage {
    applications: Application[];
    page: ApplicationPageMeta;
}

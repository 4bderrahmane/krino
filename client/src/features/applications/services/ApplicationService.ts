import api from '@/shared/services/api.ts';
import type {
    Application,
    ApplicationPage,
    ApplicationStatus,
} from '@/features/applications/types/application.types.ts';

// ---------------------------------------------------------------------------
// Raw backend shapes, kept private to this module. (The backend exposes the
// offer reference as `jobId`; we normalise it to `offerId` to match the client
// vocabulary — the single translation boundary lives here.)
// ---------------------------------------------------------------------------
interface CandidateDTO {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
}

// The backend exposes the resume as metadata (no URL); the file itself is
// downloaded from GET /applications/{id}/resume (see getApplicationResumeBlob).
interface ResumeDTO {
    originalFilename?: string | null;
    contentType?: string | null;
    sizeBytes?: number | null;
    uploadedAt?: string | null;
}

interface ApplicationResponseDTO {
    id: string;
    jobId: string;
    jobTitle?: string | null;
    jobDepartment?: string | null;
    candidate: CandidateDTO;
    status: ApplicationStatus;
    resume?: ResumeDTO | null;
    appliedAt?: string | null;
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

const toApplication = (dto: ApplicationResponseDTO): Application => ({
    id: dto.id,
    offerId: dto.jobId,
    offerTitle: dto.jobTitle ?? '',
    offerDepartment: dto.jobDepartment ?? null,
    candidate: {
        id: dto.candidate.id,
        firstName: dto.candidate.firstName,
        lastName: dto.candidate.lastName,
        fullName: `${dto.candidate.firstName} ${dto.candidate.lastName}`.trim(),
        email: dto.candidate.email,
    },
    status: dto.status,
    resume: dto.resume
        ? {
            filename: dto.resume.originalFilename ?? null,
            contentType: dto.resume.contentType ?? null,
            sizeBytes: dto.resume.sizeBytes ?? null,
            uploadedAt: dto.resume.uploadedAt ?? null,
        }
        : null,
    appliedAt: dto.appliedAt ?? null,
});

const toApplicationPage = (data: PageResponse<ApplicationResponseDTO>): ApplicationPage => ({
    applications: data.content.map(toApplication),
    page: data.page,
});

const APPLICATIONS_ENDPOINT = '/applications';

/** All applications — staff only (ADMIN / HR_MANAGER). */
export const getApplications = async (page = 0, size = 20): Promise<ApplicationPage> => {
    const {data} = await api.get<PageResponse<ApplicationResponseDTO>>(APPLICATIONS_ENDPOINT, {
        params: {page, size},
    });
    return toApplicationPage(data);
};

/** The current user's own applications. */
export const getMyApplications = async (page = 0, size = 20): Promise<ApplicationPage> => {
    const {data} = await api.get<PageResponse<ApplicationResponseDTO>>(`${APPLICATIONS_ENDPOINT}/me`, {
        params: {page, size},
    });
    return toApplicationPage(data);
};

export const getApplicationById = async (id: string): Promise<Application> => {
    const {data} = await api.get<ApplicationResponseDTO>(`${APPLICATIONS_ENDPOINT}/${id}`);
    return toApplication(data);
};

/** Apply to an offer: creates the application (backend expects the offer id as `jobId`). */
export const createApplication = async (offerId: string): Promise<Application> => {
    const {data} = await api.post<ApplicationResponseDTO>(APPLICATIONS_ENDPOINT, {jobId: offerId});
    return toApplication(data);
};

/** Attach a freshly uploaded PDF to an application. */
export const uploadApplicationResume = async (applicationId: string, resume: File): Promise<Application> => {
    const formData = new FormData();
    formData.append('resume', resume);
    // Override the default JSON content-type so axios sets the multipart boundary.
    const {data} = await api.put<ApplicationResponseDTO>(
        `${APPLICATIONS_ENDPOINT}/${applicationId}/resume`,
        formData,
        {headers: {'Content-Type': undefined}},
    );
    return toApplication(data);
};

/** Attach the candidate's base CV (from registration) to an application. */
export const applyWithBaseCv = async (applicationId: string): Promise<Application> => {
    const {data} = await api.put<ApplicationResponseDTO>(
        `${APPLICATIONS_ENDPOINT}/${applicationId}/resume/from-base`,
    );
    return toApplication(data);
};

/**
 * Download an application's resume PDF. The endpoint is authenticated (it streams
 * the file, no public URL), so we fetch it through the api client — which carries
 * the session cookie and the token-refresh handling — as a blob the caller can
 * open or save.
 */
export const getApplicationResumeBlob = async (applicationId: string): Promise<Blob> => {
    const {data} = await api.get<Blob>(`${APPLICATIONS_ENDPOINT}/${applicationId}/resume`, {
        responseType: 'blob',
    });
    return data;
};

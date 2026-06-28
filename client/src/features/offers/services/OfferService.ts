import api from '@/shared/services/api.ts';
import type {
    ContractType,
    CreateOfferInput,
    EmploymentType,
    ExperienceLevel,
    MoroccanCity,
    Offer,
    OfferPage,
    OfferStatus,
    RemotePolicy,
    SalaryCurrency,
    SalaryPeriod,
    SkillImportance,
} from '@/features/offers/types/offer.types.ts';

// ---------------------------------------------------------------------------
// Raw backend shapes. The API still speaks "job"; these types are intentionally
// kept private to this module so the rest of the client only ever sees `Offer`.
// They mirror JobResponseDTO field-for-field — when the backend enriches that
// response, add the field here and in `toOffer` below; nothing else changes.
// ---------------------------------------------------------------------------
interface JobDepartmentDTO {
    id: string;
    name: string;
    description?: string | null;
}

interface JobSkillDTO {
    name: string;
    slug: string;
    importance: SkillImportance;
}

interface JobResponseDTO {
    id: string;
    title: string;
    description?: string | null;
    department: JobDepartmentDTO;
    applyingDeadline?: string | null;
    salaryMin?: number | null;
    salaryMax?: number | null;
    salaryCurrency?: SalaryCurrency | null;
    salaryPeriod?: SalaryPeriod | null;
    salaryVisible?: boolean | null;
    location?: MoroccanCity | null;
    remotePolicy: RemotePolicy;
    experienceLevel?: ExperienceLevel | null;
    openPositions?: number | null;
    employmentType: EmploymentType;
    contractType: ContractType;
    status: OfferStatus;
    skills?: JobSkillDTO[] | null;
}

// Mirrors JobCreateDTO field-for-field. Kept private: the rest of the client
// only constructs a `CreateOfferInput` and never sees these "job" names.
interface JobCreateDTO {
    departmentName: string;
    title: string;
    description?: string | null;
    applicationDeadline: string;
    plannedStartDate?: string | null;
    salaryMin?: number | null;
    salaryMax?: number | null;
    salaryCurrency?: SalaryCurrency | null;
    salaryPeriod?: SalaryPeriod | null;
    salaryVisible: boolean;
    salaryNegotiable: boolean;
    city?: MoroccanCity | null;
    remotePolicy: RemotePolicy;
    experienceLevel?: ExperienceLevel | null;
    minimumExperienceYears?: number | null;
    openPositions: number;
    employmentType: EmploymentType;
    contractType: ContractType;
    skills: {name: string; importance: SkillImportance}[];
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

// The single translation boundary: job -> offer.
const toOffer = (job: JobResponseDTO): Offer => ({
    id: job.id,
    title: job.title,
    description: job.description ?? null,
    department: {
        id: job.department.id,
        name: job.department.name,
        description: job.department.description ?? null,
    },
    employmentType: job.employmentType,
    contractType: job.contractType,
    experienceLevel: job.experienceLevel ?? null,
    openPositions: job.openPositions ?? 1,
    status: job.status,
    location: job.location ?? null,
    remotePolicy: job.remotePolicy,
    salaryMin: job.salaryMin ?? null,
    salaryMax: job.salaryMax ?? null,
    salaryCurrency: job.salaryCurrency ?? null,
    salaryPeriod: job.salaryPeriod ?? null,
    salaryVisible: job.salaryVisible ?? false,
    applyingDeadline: job.applyingDeadline ?? null,
    skills: job.skills ?? [],
});

const OFFERS_ENDPOINT = '/jobs';

// The backend returns the full offer list (it does not paginate offers — see
// JobService#getAllJobs), so we fetch it in one request with no page/size params.
export const getOffers = async (): Promise<OfferPage> => {
    const {data} = await api.get<PageResponse<JobResponseDTO>>(OFFERS_ENDPOINT);
    return {
        offers: data.content.map(toOffer),
        page: data.page,
    };
};

export const getOfferById = async (id: string): Promise<Offer> => {
    const {data} = await api.get<JobResponseDTO>(`${OFFERS_ENDPOINT}/${id}`);
    return toOffer(data);
};

// The other side of the translation boundary: offer -> job. Empty optionals are
// sent as null (the backend treats null as "absent"); required fields are always
// present because the form supplies sane defaults for them.
const toJobCreateDTO = (input: CreateOfferInput): JobCreateDTO => ({
    departmentName: input.department,
    title: input.title,
    description: input.description,
    applicationDeadline: input.applyingDeadline,
    plannedStartDate: input.plannedStartDate,
    salaryMin: input.salaryMin,
    salaryMax: input.salaryMax,
    salaryCurrency: input.salaryCurrency,
    salaryPeriod: input.salaryPeriod,
    salaryVisible: input.salaryVisible,
    salaryNegotiable: input.salaryNegotiable,
    city: input.location,
    remotePolicy: input.remotePolicy,
    experienceLevel: input.experienceLevel,
    minimumExperienceYears: input.minimumExperienceYears,
    openPositions: input.openPositions,
    employmentType: input.employmentType,
    contractType: input.contractType,
    skills: input.skills.map((s) => ({name: s.name, importance: s.importance})),
});

// POST /api/jobs — requires the CAN_CREATE_JOB authority (ADMIN, HR_MANAGER).
export const createOffer = async (input: CreateOfferInput): Promise<Offer> => {
    const {data} = await api.post<JobResponseDTO>(OFFERS_ENDPOINT, toJobCreateDTO(input));
    return toOffer(data);
};

// Domain types for the Offers feature.
//
// NOTE: the backend still calls these "jobs" (endpoint GET /api/jobs, JSON keys
// use `job`). The translation from that vocabulary to "offer" lives ONLY in this
// feature's service layer (OfferService). Everything downstream — hooks,
// components, UI copy — speaks "offer" and never sees "job". If the backend is
// ever renamed, only the service adapter changes.
//
// These unions mirror the backend enums exactly (same names), so each value maps
// straight onto an i18n key under `offers.*` (e.g. offers.status.OPEN). When the
// JobResponse is enriched with more fields, extend `Offer` here and map the new
// field in OfferService#toOffer — nothing else needs to change.

export type OfferStatus =
    | 'SCHEDULED'
    | 'PAUSED'
    | 'CANCELLED'
    | 'DRAFT'
    | 'OPEN'
    | 'CLOSED'
    | 'FILLED'
    | 'ARCHIVED';

export type EmploymentType = 'FULL_TIME' | 'PART_TIME';

export type ContractType = 'PERMANENT' | 'FIXED_TERM' | 'INTERNSHIP' | 'FREELANCE';

export type ExperienceLevel =
    | 'INTERN'
    | 'ENTRY_LEVEL'
    | 'JUNIOR'
    | 'MID_LEVEL'
    | 'SENIOR'
    | 'LEAD'
    | 'MANAGER';

export type RemotePolicy = 'ON_SITE' | 'HYBRID' | 'REMOTE';

export type SalaryCurrency = 'MAD' | 'EUR' | 'USD' | 'GBP';

export type SalaryPeriod = 'HOURLY' | 'DAILY' | 'MONTHLY' | 'YEARLY';

export type SkillImportance = 'REQUIRED' | 'PREFERRED';

export type MoroccanCity =
    | 'AGADIR'
    | 'AL_HOCEIMA'
    | 'BENI_MELLAL'
    | 'BERRECHID'
    | 'CASABLANCA'
    | 'CHEFCHAOUEN'
    | 'DAKHLA'
    | 'EL_JADIDA'
    | 'ERRACHIDIA'
    | 'ESSAOUIRA'
    | 'FES'
    | 'GUELMIM'
    | 'INEZGANE'
    | 'KENITRA'
    | 'KHOURIBGA'
    | 'LAAYOUNE'
    | 'LARACHE'
    | 'MARRAKECH'
    | 'MEKNES'
    | 'MOHAMMEDIA'
    | 'NADOR'
    | 'OUARZAZATE'
    | 'OUJDA'
    | 'RABAT'
    | 'SAFI'
    | 'SALE'
    | 'SETTAT'
    | 'TANGIER'
    | 'TAROUDANT'
    | 'TAZA'
    | 'TEMARA'
    | 'TETOUAN';

export interface OfferDepartment {
    id: string;
    name: string;
    description?: string | null;
}

export interface OfferSkill {
    name: string;
    slug: string;
    importance: SkillImportance;
}

export interface Offer {
    id: string;
    title: string;
    description?: string | null;
    department: OfferDepartment;

    // Classification
    employmentType: EmploymentType;
    contractType: ContractType;
    experienceLevel: ExperienceLevel | null;
    openPositions: number;
    status: OfferStatus;

    // Where the work happens (city is null for fully-remote roles)
    location: MoroccanCity | null;
    remotePolicy: RemotePolicy;

    // Compensation. Either bound may be null; the range is shown whenever a
    // currency and at least one bound are set.
    salaryMin: number | null;
    salaryMax: number | null;
    salaryCurrency: SalaryCurrency | null;
    salaryPeriod: SalaryPeriod | null;

    // Timeline (ISO date, e.g. "2026-06-30")
    applyingDeadline: string | null;

    skills: OfferSkill[];
}

// Input for creating a new offer. Stays in "offer" vocabulary; the service
// layer (OfferService#createOffer) is the single boundary that renames these
// onto the backend's JobCreateDTO ("job") shape. Covers every field the backend
// accepts on creation — including ones the read model (`Offer`) does not surface
// (plannedStartDate, salaryNegotiable, minimumExperienceYears).
export interface CreateOfferSkill {
    name: string;
    importance: SkillImportance;
}

export interface CreateOfferInput {
    department: string;              // existing department NAME (backend resolves it)
    title: string;
    description: string | null;

    employmentType: EmploymentType;
    contractType: ContractType;
    experienceLevel: ExperienceLevel | null;
    minimumExperienceYears: number | null;
    openPositions: number;

    location: MoroccanCity | null;   // maps to JobCreateDTO.city
    remotePolicy: RemotePolicy;

    salaryMin: number | null;
    salaryMax: number | null;
    salaryCurrency: SalaryCurrency | null;
    salaryPeriod: SalaryPeriod | null;
    salaryNegotiable: boolean;

    applyingDeadline: string;        // ISO instant; maps to JobCreateDTO.applicationDeadline
    plannedStartDate: string | null; // ISO date (YYYY-MM-DD)

    skills: CreateOfferSkill[];
}

// Input for editing an existing offer. The read model (`Offer`) does not surface
// plannedStartDate, minimumExperienceYears or salaryNegotiable, so the edit form
// can't show them — and a PATCH (which preserves omitted fields) is used so they
// are left untouched rather than wiped. Hence these three are absent here.
export type EditOfferInput = Omit<
    CreateOfferInput,
    'plannedStartDate' | 'minimumExperienceYears' | 'salaryNegotiable'
>;

export interface OfferPageMeta {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface OfferPage {
    offers: Offer[];
    page: OfferPageMeta;
}

// Client-side filter state for the offers list. Multi-value facets are arrays
// (OR within a facet, AND across facets); single-value facets use '' for "any".
export interface OfferFilters {
    search: string;
    department: string;              // department name; '' = all
    city: MoroccanCity | '';         // '' = all
    remotePolicies: RemotePolicy[];
    employmentTypes: EmploymentType[];
    contractTypes: ContractType[];
    experienceLevels: ExperienceLevel[];
    disclosedSalaryOnly: boolean;
    minSalary: number;               // 0 = no minimum
}

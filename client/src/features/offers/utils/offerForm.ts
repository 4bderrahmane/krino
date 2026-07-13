// Shared state, validation and mapping for the offer create/edit forms. Both
// CreateOfferPage and EditOfferPage drive the same <OfferFormFields> with this
// FormState; only their submit/persistence differs.
import type {TFunction} from 'i18next';
import type {
    ContractType,
    CreateOfferInput,
    EditOfferInput,
    EmploymentType,
    ExperienceLevel,
    MoroccanCity,
    Offer,
    RemotePolicy,
    SalaryCurrency,
    SalaryPeriod,
    SkillImportance,
} from '@/features/offers/types/offer.types.ts';

export interface SkillRow {
    name: string;
    importance: SkillImportance;
}

// The form keeps everything as strings (what the inputs emit) and converts to a
// typed input on submit. '' means "not set" for every optional field.
export interface FormState {
    department: string;
    title: string;
    description: string;
    employmentType: EmploymentType;
    contractType: ContractType;
    experienceLevel: ExperienceLevel | '';
    minimumExperienceYears: string;
    openPositions: string;
    remotePolicy: RemotePolicy;
    location: MoroccanCity | '';
    salaryMin: string;
    salaryMax: string;
    salaryCurrency: SalaryCurrency | '';
    salaryPeriod: SalaryPeriod | '';
    salaryNegotiable: boolean;
    applyingDeadline: string;
    plannedStartDate: string;
}

export const initialState: FormState = {
    department: '',
    title: '',
    description: '',
    employmentType: 'FULL_TIME',
    contractType: 'PERMANENT',
    experienceLevel: '',
    minimumExperienceYears: '',
    openPositions: '1',
    remotePolicy: 'ON_SITE',
    location: '',
    salaryMin: '',
    salaryMax: '',
    salaryCurrency: '',
    salaryPeriod: '',
    salaryNegotiable: false,
    applyingDeadline: '',
    plannedStartDate: '',
};

export const initialSkills: SkillRow[] = [{name: '', importance: 'REQUIRED'}];

// Empty string -> null; otherwise a non-negative integer (NaN -> null).
export const toIntOrNull = (value: string): number | null => {
    if (value.trim() === '') return null;
    const n = Number(value);
    return Number.isFinite(n) ? Math.trunc(n) : null;
};

// Renders an ISO instant into the "YYYY-MM-DDTHH:mm" a datetime-local input wants,
// in the viewer's local time. Empty/invalid -> ''.
export const isoToDatetimeLocal = (iso: string | null): string => {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

// Field-level validation shared by both forms. Returns a map of field -> message;
// empty means valid.
export const validateOfferForm = (form: FormState, t: TFunction): Record<string, string> => {
    const errors: Record<string, string> = {};
    if (!form.department) errors.department = t('offers.create.errors.departmentRequired');
    if (!form.title.trim()) errors.title = t('offers.create.errors.titleRequired');
    else if (form.title.trim().length > 100) errors.title = t('offers.create.errors.titleTooLong');

    if (!form.applyingDeadline) {
        errors.applyingDeadline = t('offers.create.errors.deadlineRequired');
    } else if (new Date(form.applyingDeadline).getTime() <= Date.now()) {
        errors.applyingDeadline = t('offers.create.errors.deadlineFuture');
    }

    const min = toIntOrNull(form.salaryMin);
    const max = toIntOrNull(form.salaryMax);
    if (min != null && max != null && min > max) {
        errors.salaryMax = t('offers.create.errors.salaryRange');
    }

    const positions = toIntOrNull(form.openPositions);
    if (positions == null || positions < 1) {
        errors.openPositions = t('offers.create.errors.openPositions');
    }
    return errors;
};

const cleanSkills = (skills: SkillRow[]): SkillRow[] =>
    skills.filter((s) => s.name.trim() !== '').map((s) => ({name: s.name.trim(), importance: s.importance}));

// FormState -> CreateOfferInput (create flow: every field is sent).
export const formToCreateInput = (form: FormState, skills: SkillRow[]): CreateOfferInput => ({
    department: form.department,
    title: form.title.trim(),
    description: form.description.trim() || null,
    employmentType: form.employmentType,
    contractType: form.contractType,
    experienceLevel: form.experienceLevel || null,
    minimumExperienceYears: toIntOrNull(form.minimumExperienceYears),
    openPositions: toIntOrNull(form.openPositions) ?? 1,
    location: form.location || null,
    remotePolicy: form.remotePolicy,
    salaryMin: toIntOrNull(form.salaryMin),
    salaryMax: toIntOrNull(form.salaryMax),
    salaryCurrency: form.salaryCurrency || null,
    salaryPeriod: form.salaryPeriod || null,
    salaryNegotiable: form.salaryNegotiable,
    applyingDeadline: new Date(form.applyingDeadline).toISOString(),
    plannedStartDate: form.plannedStartDate || null,
    skills: cleanSkills(skills),
});

// FormState -> EditOfferInput (edit flow: the three fields the read model can't
// show are intentionally excluded so the PATCH preserves them — see EditOfferInput).
export const formToEditInput = (form: FormState, skills: SkillRow[]): EditOfferInput => ({
    department: form.department,
    title: form.title.trim(),
    description: form.description.trim() || null,
    employmentType: form.employmentType,
    contractType: form.contractType,
    experienceLevel: form.experienceLevel || null,
    openPositions: toIntOrNull(form.openPositions) ?? 1,
    location: form.location || null,
    remotePolicy: form.remotePolicy,
    salaryMin: toIntOrNull(form.salaryMin),
    salaryMax: toIntOrNull(form.salaryMax),
    salaryCurrency: form.salaryCurrency || null,
    salaryPeriod: form.salaryPeriod || null,
    applyingDeadline: new Date(form.applyingDeadline).toISOString(),
    skills: cleanSkills(skills),
});

// Existing offer -> form seed for the edit page. Fields the read model omits
// (minimumExperienceYears, plannedStartDate, salaryNegotiable) stay at their
// defaults; the edit form neither shows nor submits them.
export const offerToFormState = (offer: Offer): {form: FormState; skills: SkillRow[]} => ({
    form: {
        ...initialState,
        department: offer.department.name,
        title: offer.title,
        description: offer.description ?? '',
        employmentType: offer.employmentType,
        contractType: offer.contractType,
        experienceLevel: offer.experienceLevel ?? '',
        openPositions: String(offer.openPositions),
        remotePolicy: offer.remotePolicy,
        location: offer.location ?? '',
        salaryMin: offer.salaryMin != null ? String(offer.salaryMin) : '',
        salaryMax: offer.salaryMax != null ? String(offer.salaryMax) : '',
        salaryCurrency: offer.salaryCurrency ?? '',
        salaryPeriod: offer.salaryPeriod ?? '',
        applyingDeadline: isoToDatetimeLocal(offer.applyingDeadline),
    },
    skills: offer.skills.length
        ? offer.skills.map((s) => ({name: s.name, importance: s.importance}))
        : initialSkills,
});

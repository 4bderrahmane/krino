import type {
    ContractType,
    EmploymentType,
    ExperienceLevel,
    MoroccanCity,
    Offer,
    OfferFilters,
    RemotePolicy,
    SalaryCurrency,
    SalaryPeriod,
} from '@/features/offers/types/offer.types.ts';

// Canonical option orders (mirror the backend enum declaration order) so the
// filter panel lists values predictably regardless of the data sample.
export const REMOTE_POLICY_ORDER: readonly RemotePolicy[] = ['ON_SITE', 'HYBRID', 'REMOTE'];
export const EMPLOYMENT_TYPE_ORDER: readonly EmploymentType[] = ['FULL_TIME', 'PART_TIME'];
export const CONTRACT_TYPE_ORDER: readonly ContractType[] = ['PERMANENT', 'FIXED_TERM', 'INTERNSHIP', 'FREELANCE'];
export const EXPERIENCE_LEVEL_ORDER: readonly ExperienceLevel[] = [
    'INTERN', 'ENTRY_LEVEL', 'JUNIOR', 'MID_LEVEL', 'SENIOR', 'LEAD', 'MANAGER',
];

export const defaultOfferFilters: OfferFilters = {
    search: '',
    department: '',
    city: '',
    remotePolicies: [],
    employmentTypes: [],
    contractTypes: [],
    experienceLevels: [],
    disclosedSalaryOnly: false,
    minSalary: 0,
};

export interface SalaryFacet {
    currency: SalaryCurrency;
    period: SalaryPeriod;
    min: number;
    max: number;
}

export interface OfferFacets {
    departments: string[];
    cities: MoroccanCity[];
    remotePolicies: RemotePolicy[];
    employmentTypes: EmploymentType[];
    contractTypes: ContractType[];
    experienceLevels: ExperienceLevel[];
    // Present only when at least one offer discloses a salary. The slider works
    // within the most common currency+period so amounts compare like-for-like.
    salary: SalaryFacet | null;
}

const present = <T>(order: readonly T[], values: Set<T>): T[] => order.filter((v) => values.has(v));

const hasDisclosedSalary = (o: Offer): boolean =>
    o.salaryMin != null || o.salaryMax != null;

// The figure a "minimum salary" filter compares against: the top of the range,
// falling back to whichever single bound exists.
const salaryCeiling = (o: Offer): number => o.salaryMax ?? o.salaryMin ?? 0;

export const deriveOfferFacets = (offers: Offer[]): OfferFacets => {
    const remote = new Set<RemotePolicy>();
    const employment = new Set<EmploymentType>();
    const contract = new Set<ContractType>();
    const experience = new Set<ExperienceLevel>();
    const departments = new Set<string>();
    const cities = new Set<MoroccanCity>();
    const comboCounts = new Map<string, number>();

    for (const o of offers) {
        remote.add(o.remotePolicy);
        employment.add(o.employmentType);
        contract.add(o.contractType);
        if (o.experienceLevel) experience.add(o.experienceLevel);
        departments.add(o.department.name);
        if (o.location) cities.add(o.location);
        if (hasDisclosedSalary(o) && o.salaryCurrency && o.salaryPeriod) {
            const key = `${o.salaryCurrency}|${o.salaryPeriod}`;
            comboCounts.set(key, (comboCounts.get(key) ?? 0) + 1);
        }
    }

    // Pick the most common currency+period as the salary slider's basis.
    let bestKey: string | null = null;
    let bestCount = 0;
    for (const [key, count] of comboCounts) {
        if (count > bestCount) {
            bestCount = count;
            bestKey = key;
        }
    }

    let salary: SalaryFacet | null = null;
    if (bestKey) {
        const [currency, period] = bestKey.split('|') as [SalaryCurrency, SalaryPeriod];
        let min = Infinity;
        let max = 0;
        for (const o of offers) {
            if (hasDisclosedSalary(o) && o.salaryCurrency === currency && o.salaryPeriod === period) {
                min = Math.min(min, o.salaryMin ?? o.salaryMax ?? 0);
                max = Math.max(max, salaryCeiling(o));
            }
        }
        if (max > 0) salary = {currency, period, min: min === Infinity ? 0 : min, max};
    }

    return {
        departments: Array.from(departments).sort((a, b) => a.localeCompare(b)),
        cities: Array.from(cities),
        remotePolicies: present(REMOTE_POLICY_ORDER, remote),
        employmentTypes: present(EMPLOYMENT_TYPE_ORDER, employment),
        contractTypes: present(CONTRACT_TYPE_ORDER, contract),
        experienceLevels: present(EXPERIENCE_LEVEL_ORDER, experience),
        salary,
    };
};

export const applyOfferFilters = (
    offers: Offer[],
    filters: OfferFilters,
    salary: SalaryFacet | null,
): Offer[] => {
    const query = filters.search.trim().toLowerCase();

    return offers.filter((o) => {
        if (query) {
            const haystack = [o.title, o.department.name, ...o.skills.map((s) => s.name)]
                .join(' ')
                .toLowerCase();
            if (!haystack.includes(query)) return false;
        }
        if (filters.department && o.department.name !== filters.department) return false;
        if (filters.city && o.location !== filters.city) return false;
        if (filters.remotePolicies.length && !filters.remotePolicies.includes(o.remotePolicy)) return false;
        if (filters.employmentTypes.length && !filters.employmentTypes.includes(o.employmentType)) return false;
        if (filters.contractTypes.length && !filters.contractTypes.includes(o.contractType)) return false;
        if (filters.experienceLevels.length
            && (!o.experienceLevel || !filters.experienceLevels.includes(o.experienceLevel))) {
            return false;
        }
        if (filters.disclosedSalaryOnly && !hasDisclosedSalary(o)) return false;
        if (filters.minSalary > 0) {
            // Compare like-for-like: only offers in the slider's currency+period.
            if (!salary || !hasDisclosedSalary(o)) return false;
            if (o.salaryCurrency !== salary.currency || o.salaryPeriod !== salary.period) return false;
            if (salaryCeiling(o) < filters.minSalary) return false;
        }
        return true;
    });
};

export const countActiveFilters = (filters: OfferFilters): number =>
    (filters.search.trim() ? 1 : 0)
    + (filters.department ? 1 : 0)
    + (filters.city ? 1 : 0)
    + filters.remotePolicies.length
    + filters.employmentTypes.length
    + filters.contractTypes.length
    + filters.experienceLevels.length
    + (filters.disclosedSalaryOnly ? 1 : 0)
    + (filters.minSalary > 0 ? 1 : 0);

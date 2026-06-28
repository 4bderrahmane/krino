// Ordered option lists for offer forms. The classification orders already exist
// for the filter rail (filterOffers.ts) — re-export them here so the create form
// and the filters never drift apart, and add the remaining enums the form needs.
import {
    CONTRACT_TYPE_ORDER,
    EMPLOYMENT_TYPE_ORDER,
    EXPERIENCE_LEVEL_ORDER,
    REMOTE_POLICY_ORDER,
} from '@/features/offers/utils/filterOffers.ts';
import type {
    MoroccanCity,
    SalaryCurrency,
    SalaryPeriod,
    SkillImportance,
} from '@/features/offers/types/offer.types.ts';

export {
    CONTRACT_TYPE_ORDER,
    EMPLOYMENT_TYPE_ORDER,
    EXPERIENCE_LEVEL_ORDER,
    REMOTE_POLICY_ORDER,
};

export const SALARY_CURRENCY_ORDER: readonly SalaryCurrency[] = ['MAD', 'EUR', 'USD', 'GBP'];

export const SALARY_PERIOD_ORDER: readonly SalaryPeriod[] = ['HOURLY', 'DAILY', 'MONTHLY', 'YEARLY'];

export const SKILL_IMPORTANCE_ORDER: readonly SkillImportance[] = ['REQUIRED', 'PREFERRED'];

// Mirrors the backend MoroccanCity enum declaration order (alphabetical).
export const MOROCCAN_CITY_ORDER: readonly MoroccanCity[] = [
    'AGADIR', 'AL_HOCEIMA', 'BENI_MELLAL', 'BERRECHID', 'CASABLANCA', 'CHEFCHAOUEN',
    'DAKHLA', 'EL_JADIDA', 'ERRACHIDIA', 'ESSAOUIRA', 'FES', 'GUELMIM', 'INEZGANE',
    'KENITRA', 'KHOURIBGA', 'LAAYOUNE', 'LARACHE', 'MARRAKECH', 'MEKNES', 'MOHAMMEDIA',
    'NADOR', 'OUARZAZATE', 'OUJDA', 'RABAT', 'SAFI', 'SALE', 'SETTAT', 'TANGIER',
    'TAROUDANT', 'TAZA', 'TEMARA', 'TETOUAN',
];

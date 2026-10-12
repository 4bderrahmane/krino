import {describe, expect, it} from 'vitest';
import {
    applyOfferFilters,
    countActiveFilters,
    defaultOfferFilters,
    deriveOfferFacets,
} from '@/features/offers/utils/filterOffers.ts';
import type {OfferFilters} from '@/features/offers/types/offer.types.ts';
import {anOffer, anOfferWithoutSalary} from '@/test/fixtures.ts';

const withFilters = (overrides: Partial<OfferFilters>): OfferFilters => ({
    ...defaultOfferFilters,
    ...overrides,
});

describe('deriveOfferFacets', () => {
    it('returns empty facets and no salary basis for an empty catalogue', () => {
        const facets = deriveOfferFacets([]);

        expect(facets.departments).toEqual([]);
        expect(facets.remotePolicies).toEqual([]);
        expect(facets.salary).toBeNull();
    });

    it('lists departments alphabetically regardless of the order they appear in', () => {
        const facets = deriveOfferFacets([
            anOffer({department: {id: 'd1', name: 'Sales', description: null}}),
            anOffer({department: {id: 'd2', name: 'Engineering', description: null}}),
            anOffer({department: {id: 'd3', name: 'Marketing', description: null}}),
        ]);

        expect(facets.departments).toEqual(['Engineering', 'Marketing', 'Sales']);
    });

    it('deduplicates departments', () => {
        const facets = deriveOfferFacets([anOffer(), anOffer(), anOffer()]);

        expect(facets.departments).toEqual(['Engineering']);
    });

    // The panel must read in a stable order, not in whatever order the sample
    // happened to arrive in.
    it('orders enum facets canonically rather than by first appearance', () => {
        const facets = deriveOfferFacets([
            anOffer({remotePolicy: 'REMOTE', experienceLevel: 'MANAGER'}),
            anOffer({remotePolicy: 'ON_SITE', experienceLevel: 'INTERN'}),
            anOffer({remotePolicy: 'HYBRID', experienceLevel: 'SENIOR'}),
        ]);

        expect(facets.remotePolicies).toEqual(['ON_SITE', 'HYBRID', 'REMOTE']);
        expect(facets.experienceLevels).toEqual(['INTERN', 'SENIOR', 'MANAGER']);
    });

    it('omits an unset experience level from the facet', () => {
        const facets = deriveOfferFacets([anOffer({experienceLevel: null})]);

        expect(facets.experienceLevels).toEqual([]);
    });

    it('omits a null location from the cities facet', () => {
        const facets = deriveOfferFacets([anOffer({location: null, remotePolicy: 'REMOTE'})]);

        expect(facets.cities).toEqual([]);
    });

    // The slider can only compare like-for-like, so it picks one currency+period.
    it('bases the salary facet on the most common currency and period', () => {
        const facets = deriveOfferFacets([
            anOffer({salaryMin: 10000, salaryMax: 20000, salaryCurrency: 'MAD', salaryPeriod: 'MONTHLY'}),
            anOffer({salaryMin: 15000, salaryMax: 35000, salaryCurrency: 'MAD', salaryPeriod: 'MONTHLY'}),
            anOffer({salaryMin: 90000, salaryMax: 120000, salaryCurrency: 'EUR', salaryPeriod: 'YEARLY'}),
        ]);

        expect(facets.salary).toEqual({currency: 'MAD', period: 'MONTHLY', min: 10000, max: 35000});
    });

    it('has no salary facet when nothing discloses a salary', () => {
        const facets = deriveOfferFacets([anOfferWithoutSalary(), anOfferWithoutSalary()]);

        expect(facets.salary).toBeNull();
    });

    it('derives the range from a single bound when only one is set', () => {
        const facets = deriveOfferFacets([
            anOffer({salaryMin: 12000, salaryMax: null, salaryCurrency: 'MAD', salaryPeriod: 'MONTHLY'}),
        ]);

        expect(facets.salary).toMatchObject({min: 12000, max: 12000});
    });
});

describe('applyOfferFilters', () => {
    const facets = deriveOfferFacets([anOffer()]);

    it('returns everything when no filter is set', () => {
        const offers = [anOffer(), anOffer({title: 'Designer'})];

        expect(applyOfferFilters(offers, defaultOfferFilters, facets.salary)).toHaveLength(2);
    });

    it('searches title, department and skill names, case-insensitively', () => {
        const byTitle = anOffer({title: 'Platform Engineer'});
        const byDepartment = anOffer({title: 'Recruiter', department: {id: 'd', name: 'Platform', description: null}});
        const bySkill = anOffer({
            title: 'Analyst',
            department: {id: 'd', name: 'Finance', description: null},
            skills: [{name: 'Platform tooling', slug: 'platform-tooling', importance: 'REQUIRED'}],
        });
        const unrelated = anOffer({title: 'Accountant', department: {id: 'd', name: 'Finance', description: null}});

        const result = applyOfferFilters(
            [byTitle, byDepartment, bySkill, unrelated],
            withFilters({search: '  pLaTfOrM  '}),
            facets.salary,
        );

        expect(result).toEqual([byTitle, byDepartment, bySkill]);
    });

    it('filters by department and by city exactly', () => {
        const engineering = anOffer({location: 'RABAT'});
        const sales = anOffer({
            department: {id: 'd', name: 'Sales', description: null},
            location: 'CASABLANCA',
        });

        expect(applyOfferFilters([engineering, sales], withFilters({department: 'Sales'}), facets.salary))
            .toEqual([sales]);
        expect(applyOfferFilters([engineering, sales], withFilters({city: 'RABAT'}), facets.salary))
            .toEqual([engineering]);
    });

    it('treats a multi-select as "any of the selected"', () => {
        const onSite = anOffer({remotePolicy: 'ON_SITE'});
        const hybrid = anOffer({remotePolicy: 'HYBRID'});
        const remote = anOffer({remotePolicy: 'REMOTE', location: null});

        const result = applyOfferFilters(
            [onSite, hybrid, remote],
            withFilters({remotePolicies: ['ON_SITE', 'REMOTE']}),
            facets.salary,
        );

        expect(result).toEqual([onSite, remote]);
    });

    it('excludes offers with no experience level when levels are selected', () => {
        const senior = anOffer({experienceLevel: 'SENIOR'});
        const unspecified = anOffer({experienceLevel: null});

        const result = applyOfferFilters(
            [senior, unspecified],
            withFilters({experienceLevels: ['SENIOR']}),
            facets.salary,
        );

        expect(result).toEqual([senior]);
    });

    it('keeps only disclosed salaries when asked', () => {
        const disclosed = anOffer();
        const undisclosed = anOfferWithoutSalary();

        const result = applyOfferFilters(
            [disclosed, undisclosed],
            withFilters({disclosedSalaryOnly: true}),
            facets.salary,
        );

        expect(result).toEqual([disclosed]);
    });

    it('compares a minimum salary against the top of the range', () => {
        const salary = {currency: 'MAD', period: 'MONTHLY', min: 0, max: 50000} as const;
        const upTo30k = anOffer({salaryMin: 20000, salaryMax: 30000});
        const upTo15k = anOffer({salaryMin: 10000, salaryMax: 15000});

        const result = applyOfferFilters([upTo30k, upTo15k], withFilters({minSalary: 25000}), salary);

        expect(result).toEqual([upTo30k]);
    });

    // A 90,000 EUR/year role must not be filtered out (or in) by a MAD/month slider.
    it('drops offers in another currency or period once a minimum salary is set', () => {
        const salary = {currency: 'MAD', period: 'MONTHLY', min: 0, max: 50000} as const;
        const monthlyMad = anOffer({salaryMin: 20000, salaryMax: 30000});
        const yearlyEur = anOffer({
            salaryMin: 90000,
            salaryMax: 120000,
            salaryCurrency: 'EUR',
            salaryPeriod: 'YEARLY',
        });

        const result = applyOfferFilters([monthlyMad, yearlyEur], withFilters({minSalary: 25000}), salary);

        expect(result).toEqual([monthlyMad]);
    });

    it('matches nothing on a minimum salary when there is no salary basis at all', () => {
        const result = applyOfferFilters([anOffer()], withFilters({minSalary: 1}), null);

        expect(result).toEqual([]);
    });

    it('applies filters together rather than as alternatives', () => {
        const wanted = anOffer({remotePolicy: 'REMOTE', location: null, title: 'Remote Platform Engineer'});
        const rightRoleWrongPolicy = anOffer({remotePolicy: 'ON_SITE', title: 'Platform Engineer'});
        const rightPolicyWrongRole = anOffer({remotePolicy: 'REMOTE', location: null, title: 'Recruiter'});

        const result = applyOfferFilters(
            [wanted, rightRoleWrongPolicy, rightPolicyWrongRole],
            withFilters({search: 'platform', remotePolicies: ['REMOTE']}),
            facets.salary,
        );

        expect(result).toEqual([wanted]);
    });
});

describe('countActiveFilters', () => {
    it('counts nothing for the default filters', () => {
        expect(countActiveFilters(defaultOfferFilters)).toBe(0);
    });

    it('ignores a search box holding only whitespace', () => {
        expect(countActiveFilters(withFilters({search: '   '}))).toBe(0);
    });

    // The badge has to agree with the removable chips the page renders, and each
    // selected value is its own chip.
    it('counts each selected value in a multi-select', () => {
        expect(countActiveFilters(withFilters({
            remotePolicies: ['ON_SITE', 'REMOTE'],
            contractTypes: ['PERMANENT'],
        }))).toBe(3);
    });

    it('counts every distinct kind of filter once', () => {
        expect(countActiveFilters(withFilters({
            search: 'engineer',
            department: 'Engineering',
            city: 'RABAT',
            remotePolicies: ['REMOTE'],
            employmentTypes: ['FULL_TIME'],
            contractTypes: ['PERMANENT'],
            experienceLevels: ['SENIOR'],
            disclosedSalaryOnly: true,
            minSalary: 10000,
        }))).toBe(9);
    });
});

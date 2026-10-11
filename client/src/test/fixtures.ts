import type {Offer} from '@/features/offers/types/offer.types.ts';

/**
 * Test data builders.
 *
 * Each builder returns a complete, valid object and takes overrides, so a test
 * states only the fields it actually cares about and the rest cannot drift into
 * being accidentally significant.
 */

let offerCounter = 0;

export const anOffer = (overrides: Partial<Offer> = {}): Offer => {
    offerCounter += 1;
    return {
        id: `offer-${offerCounter}`,
        title: 'Backend Engineer',
        description: 'Build and run the platform services.',
        department: {id: `dept-${offerCounter}`, name: 'Engineering', description: null},
        employmentType: 'FULL_TIME',
        contractType: 'PERMANENT',
        experienceLevel: 'SENIOR',
        openPositions: 1,
        status: 'OPEN',
        location: 'CASABLANCA',
        remotePolicy: 'ON_SITE',
        salaryMin: 20000,
        salaryMax: 30000,
        salaryCurrency: 'MAD',
        salaryPeriod: 'MONTHLY',
        applyingDeadline: '2099-12-31T23:59:00.000Z',
        skills: [],
        ...overrides,
    };
};

/**
 * The application payload the backend returns, in its own vocabulary ("job", not
 * "offer"). Complete on purpose: the service mapper reads nested fields such as
 * candidate.firstName, so a partial stub throws inside the client rather than
 * failing the assertion the test meant to make.
 */
export const anApplicationResponse = (overrides: Record<string, unknown> = {}) => ({
    id: 'application-1',
    jobId: 'offer-1',
    jobTitle: 'Backend Engineer',
    jobDepartment: 'Engineering',
    candidate: {
        id: 'candidate-1',
        firstName: 'Test',
        lastName: 'User',
        email: 'test@krino.local',
    },
    status: 'SUBMITTED',
    resume: null,
    appliedAt: '2026-08-06T10:00:00.000Z',
    ...overrides,
});

/** An offer with no salary disclosed, which several filter rules turn on. */
export const anOfferWithoutSalary = (overrides: Partial<Offer> = {}): Offer =>
    anOffer({
        salaryMin: null,
        salaryMax: null,
        salaryCurrency: null,
        salaryPeriod: null,
        ...overrides,
    });

import {setupServer} from 'msw/node';
import {http, HttpResponse} from 'msw';
import type {Role, UserResponseDTO} from '@/features/authentication/types/api.types';
import type {Offer} from '@/features/offers/types/offer.types.ts';

/**
 * Network-level stubbing.
 *
 * Handlers intercept HTTP rather than mocking the service modules, so the axios
 * instance and its interceptors in shared/services/api.ts run for real: CSRF
 * seeding, the single-flight refresh, the retry-once rule. Mocking OfferService
 * instead would skip precisely the code most likely to be wrong.
 *
 * Paths are matched with a leading wildcard because the client asks for "/api/..."
 * relative to whatever origin serves it, and the test origin is jsdom's.
 */
export const server = setupServer();

export const apiPath = (path: string): string => `*/api${path}`;

export const aUser = (roles: Role[], overrides: Partial<UserResponseDTO> = {}): UserResponseDTO => ({
    id: '11111111-2222-3333-4444-555555555555',
    version: 0,
    firstName: 'Test',
    lastName: 'User',
    email: 'test@krino.local',
    phoneNumber: '0600000000',
    roles,
    ...overrides,
});

/** Nobody is signed in: the session probe and the refresh that follows both fail. */
export const anonymousSession = () => [
    http.get(apiPath('/auth/csrf'), () =>
        HttpResponse.json({cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN'})),
    http.get(apiPath('/users/me'), () => new HttpResponse(null, {status: 401})),
    http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 401})),
];

/** A signed-in user, with the CSRF seed their mutations would need. */
export const signedInSession = (user: UserResponseDTO) => [
    http.get(apiPath('/users/me'), () => HttpResponse.json(user)),
    http.get(apiPath('/auth/csrf'), () =>
        HttpResponse.json({cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN'})),
];

/** A page of offers from whichever catalogue endpoint the caller is entitled to. */
export const catalogue = (offers: Offer[]) => ({
    content: offers,
    page: {number: 0, size: 100, totalElements: offers.length, totalPages: 1},
});

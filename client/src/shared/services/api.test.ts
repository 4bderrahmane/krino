import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {http, HttpResponse} from 'msw';
import api from '@/shared/services/api.ts';
import {apiPath, server} from '@/test/msw/server';

/**
 * The session machinery in api.ts: refresh-on-401, single-flight rotation, and the
 * rule about when a dead session should bounce the user to the login screen.
 *
 * These go through MSW rather than mocking axios, because the interceptors are the
 * subject: a mocked axios would assert only that the test's own stub was called.
 */

/** Records the API paths that actually left the client, in order. */
const recordRequests = (): string[] => {
    const paths: string[] = [];
    server.events.on('request:start', ({request}) => {
        const {pathname} = new URL(request.url);
        if (pathname.startsWith('/api/')) paths.push(pathname);
    });
    return paths;
};

/**
 * Replaces window.location so the module's redirect is observable instead of being
 * a jsdom navigation error, and so the "which page am I on" test can vary.
 */
const stubLocation = (pathname: string) => {
    const location = {
        pathname,
        origin: 'http://localhost:3000',
        href: `http://localhost:3000${pathname}`,
    };
    vi.stubGlobal('location', location);
    return location;
};

const csrfSeed = () =>
    http.get(apiPath('/auth/csrf'), () =>
        HttpResponse.json({cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN'}));

beforeEach(() => {
    stubLocation('/dashboard');
});

afterEach(() => {
    vi.unstubAllGlobals();
    server.events.removeAllListeners();
});

describe('refreshing an expired access token', () => {
    it('refreshes once and replays the original request', async () => {
        let attempts = 0;
        server.use(
            csrfSeed(),
            http.get(apiPath('/applications'), () => {
                attempts += 1;
                return attempts === 1
                    ? new HttpResponse(null, {status: 401})
                    : HttpResponse.json({content: []});
            }),
            http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 204})),
        );

        const response = await api.get('/applications');

        expect(response.data).toEqual({content: []});
        expect(attempts).toBe(2);
    });

    // The refresh token is single-use and rotates. Two requests expiring together
    // must not rotate it twice, or the second rotation invalidates the first.
    it('shares one refresh between requests that fail together', async () => {
        const paths = recordRequests();
        let refreshes = 0;
        const expiredOnce = new Set<string>();

        server.use(
            csrfSeed(),
            http.post(apiPath('/auth/refresh'), () => {
                refreshes += 1;
                return new HttpResponse(null, {status: 204});
            }),
            http.get(apiPath('/applications'), () => respondOnceWith401('applications', expiredOnce)),
            http.get(apiPath('/interviews'), () => respondOnceWith401('interviews', expiredOnce)),
        );

        await Promise.all([api.get('/applications'), api.get('/interviews')]);

        expect(refreshes).toBe(1);
        expect(paths.filter((p) => p === '/api/auth/refresh')).toHaveLength(1);
    });

    it('gives up rather than looping when the replayed request fails again', async () => {
        let attempts = 0;
        server.use(
            csrfSeed(),
            http.get(apiPath('/applications'), () => {
                attempts += 1;
                return new HttpResponse(null, {status: 401});
            }),
            http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 204})),
        );

        await expect(api.get('/applications')).rejects.toMatchObject({response: {status: 401}});
        expect(attempts).toBe(2);
    });
});

describe('ending a session', () => {
    it('sends the user to the login screen when the refresh token is rejected', async () => {
        const location = stubLocation('/dashboard');
        server.use(
            csrfSeed(),
            http.get(apiPath('/applications'), () => new HttpResponse(null, {status: 401})),
            http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 401})),
        );

        await expect(api.get('/applications')).rejects.toBeDefined();

        expect(location.href).toBe('/login');
    });

    // The bug this guards: a visitor browsing the public catalogue has no session,
    // so the on-load probe always 401s. Bouncing them would make the storefront
    // unreachable to exactly the people it exists for.
    it.each(['/offers', '/offers/8d6cdff1-e1df-4ff4-831b-709cf2989f95', '/login', '/'])(
        'leaves a visitor alone on %s',
        async (pathname) => {
            const location = stubLocation(pathname);
            const unchanged = location.href;
            server.use(
                csrfSeed(),
                http.get(apiPath('/users/me'), () => new HttpResponse(null, {status: 401})),
                http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 401})),
            );

            await expect(api.get('/users/me')).rejects.toBeDefined();

            expect(location.href).toBe(unchanged);
        },
    );

    // Staff pages living under the public prefix are not public.
    it('still bounces from a guarded page nested under the public prefix', async () => {
        const location = stubLocation('/offers/new');
        server.use(
            csrfSeed(),
            http.get(apiPath('/users/me'), () => new HttpResponse(null, {status: 401})),
            http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 401})),
        );

        await expect(api.get('/users/me')).rejects.toBeDefined();

        expect(location.href).toBe('/login');
    });

    it('keeps the session through a transient refresh failure', async () => {
        const location = stubLocation('/dashboard');
        const unchanged = location.href;
        server.use(
            csrfSeed(),
            http.get(apiPath('/applications'), () => new HttpResponse(null, {status: 401})),
            // A 5xx is a blip, not a verdict on the session.
            http.post(apiPath('/auth/refresh'), () => new HttpResponse(null, {status: 503})),
        );

        await expect(api.get('/applications')).rejects.toBeDefined();

        expect(location.href).toBe(unchanged);
    });
});

describe('CSRF seeding', () => {
    it('fetches a token before a mutating request', async () => {
        const paths = recordRequests();
        server.use(
            csrfSeed(),
            http.post(apiPath('/departments'), () => HttpResponse.json({id: 'd1'}, {status: 201})),
        );

        await api.post('/departments', {name: 'Engineering'});

        expect(paths).toEqual(['/api/auth/csrf', '/api/departments']);
    });

    it('does not fetch one for a read', async () => {
        const paths = recordRequests();
        server.use(http.get(apiPath('/public/jobs'), () => HttpResponse.json({content: [], page: {}})));

        await api.get('/public/jobs');

        expect(paths).toEqual(['/api/public/jobs']);
    });
});

/** 401 the first time a given endpoint is asked, then succeed. */
function respondOnceWith401(key: string, seen: Set<string>): Response {
    if (seen.has(key)) return HttpResponse.json({content: []});
    seen.add(key);
    return new HttpResponse(null, {status: 401});
}

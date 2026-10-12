import {describe, expect, it} from 'vitest';
import {screen, waitFor, act} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import {anonymousSession, apiPath, aUser, catalogue, server, signedInSession} from '@/test/msw/server';
import {renderApp} from '@/test/renderWithProviders';
import {anOffer} from '@/test/fixtures.ts';

/**
 * The public/guarded split.
 *
 * A visitor must reach the catalogue without an account, and must still be stopped
 * everywhere else. Both halves are asserted here because the failure mode is silent:
 * the app looks fine to anyone already signed in, which is everyone developing it.
 */

const publicCatalogue = () =>
    http.get(apiPath('/public/jobs'), () =>
        HttpResponse.json(catalogue([anOffer({title: 'Backend Engineer'})])));

const staffCatalogue = () =>
    http.get(apiPath('/jobs'), () =>
        HttpResponse.json(catalogue([anOffer({title: 'Unpublished Draft', status: 'DRAFT'})])));

// The directory has the same two faces as the catalogue: departments with an opening,
// and the whole internal list including ones whose work is unannounced.
const publicDirectory = () =>
    http.get(apiPath('/public/departments'), () =>
        HttpResponse.json([{id: 'dept-open', name: 'Engineering', description: null}]));

const staffDirectory = () =>
    http.get(apiPath('/departments'), () =>
        HttpResponse.json([{id: 'dept-quiet', name: 'Skunkworks', description: null}]));

describe('routing for a visitor with no session', () => {
    it('sends the site root to the catalogue rather than the login screen', async () => {
        server.use(...anonymousSession(), publicCatalogue());

        const {currentPath} = renderApp('/');

        await waitFor(() => expect(currentPath()).toBe('/offers'));
    });

    it('renders the catalogue at /offers', async () => {
        server.use(...anonymousSession(), publicCatalogue());

        renderApp('/offers');

        expect(await screen.findByText('Backend Engineer')).toBeInTheDocument();
    });

    it('reads the published catalogue, never the staff one', async () => {
        const requested: string[] = [];
        server.events.on('request:start', ({request}) => {
            const {pathname} = new URL(request.url);
            if (pathname.startsWith('/api/')) requested.push(pathname);
        });
        server.use(...anonymousSession(), publicCatalogue(), staffCatalogue());

        renderApp('/offers');
        await screen.findByText('Backend Engineer');

        expect(requested).toContain('/api/public/jobs');
        expect(requested).not.toContain('/api/jobs');
        server.events.removeAllListeners();
    });

    it('renders the department directory at /departments', async () => {
        server.use(...anonymousSession(), publicDirectory());

        const {currentPath} = renderApp('/departments');

        expect(await screen.findByText('Engineering')).toBeInTheDocument();
        expect(currentPath()).toBe('/departments');
    });

    it('reads the public directory, never the internal one', async () => {
        const requested: string[] = [];
        server.events.on('request:start', ({request}) => {
            const {pathname} = new URL(request.url);
            if (pathname.startsWith('/api/')) requested.push(pathname);
        });
        server.use(...anonymousSession(), publicDirectory(), staffDirectory());

        renderApp('/departments');
        await screen.findByText('Engineering');

        expect(requested).toContain('/api/public/departments');
        expect(requested).not.toContain('/api/departments');
        server.events.removeAllListeners();
    });

    it('offers a way in from the catalogue, carrying a return path', async () => {
        server.use(...anonymousSession(), publicCatalogue());

        renderApp('/offers');
        await screen.findByText('Backend Engineer');

        expect(screen.getByRole('link', {name: /sign in/i})).toHaveAttribute(
            'href', '/login?redirect=%2Foffers');
    });

    it('still guards the dashboard', async () => {
        server.use(...anonymousSession());

        const {currentPath} = renderApp('/dashboard');

        await waitFor(() => expect(currentPath()).toBe('/login'));
    });

    // /offers/new sits under the public prefix but is a staff page, so the prefix
    // must not be what decides access.
    it('still guards the create-offer page nested under the public prefix', async () => {
        server.use(...anonymousSession());

        const {currentPath} = renderApp('/offers/new');

        await waitFor(() => expect(currentPath()).toBe('/login'));
    });

    it('shows a not-found page for an unknown URL instead of demanding a login', async () => {
        server.use(...anonymousSession());

        const {currentPath} = renderApp('/no-such-page');

        await waitFor(() => expect(currentPath()).toBe('/no-such-page'));
    });
});

describe('routing for a signed-in user', () => {
    it('sends the site root to the dashboard', async () => {
        server.use(...signedInSession(aUser(['CANDIDATE'])));

        const {currentPath} = renderApp('/');

        await waitFor(() => expect(currentPath()).toBe('/dashboard'));
    });

    it('reads the published catalogue for a candidate', async () => {
        const requested: string[] = [];
        server.events.on('request:start', ({request}) => {
            const {pathname} = new URL(request.url);
            if (pathname.startsWith('/api/')) requested.push(pathname);
        });
        server.use(...signedInSession(aUser(['CANDIDATE'])), publicCatalogue(), staffCatalogue());

        renderApp('/offers');
        await screen.findByText('Backend Engineer');

        expect(requested).toContain('/api/public/jobs');
        expect(requested).not.toContain('/api/jobs');
        server.events.removeAllListeners();
    });

    // Staff are the only ones who should ever see an unpublished posting.
    it('reads the staff catalogue, drafts included, for an HR manager', async () => {
        server.use(...signedInSession(aUser(['HR_MANAGER'])), publicCatalogue(), staffCatalogue());

        renderApp('/offers');

        expect(await screen.findByText('Unpublished Draft')).toBeInTheDocument();
    });

    // The directory a candidate sees is the visitor's, not the staff one: signing in
    // must not widen it to departments with nothing published.
    it('reads the public directory for a candidate', async () => {
        server.use(...signedInSession(aUser(['CANDIDATE'])), publicDirectory(), staffDirectory());

        renderApp('/departments');

        expect(await screen.findByText('Engineering')).toBeInTheDocument();
        expect(screen.queryByText('Skunkworks')).not.toBeInTheDocument();
    });

    it('reads the internal directory, unannounced departments included, for an HR manager', async () => {
        server.use(...signedInSession(aUser(['HR_MANAGER'])), publicDirectory(), staffDirectory());

        renderApp('/departments');

        expect(await screen.findByText('Skunkworks')).toBeInTheDocument();
    });
});


describe('mandatory initial password change', () => {
    it.each(['/dashboard', '/offers/new', '/offers', '/settings/profile'])(
        'redirects a restricted session from %s to password settings', async (path) => {
            server.use(...signedInSession(aUser(['HR_MANAGER'], {mustChangePassword: true})));
            const {currentPath} = renderApp(path);
            await waitFor(() => expect(currentPath()).toBe('/settings/password'));
            expect(await screen.findByRole('heading', {name: 'Change Password'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Later'})).not.toBeInTheDocument();
        },
    );

    it('unlocks navigation after a successful password change and account reload', async () => {
        const user = aUser(['HR_MANAGER'], {mustChangePassword: true});
        server.use(...signedInSession(user),
            http.put(apiPath('/users/me/password'), async ({request}) => {
                expect(await request.json()).toEqual({
                    currentPassword: 'InitialPassword123!', newPassword: 'NewPassword123!',
                    confirmNewPassword: 'NewPassword123!',
                });
                user.mustChangePassword = false;
                return new HttpResponse(null, {status: 204});
            }), staffCatalogue());
        const {router, currentPath} = renderApp('/settings/password');
        const events = userEvent.setup();
        await events.type(await screen.findByLabelText('Current Password'), 'InitialPassword123!');
        await events.type(screen.getByLabelText('New Password', {exact: true}), 'NewPassword123!');
        await events.type(screen.getByLabelText('Confirm New Password'), 'NewPassword123!');
        await events.click(screen.getByRole('button', {name: 'Change Password'}));
        await waitFor(() => expect(screen.queryByText(/Your account is still using the temporary password/)).not.toBeInTheDocument());
        await act(() => router.navigate('/offers'));
        expect(await screen.findByText('Unpublished Draft')).toBeInTheDocument();
        expect(currentPath()).toBe('/offers');
    });

    it('keeps the password form usable after an incorrect current password', async () => {
        let refreshCalls = 0;
        server.use(...signedInSession(aUser(['HR_MANAGER'], {mustChangePassword: true})),
            http.put(apiPath('/users/me/password'), () => HttpResponse.json(
                {errorCode: 'INVALID_CREDENTIALS', status: 401}, {status: 401})),
            http.post(apiPath('/auth/refresh'), () => {
                refreshCalls++;
                return new HttpResponse(null, {status: 403});
            }));
        const {currentPath} = renderApp('/settings/password');
        const events = userEvent.setup();
        await events.type(await screen.findByLabelText('Current Password'), 'WrongPassword123!');
        await events.type(screen.getByLabelText('New Password', {exact: true}), 'NewPassword123!');
        await events.type(screen.getByLabelText('Confirm New Password'), 'NewPassword123!');
        await events.click(screen.getByRole('button', {name: 'Change Password'}));
        expect(await screen.findByText('Your current password is incorrect.')).toBeInTheDocument();
        expect(refreshCalls).toBe(0);
        expect(currentPath()).toBe('/settings/password');
    });
});

import {describe, expect, it} from 'vitest';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import i18n from '@/utils/i18n';
import OfferDetailPage from '@/features/offers/components/OfferDetailPage.tsx';
import {anonymousSession, apiPath, aUser, server, signedInSession} from '@/test/msw/server';
import {renderWithProviders} from '@/test/renderWithProviders';
import {anApplicationResponse, anOffer} from '@/test/fixtures.ts';
import type {Role} from '@/features/authentication/types/api.types';
import type {Offer} from '@/features/offers/types/offer.types.ts';

/**
 * One page, four audiences. The details are readable by all of them; what differs
 * is the call to action, and getting that wrong either hides the apply form from a
 * candidate or shows lifecycle controls to someone who cannot use them.
 */

const t = i18n.getFixedT('en');

const OFFER = anOffer({title: 'Senior Backend Engineer', description: 'Run the platform.'});

const renderOffer = (offer: Offer = OFFER) =>
    renderWithProviders(<OfferDetailPage/>, {path: '/offers/:id', at: `/offers/${offer.id}`});

/** Serves the offer on whichever endpoint the audience is entitled to use. */
const offerOnBothEndpoints = (offer: Offer = OFFER) => [
    http.get(apiPath(`/public/jobs/${offer.id}`), () => HttpResponse.json(offer)),
    http.get(apiPath(`/jobs/${offer.id}`), () => HttpResponse.json(offer)),
];

const asVisitor = (offer: Offer = OFFER) =>
    server.use(...anonymousSession(), ...offerOnBothEndpoints(offer));

const asUser = (roles: Role[], overrides = {}, offer: Offer = OFFER) =>
    server.use(...signedInSession(aUser(roles, overrides)), ...offerOnBothEndpoints(offer),
        http.get(apiPath('/applications/me/by-job/:jobId'), () => new HttpResponse(null, {status: 404})));

const applyForm = () => screen.queryByRole('button', {name: t('offers.apply.submit')});
const managePanel = () => screen.queryByRole('heading', {name: t('offers.manage.heading')});
const signInPrompt = () => screen.queryByText(t('offers.apply.signInPrompt'));

describe('the offer itself', () => {
    it('is readable without an account', async () => {
        asVisitor();

        renderOffer();

        expect(await screen.findByRole('heading', {name: 'Senior Backend Engineer'})).toBeInTheDocument();
        expect(screen.getByText('Run the platform.')).toBeInTheDocument();
        expect(screen.getByText(t('offers.detail.salary'))).toBeInTheDocument();
    });

    it('shows an error and a way back when the offer cannot be loaded', async () => {
        server.use(
            ...anonymousSession(),
            http.get(apiPath(`/public/jobs/${OFFER.id}`), () => new HttpResponse(null, {status: 404})),
        );

        renderOffer();

        expect(await screen.findByText(t('offers.loadError'))).toBeInTheDocument();
        expect(screen.getByRole('link', {name: t('offers.detail.back')})).toHaveAttribute('href', '/offers');
    });
});

describe('a visitor with no account', () => {
    it('is invited to sign in, and sent back to this offer afterwards', async () => {
        asVisitor();

        renderOffer();

        expect(await screen.findByText(t('offers.apply.signInPrompt'))).toBeInTheDocument();
        expect(screen.getByRole('link', {name: t('auth.signIn')}))
            .toHaveAttribute('href', `/login?redirect=${encodeURIComponent(`/offers/${OFFER.id}`)}`);
        expect(screen.getByRole('link', {name: t('auth.signUp')})).toHaveAttribute('href', '/register');
    });

    it('gets neither the apply form nor the lifecycle controls', async () => {
        asVisitor();

        renderOffer();
        await screen.findByText(t('offers.apply.signInPrompt'));

        expect(applyForm()).not.toBeInTheDocument();
        expect(managePanel()).not.toBeInTheDocument();
    });

    // A visitor has no session, so the authenticated endpoint would 401 for them.
    it('reads the offer from the public endpoint', async () => {
        const requested: string[] = [];
        server.events.on('request:start', ({request}) => {
            const {pathname} = new URL(request.url);
            if (pathname.startsWith('/api/')) requested.push(pathname);
        });
        asVisitor();

        renderOffer();
        await screen.findByRole('heading', {name: 'Senior Backend Engineer'});

        expect(requested).toContain(`/api/public/jobs/${OFFER.id}`);
        expect(requested).not.toContain(`/api/jobs/${OFFER.id}`);
        server.events.removeAllListeners();
    });
});

describe('a candidate', () => {
    it('gets the apply form instead of the sign-in prompt', async () => {
        asUser(['CANDIDATE']);

        renderOffer();

        expect(await screen.findByRole('button', {name: t('offers.apply.submit')})).toBeInTheDocument();
        expect(signInPrompt()).not.toBeInTheDocument();
        expect(managePanel()).not.toBeInTheDocument();
    });

    it('is offered their stored CV when they have one', async () => {
        asUser(['CANDIDATE'], {resumeFilename: 'aya-cv.pdf'});

        renderOffer();

        expect(await screen.findByLabelText(t('offers.apply.useBaseCv', {name: 'aya-cv.pdf'})))
            .toBeInTheDocument();
    });

    it('is only offered an upload when there is no CV on file', async () => {
        asUser(['CANDIDATE'], {resumeFilename: null});

        renderOffer();

        expect(await screen.findByLabelText(t('offers.apply.uploadNew'))).toBeInTheDocument();
        expect(screen.queryByLabelText(/apply with my cv/i)).not.toBeInTheDocument();
    });

    // Applying is two calls: create the application, then attach a CV to it. A
    // half-finished apply leaves an application with no CV attached.
    it('creates the application and attaches the stored CV', async () => {
        const calls: string[] = [];
        asUser(['CANDIDATE'], {resumeFilename: 'aya-cv.pdf'});
        server.use(
            http.post(apiPath('/applications'), async ({request}) => {
                calls.push('create');
                await expect(request.json()).resolves.toEqual({jobPublicId: OFFER.id});
                return HttpResponse.json(
                    anApplicationResponse({id: 'app-1', jobId: OFFER.id}),
                    {status: 201},
                );
            }),
            http.put(apiPath('/applications/app-1/resume/from-base'), () => {
                calls.push('attach');
                return HttpResponse.json(anApplicationResponse({id: 'app-1', jobId: OFFER.id}));
            }),
        );

        renderOffer();
        await userEvent.click(await screen.findByRole('button', {name: t('offers.apply.submit')}));

        await waitFor(() => expect(screen.getByText(t('offers.apply.submitted'))).toBeInTheDocument());
        expect(calls).toEqual(['create', 'attach']);
    });

    it('explains a rejected application instead of claiming success', async () => {
        asUser(['CANDIDATE'], {resumeFilename: 'aya-cv.pdf'});
        server.use(
            http.post(apiPath('/applications'), () =>
                HttpResponse.json({errorCode: 'DATA_CONFLICT'}, {status: 409})),
        );

        renderOffer();
        await userEvent.click(await screen.findByRole('button', {name: t('offers.apply.submit')}));

        expect(await screen.findByText(t('codes.DATA_CONFLICT', {ns: 'errors'}))).toBeInTheDocument();
        expect(screen.queryByText(t('offers.apply.submitted'))).not.toBeInTheDocument();
    });

    it('retries a failed résumé attachment using the created application ID', async () => {
        let creates = 0;
        let attachments = 0;
        asUser(['CANDIDATE'], {resumeFilename: 'profile.pdf'});
        server.use(
            http.post(apiPath('/applications'), () => {
                creates += 1;
                return HttpResponse.json(anApplicationResponse({id: 'app-retry', jobId: OFFER.id}), {status: 201});
            }),
            http.put(apiPath('/applications/app-retry/resume/from-base'), () => {
                attachments += 1;
                return attachments === 1
                    ? HttpResponse.json({errorCode: 'STORAGE_ERROR'}, {status: 503})
                    : HttpResponse.json(anApplicationResponse({id: 'app-retry', jobId: OFFER.id,
                        resume: {originalFilename: 'profile.pdf'}}));
            }),
        );

        renderOffer();
        await userEvent.click(await screen.findByRole('button', {name: t('offers.apply.submit')}));
        expect(await screen.findByText(t('offers.apply.resumeMissing'))).toBeInTheDocument();
        expect(screen.getByRole('link', {name: t('offers.apply.viewApplication')}))
            .toHaveAttribute('href', '/applications/app-retry');
        await userEvent.click(screen.getByRole('button', {name: t('offers.apply.retryAttachment')}));

        expect(await screen.findByText(t('offers.apply.submitted'))).toBeInTheDocument();
        expect(creates).toBe(1);
        expect(attachments).toBe(2);
    });

    it('finds an incomplete application after reload and attaches without creating another', async () => {
        let creates = 0;
        let attachments = 0;
        asUser(['CANDIDATE'], {resumeFilename: 'profile.pdf'});
        server.use(
            http.get(apiPath(`/applications/me/by-job/${OFFER.id}`), () =>
                HttpResponse.json(anApplicationResponse({id: 'existing-app', jobId: OFFER.id, resume: null}))),
            http.post(apiPath('/applications'), () => {
                creates += 1;
                return new HttpResponse(null, {status: 500});
            }),
            http.put(apiPath('/applications/existing-app/resume/from-base'), () => {
                attachments += 1;
                return HttpResponse.json(anApplicationResponse({id: 'existing-app', jobId: OFFER.id,
                    resume: {originalFilename: 'profile.pdf'}}));
            }),
        );

        renderOffer();
        expect(await screen.findByText(t('offers.apply.resumeMissing'))).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', {name: t('offers.apply.retryAttachment')}));

        expect(await screen.findByText(t('offers.apply.submitted'))).toBeInTheDocument();
        expect(creates).toBe(0);
        expect(attachments).toBe(1);
    });

    it('does not create a duplicate when an application already has a résumé', async () => {
        let creates = 0;
        asUser(['CANDIDATE'], {resumeFilename: 'profile.pdf'});
        server.use(
            http.get(apiPath(`/applications/me/by-job/${OFFER.id}`), () =>
                HttpResponse.json(anApplicationResponse({id: 'complete-app', jobId: OFFER.id,
                    resume: {originalFilename: 'profile.pdf'}}))),
            http.post(apiPath('/applications'), () => {
                creates += 1;
                return new HttpResponse(null, {status: 500});
            }),
        );

        renderOffer();
        expect(await screen.findByText(t('offers.apply.alreadyApplied'))).toBeInTheDocument();
        expect(screen.getByRole('link', {name: t('offers.apply.viewApplication')}))
            .toHaveAttribute('href', '/applications/complete-app');
        expect(applyForm()).not.toBeInTheDocument();
        expect(creates).toBe(0);
    });

    it('recovers an application when the create response is lost', async () => {
        let created = false;
        let creates = 0;
        let attachments = 0;
        asUser(['CANDIDATE'], {resumeFilename: 'profile.pdf'});
        server.use(
            http.get(apiPath(`/applications/me/by-job/${OFFER.id}`), () => created
                ? HttpResponse.json(anApplicationResponse({id: 'recovered-app', jobId: OFFER.id}))
                : new HttpResponse(null, {status: 404})),
            http.post(apiPath('/applications'), () => {
                created = true;
                creates += 1;
                return HttpResponse.json({errorCode: 'DATA_CONFLICT'}, {status: 409});
            }),
            http.put(apiPath('/applications/recovered-app/resume/from-base'), () => {
                attachments += 1;
                return HttpResponse.json(anApplicationResponse({id: 'recovered-app', jobId: OFFER.id,
                    resume: {originalFilename: 'profile.pdf'}}));
            }),
        );

        renderOffer();
        await userEvent.click(await screen.findByRole('button', {name: t('offers.apply.submit')}));
        expect(await screen.findByText(t('offers.apply.resumeMissing'))).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', {name: t('offers.apply.retryAttachment')}));

        expect(await screen.findByText(t('offers.apply.submitted'))).toBeInTheDocument();
        expect(creates).toBe(1);
        expect(attachments).toBe(1);
    });

    it('refuses a CV that is not a PDF before sending anything', async () => {
        const calls: string[] = [];
        server.events.on('request:start', ({request}) => {
            const {pathname} = new URL(request.url);
            if (request.method !== 'GET' && pathname.startsWith('/api/applications')) calls.push(pathname);
        });
        asUser(['CANDIDATE'], {resumeFilename: null});

        renderOffer();
        const upload = await screen.findByLabelText(t('offers.apply.uploadNew'));
        await userEvent.click(upload);
        // applyAccept off on purpose: the input carries accept="application/pdf",
        // and user-event would otherwise discard the file before the component ever
        // sees it. A real file dialog can still hand over a non-PDF, which is the
        // whole reason the component validates.
        await userEvent.upload(
            document.querySelector('#apply-resume') as HTMLInputElement,
            new File(['nope'], 'cv.png', {type: 'image/png'}),
            {applyAccept: false},
        );
        await userEvent.click(screen.getByRole('button', {name: t('offers.apply.submit')}));

        expect(await screen.findByText(t('offers.apply.cvType'))).toBeInTheDocument();
        expect(calls).toEqual([]);
        server.events.removeAllListeners();
    });
});

describe('staff', () => {
    it('get the lifecycle controls and no apply form', async () => {
        asUser(['HR_MANAGER']);

        renderOffer();

        expect(await screen.findByRole('heading', {name: t('offers.manage.heading')})).toBeInTheDocument();
        expect(applyForm()).not.toBeInTheDocument();
        expect(signInPrompt()).not.toBeInTheDocument();
    });

    // The panel mirrors the backend state machine: an open posting can be paused
    // or closed, but publishing it again is not a transition the server accepts.
    it('are offered only the transitions the posting allows', async () => {
        asUser(['HR_MANAGER'], {}, OFFER);

        renderOffer();
        await screen.findByRole('heading', {name: t('offers.manage.heading')});

        expect(screen.getByRole('button', {name: t('offers.manage.pause')})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: t('offers.manage.close')})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: t('offers.manage.publish')})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: t('offers.manage.archive')})).not.toBeInTheDocument();
    });

    it('can reach a draft that visitors cannot', async () => {
        const draft = anOffer({title: 'Unannounced Role', status: 'DRAFT'});
        asUser(['HR_MANAGER'], {}, draft);

        renderOffer(draft);

        expect(await screen.findByRole('heading', {name: 'Unannounced Role'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: t('offers.manage.publish')})).toBeInTheDocument();
    });
});

describe('an interviewer', () => {
    // Neither candidate nor staff: they read the posting and act on none of it.
    it('reads the offer but is offered no actions at all', async () => {
        asUser(['INTERVIEWER']);

        renderOffer();

        expect(await screen.findByRole('heading', {name: 'Senior Backend Engineer'})).toBeInTheDocument();
        expect(applyForm()).not.toBeInTheDocument();
        expect(managePanel()).not.toBeInTheDocument();
        expect(signInPrompt()).not.toBeInTheDocument();
    });
});

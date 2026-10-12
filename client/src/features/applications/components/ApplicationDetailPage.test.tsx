import {describe, expect, it} from 'vitest';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import ApplicationDetailPage from './ApplicationDetailPage.tsx';
import i18n from '@/utils/i18n';
import {aUser, apiPath, server, signedInSession} from '@/test/msw/server';
import {anApplicationResponse} from '@/test/fixtures';
import {renderWithProviders} from '@/test/renderWithProviders';

const t = i18n.getFixedT('en');
const path = '/applications/application-1';
const application = (overrides: Record<string, unknown> = {}) =>
    anApplicationResponse({status: 'PENDING', ...overrides});
const renderDetail = () =>
    renderWithProviders(<ApplicationDetailPage/>, {path: '/applications/:id', at: path});
const serve = (user: ReturnType<typeof aUser>, overrides: Record<string, unknown> = {}) => {
    server.use(
        ...signedInSession(user),
        http.get(apiPath(path), () => HttpResponse.json(application(overrides))),
    );
};

describe('application detail actions', () => {
    it('lets HR record a decision and immediately shows the returned status', async () => {
        const payloads: unknown[] = [];
        serve(aUser(['HR_MANAGER']));
        server.use(http.patch(apiPath(path), async ({request}) => {
            payloads.push(await request.json());
            return HttpResponse.json(application({status: 'ACCEPTED'}));
        }));

        renderDetail();
        await userEvent.selectOptions(await screen.findByLabelText(t('applications.actions.decision')), 'ACCEPTED');
        await userEvent.click(screen.getByRole('button', {name: t('applications.actions.saveDecision')}));

        await waitFor(() => expect(document.querySelector('.status-badge')).toHaveTextContent(t('applications.status.ACCEPTED')));
        expect(payloads).toEqual([{status: 'ACCEPTED'}]);
        expect(screen.queryByRole('button', {name: t('applications.actions.deleteApplication')})).not.toBeInTheDocument();
    });

    it('lets the owner replace the résumé with their profile CV and shows its filename', async () => {
        const owner = aUser(['CANDIDATE'], {resumeFilename: 'profile.pdf'});
        serve(owner, {candidate: {...application().candidate, id: owner.id}});
        let replacements = 0;
        server.use(http.put(apiPath(`${path}/resume/from-base`), () => {
            replacements += 1;
            return HttpResponse.json(application({
                candidate: {...application().candidate, id: owner.id},
                resume: {originalFilename: 'profile.pdf', contentType: 'application/pdf', sizeBytes: 8},
            }));
        }));

        renderDetail();
        await userEvent.click(await screen.findByRole('button', {name: t('applications.actions.useBaseResume')}));

        expect(await screen.findByText('profile.pdf')).toBeInTheDocument();
        expect(replacements).toBe(1);
        expect(screen.queryByLabelText(t('applications.actions.decision'))).not.toBeInTheDocument();
    });

    it('asks the owner to confirm deletion and sends it only after confirmation', async () => {
        const owner = aUser(['CANDIDATE']);
        serve(owner, {candidate: {...application().candidate, id: owner.id}});
        let deletions = 0;
        server.use(http.delete(apiPath(path), () => {
            deletions += 1;
            return new HttpResponse(null, {status: 204});
        }));

        renderDetail();
        await userEvent.click(await screen.findByRole('button', {name: t('applications.actions.deleteApplication')}));
        expect(deletions).toBe(0);
        await userEvent.click(screen.getByRole('button', {name: t('applications.actions.confirmDelete')}));
        await waitFor(() => expect(deletions).toBe(1));
    });

    it('keeps the application visible and explains a rejected deletion', async () => {
        const owner = aUser(['CANDIDATE']);
        serve(owner, {candidate: {...application().candidate, id: owner.id}});
        server.use(http.delete(apiPath(path), () =>
            HttpResponse.json({errorCode: 'DATA_CONFLICT'}, {status: 409})));

        renderDetail();
        await userEvent.click(await screen.findByRole('button', {name: t('applications.actions.deleteApplication')}));
        await userEvent.click(screen.getByRole('button', {name: t('applications.actions.confirmDelete')}));

        expect(await screen.findByRole('alert')).toHaveTextContent(t('codes.DATA_CONFLICT', {ns: 'errors'}));
        expect(screen.getByRole('heading', {name: 'Backend Engineer'})).toBeInTheDocument();
    });

    it('shows no management actions to a different candidate', async () => {
        serve(aUser(['CANDIDATE']));
        renderDetail();

        await screen.findByRole('heading', {name: 'Backend Engineer'});
        expect(screen.queryByRole('heading', {name: t('applications.actions.title')})).not.toBeInTheDocument();
    });
});

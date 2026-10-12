import {describe, expect, it} from 'vitest';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import InterviewsPage from '@/features/interviews/components/InterviewsPage.tsx';
import {apiPath, aUser, server, signedInSession} from '@/test/msw/server';
import {renderApp, renderWithProviders} from '@/test/renderWithProviders';
import i18n from '@/utils/i18n';

const t = i18n.getFixedT('en');
const interviewer = aUser(['INTERVIEWER']);
const slot = {
    id: 'slot-1', interviewer: {
        id: interviewer.id, firstName: 'Test', lastName: 'User', email: interviewer.email,
    }, startAt: '2099-01-01T09:00:00Z', endAt: '2099-01-01T09:45:00Z',
    durationInMinutes: 45, available: true,
};
const interview = {
    id: 'interview-1', applicationId: 'application-1',
    interviewer: slot.interviewer,
    candidate: {id: 'candidate-1', firstName: 'Jane', lastName: 'Doe', email: 'jane@example.test'},
    job: {id: 'job-1', title: 'Engineer'},
    slot, status: 'SCHEDULED', recommendation: null, notes: null,
    isOnline: false, meetingUrl: null,
};
const page = (items: unknown[]) => ({
    content: items,
    page: {number: 0, size: 20, totalElements: items.length, totalPages: 1},
});

describe('interviewer workflow', () => {
    it('loads own availability and creates a slot without a staff directory lookup', async () => {
        let payload: Record<string, unknown> | undefined;
        server.use(...signedInSession(interviewer),
            http.get(apiPath('/slots/me'), () => HttpResponse.json(page([slot]))),
            http.post(apiPath('/slots'), async ({request}) => {
                payload = await request.json() as Record<string, unknown>;
                return HttpResponse.json({...slot, id: 'slot-2'}, {status: 201});
            }));
        renderApp('/slots');
        const events = userEvent.setup();

        expect(await screen.findByRole('heading', {name: t('slots.myTitle')})).toBeInTheDocument();
        expect(screen.getByText('Test User')).toBeInTheDocument();
        await events.click(screen.getByRole('button', {name: `+ ${t('slots.create.newSlot')}`}));
        expect(screen.queryByRole('combobox', {name: /interviewer/i})).not.toBeInTheDocument();
        await events.type(screen.getByLabelText(`${t('slots.create.date')} *`), '2099-02-01');
        await events.type(screen.getByLabelText(`${t('slots.create.startTime')} *`), '09:00');
        await events.type(screen.getByLabelText(`${t('slots.create.endTime')} *`), '09:45');
        await events.click(screen.getByRole('button', {name: t('slots.create.submit')}));

        await waitFor(() => expect(payload).toBeDefined());
        expect(payload).not.toHaveProperty('interviewerId');
        expect(payload).toHaveProperty('startAt');
        expect(payload).toHaveProperty('endAt');
    });

    it('records a recommendation and notes through PATCH without scheduling fields', async () => {
        let payload: Record<string, unknown> | undefined;
        server.use(...signedInSession(interviewer),
            http.get(apiPath('/interviews/me'), () => HttpResponse.json(page([interview]))),
            http.patch(apiPath('/interviews/interview-1'), async ({request}) => {
                payload = await request.json() as Record<string, unknown>;
                return HttpResponse.json({...interview, ...payload});
            }));
        renderWithProviders(<InterviewsPage/>);
        const events = userEvent.setup();

        await screen.findByText('Jane Doe');
        await events.click(screen.getByRole('button', {name: t('interviews.outcome.action')}));
        await events.selectOptions(screen.getByRole('combobox', {name: t('interviews.columns.status')}), 'COMPLETED');
        await events.selectOptions(screen.getByRole('combobox', {name: `${t('interviews.form.recommendation')} *`}), 'YES');
        await events.type(screen.getByRole('textbox', {name: t('interviews.form.notes')}), 'Strong problem solving');
        await events.click(screen.getByRole('button', {name: t('interviews.outcome.submit')}));

        await waitFor(() => expect(payload).toEqual({
            status: 'COMPLETED', recommendation: 'YES', notes: 'Strong problem solving',
        }));
        expect(screen.queryByRole('button', {name: t('interviews.form.schedule')})).not.toBeInTheDocument();
    });

    it('keeps candidate interviews read only', async () => {
        server.use(...signedInSession(aUser(['CANDIDATE'])),
            http.get(apiPath('/interviews/me'), () => HttpResponse.json(page([interview]))));
        renderWithProviders(<InterviewsPage/>);

        await screen.findByText('Jane Doe');
        expect(screen.queryByRole('button', {name: t('interviews.outcome.action')})).not.toBeInTheDocument();
        expect(screen.queryByRole('columnheader', {name: t('common.actions')})).not.toBeInTheDocument();
    });
});

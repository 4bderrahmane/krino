import {describe, expect, it, vi} from 'vitest';
import {fireEvent, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import ApplicationsPage from '@/features/applications/components/ApplicationsPage.tsx';
import InterviewsPage from './InterviewsPage.tsx';
import InterviewForm from './InterviewForm.tsx';
import {apiPath, aUser, server, signedInSession} from '@/test/msw/server';
import {renderWithProviders} from '@/test/renderWithProviders';
import i18n from '@/utils/i18n';

const t = i18n.getFixedT('en');
const recordPage = <T,>(records: T[], page: number, size = 20) => ({
    content: records.slice(page * size, (page + 1) * size),
    page: {number: page, size, totalElements: records.length, totalPages: Math.ceil(records.length / size)},
});
const application = (number: number) => ({
    id: `application-${number}`, jobId: `offer-${number}`, jobTitle: `Offer ${number}`,
    candidate: {id: `candidate-${number}`, firstName: 'Candidate', lastName: `${number}`,
        email: `candidate${number}@example.test`},
    status: 'PENDING', resume: null, appliedAt: '2026-08-06T10:00:00.000Z',
});
const slot = (number: number) => ({
    id: `slot-${number}`,
    interviewer: {id: `interviewer-${number}`, firstName: 'Interviewer', lastName: `${number}`,
        email: `interviewer${number}@example.test`},
    startAt: '2099-01-01T09:00:00Z', endAt: '2099-01-01T09:45:00Z', available: true,
});
const interview = (number: number) => ({
    id: `interview-${number}`, applicationId: `application-${number}`,
    candidate: application(number).candidate,
    interviewer: slot(number).interviewer,
    job: {id: `offer-${number}`, title: `Offer ${number}`},
    slot: slot(number), status: 'SCHEDULED', isOnline: false,
});
const allApplications = Array.from({length: 105}, (_, index) => application(index + 1));
const allSlots = Array.from({length: 105}, (_, index) => slot(index + 1));

const nextIn = (label: string) =>
    within(screen.getByRole('navigation', {name: label})).getByRole('button', {name: t('common.next')});

describe('pagination across application and interview workflows', () => {
    it('reaches applications after the first 20', async () => {
        const requestedPages: number[] = [];
        server.use(...signedInSession(aUser(['HR_MANAGER'])),
            http.get(apiPath('/applications'), ({request}) => {
                const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
                requestedPages.push(page);
                return HttpResponse.json(recordPage(allApplications, page));
            }));
        renderWithProviders(<ApplicationsPage/>);
        await screen.findByText('Offer 1');

        await userEvent.click(nextIn(t('common.pagination')));
        expect(await screen.findByText('Offer 21')).toBeInTheDocument();
        expect(requestedPages).toContain(1);
    });

    it('reaches interviews after the first 20', async () => {
        const records = Array.from({length: 25}, (_, index) => interview(index + 1));
        const requestedPages: number[] = [];
        server.use(...signedInSession(aUser(['HR_MANAGER'])),
            http.get(apiPath('/interviews'), ({request}) => {
                const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
                requestedPages.push(page);
                return HttpResponse.json(recordPage(records, page));
            }));
        renderWithProviders(<InterviewsPage/>);
        await screen.findByText('Candidate 1');

        await userEvent.click(nextIn(t('common.pagination')));
        expect(await screen.findByText('Candidate 21')).toBeInTheDocument();
        expect(requestedPages).toContain(1);
    });

    it('searches and selects application and slot records beyond the first 100', async () => {
        const onClose = vi.fn();
        let payload: Record<string, unknown> | undefined;
        const applicationRequests: URL[] = [];
        const slotRequests: URL[] = [];
        server.use(...signedInSession(aUser(['HR_MANAGER'])),
            http.get(apiPath('/applications/scheduling-options'), ({request}) => {
                const url = new URL(request.url);
                applicationRequests.push(url);
                const search = (url.searchParams.get('search') ?? '').toLowerCase();
                const matches = allApplications.filter((item) =>
                    `Candidate ${item.candidate.lastName} ${item.jobTitle}`.toLowerCase().includes(search));
                return HttpResponse.json(recordPage(matches, Number(url.searchParams.get('page') ?? 0)));
            }),
            http.get(apiPath('/slots/scheduling-options'), ({request}) => {
                const url = new URL(request.url);
                slotRequests.push(url);
                const search = (url.searchParams.get('search') ?? '').toLowerCase();
                const matches = allSlots.filter((item) =>
                    `Interviewer ${item.interviewer.lastName}`.toLowerCase().includes(search));
                return HttpResponse.json(recordPage(matches, Number(url.searchParams.get('page') ?? 0)));
            }),
            http.post(apiPath('/interviews'), async ({request}) => {
                payload = await request.json() as Record<string, unknown>;
                return HttpResponse.json({id: 'new-interview', ...payload}, {status: 201});
            }));
        renderWithProviders(<InterviewForm onClose={onClose}/>);
        const events = userEvent.setup();

        await screen.findByRole('option', {name: 'Candidate 1 — Offer 1'});
        for (let page = 1; page <= 5; page++) {
            await events.click(nextIn(t('interviews.form.applicationPages')));
            await screen.findByRole('option', {name: `Candidate ${page * 20 + 1} — Offer ${page * 20 + 1}`});
        }
        await events.selectOptions(screen.getByRole('combobox', {name: `${t('interviews.form.application')} *`}),
            'application-105');
        fireEvent.change(screen.getByRole('searchbox', {name: t('interviews.form.searchApplications')}),
            {target: {value: 'Candidate 105'}});
        await waitFor(() => expect(applicationRequests.some((url) => url.searchParams.get('search') === 'Candidate 105'))
            .toBe(true));
        expect(screen.getByRole('option', {name: 'Candidate 105 — Offer 105'})).toBeInTheDocument();

        await screen.findByRole('option', {name: /^Interviewer 1 ·/});
        for (let page = 1; page <= 5; page++) {
            await events.click(nextIn(t('interviews.form.slotPages')));
            await screen.findByRole('option', {name: new RegExp(`^Interviewer ${page * 20 + 1} ·`)});
        }
        await events.selectOptions(screen.getByRole('combobox', {name: `${t('interviews.form.slot')} *`}), 'slot-105');
        fireEvent.change(screen.getByRole('searchbox', {name: t('interviews.form.searchSlots')}),
            {target: {value: 'Interviewer 105'}});
        await waitFor(() => expect(slotRequests.some((url) => url.searchParams.get('search') === 'Interviewer 105'))
            .toBe(true));
        expect(screen.getByRole('option', {name: /^Interviewer 105 ·/})).toBeInTheDocument();
        await events.click(screen.getByRole('button', {name: t('interviews.form.createSubmit')}));

        await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
        expect(payload).toMatchObject({applicationId: 'application-105', slotId: 'slot-105'});
        expect(applicationRequests.some((url) => url.searchParams.get('page') === '5')).toBe(true);
        expect(slotRequests.some((url) => url.searchParams.get('page') === '5')).toBe(true);
    }, 15000);
});

import {describe, expect, it} from 'vitest';
import {screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import ApplicationsPage from '@/features/applications/components/ApplicationsPage.tsx';
import InterviewsPage from './InterviewsPage.tsx';
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

});

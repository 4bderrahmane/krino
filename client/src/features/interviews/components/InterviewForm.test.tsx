import {describe, expect, it, vi} from 'vitest';
import {screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {http, HttpResponse} from 'msw';
import InterviewForm from '@/features/interviews/components/InterviewForm';
import type {Interview} from '@/features/interviews/types/interview.types';
import {apiPath, aUser, server, signedInSession} from '@/test/msw/server';
import {renderWithProviders} from '@/test/renderWithProviders';
import i18n from '@/utils/i18n';

const t = i18n.getFixedT('en');
const cancelled: Interview = {
    id: 'interview-1', applicationId: 'application-1', slotId: null,
    candidate: {id: 'candidate-1', fullName: 'Candidate', email: 'candidate@example.test'},
    interviewer: {id: 'interviewer-1', fullName: 'Interviewer', email: 'interviewer@example.test'},
    offer: null, schedule: null, status: 'CANCELLED', recommendation: null,
    isOnline: false, meetingUrl: null, notes: 'Cancelled at candidate request',
};
const emptySlots = () => http.get(apiPath('/slots/scheduling-options'), () => HttpResponse.json({
    content: [], page: {number: 0, size: 100, totalElements: 0, totalPages: 0},
}));

describe('cancelled interview editing', () => {
    it('saves a cancelled interview without requiring or booking a slot', async () => {
        let payload: Record<string, unknown> | undefined;
        const onClose = vi.fn();
        server.use(...signedInSession(aUser(['HR_MANAGER'])),
            http.put(apiPath('/interviews/interview-1'), async ({request}) => {
                payload = await request.json() as Record<string, unknown>;
                return HttpResponse.json({id: 'interview-1', applicationId: 'application-1',
                    slot: null, status: 'CANCELLED', isOnline: false});
            }));
        renderWithProviders(<InterviewForm interview={cancelled} onClose={onClose}/>);
        const events = userEvent.setup();
        expect(screen.getByRole('combobox', {name: t('interviews.form.slot')})).toBeDisabled();
        await events.click(screen.getByRole('button', {name: t('interviews.form.editSubmit')}));
        await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
        expect(payload).toMatchObject({applicationId: 'application-1', slotId: null, status: 'CANCELLED'});
    });

    it('requires a slot when reopening a cancelled interview', async () => {
        const onClose = vi.fn();
        server.use(...signedInSession(aUser(['HR_MANAGER'])), emptySlots());
        renderWithProviders(<InterviewForm interview={cancelled} onClose={onClose}/>);
        const events = userEvent.setup();
        await events.selectOptions(screen.getByRole('combobox', {name: t('interviews.columns.status')}), 'SCHEDULED');
        await events.click(screen.getByRole('button', {name: t('interviews.form.editSubmit')}));
        expect(await screen.findByText(t('interviews.form.errors.slotRequired'))).toBeInTheDocument();
        expect(onClose).not.toHaveBeenCalled();
    });

    it('cancels an existing booking with a null slot in the request', async () => {
        let payload: Record<string, unknown> | undefined;
        const onClose = vi.fn();
        server.use(...signedInSession(aUser(['HR_MANAGER'])), emptySlots(),
            http.put(apiPath('/interviews/interview-1'), async ({request}) => {
                payload = await request.json() as Record<string, unknown>;
                return HttpResponse.json({id: 'interview-1', applicationId: 'application-1',
                    slot: null, status: 'CANCELLED', isOnline: false});
            }));
        renderWithProviders(<InterviewForm interview={{...cancelled, status: 'SCHEDULED', slotId: 'slot-1'}} onClose={onClose}/>);
        const events = userEvent.setup();
        await events.selectOptions(screen.getByRole('combobox', {name: t('interviews.columns.status')}), 'CANCELLED');
        await events.click(screen.getByRole('button', {name: t('interviews.form.editSubmit')}));
        await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
        expect(payload).toMatchObject({slotId: null, status: 'CANCELLED'});
    });
});

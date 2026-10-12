import React, {useEffect, useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {useQuery} from '@tanstack/react-query';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {useAuth} from '@/shared/hooks/useAuth';
import {resolveServerError} from '@/shared/services/errors';
import {useCreateInterview, useUpdateInterview} from '@/features/interviews/hooks/useInterviews.ts';
import {getSchedulingApplications} from '@/features/applications/services/ApplicationService.ts';
import type {Application} from '@/features/applications/types/application.types.ts';
import {getSchedulingSlots} from '@/features/slots/services/SlotService.ts';
import type {Slot} from '@/features/slots/types/slot.types.ts';
import PaginationControls from '@/shared/components/PaginationControls.tsx';
import type {
    Interview,
    InterviewRecommendation,
    InterviewStatus,
} from '@/features/interviews/types/interview.types.ts';

interface InterviewFormProps {
    interview?: Interview;
    onClose: () => void;
}

const STATUS_ORDER: InterviewStatus[] = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
const RECOMMENDATION_ORDER: InterviewRecommendation[] = ['STRONG_YES', 'YES', 'NO', 'STRONG_NO'];

const slotLabel = (interviewer: string, date: string | null, start: string | null, end: string | null): string => {
    const when = [date, [start, end].filter(Boolean).map((v) => v!.slice(0, 5)).join('–')]
        .filter(Boolean)
        .join(' ');
    return `${interviewer} · ${when}`;
};

const InterviewForm: React.FC<InterviewFormProps> = ({interview, onClose}) => {
    const {t} = useTranslation();
    const {showSuccessToast} = useSuccessToast();
    const {user} = useAuth();
    const isEdit = Boolean(interview);

    const createInterview = useCreateInterview();
    const updateInterview = useUpdateInterview();

    const [applicationId, setApplicationId] = useState(interview?.applicationId ?? '');
    const [slotId, setSlotId] = useState(interview?.slotId ?? '');
    const [status, setStatus] = useState<InterviewStatus>(interview?.status ?? 'SCHEDULED');
    const [applicationSearch, setApplicationSearch] = useState('');
    const [applicationPage, setApplicationPage] = useState(0);
    const [slotSearch, setSlotSearch] = useState('');
    const [slotPage, setSlotPage] = useState(0);
    const [selectedApplication, setSelectedApplication] = useState<Application | null>(null);
    const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null);

    // Selection queries use server-side search. Their keys include search and page,
    // so a later record can be reached without loading the entire directory.
    const {data: appsData, isLoading: appsLoading, isError: appsError, refetch: refetchApps} = useQuery({
        queryKey: ['applications', 'picker', user?.id, applicationSearch, applicationPage],
        queryFn: ({signal}) => getSchedulingApplications(applicationPage, applicationSearch, signal),
        enabled: !isEdit && !!user,
    });
    const {data: slotsData, isLoading: slotsLoading, isError: slotsError, refetch: refetchSlots} = useQuery({
        queryKey: ['slots', 'picker', user?.id, slotSearch, slotPage],
        queryFn: ({signal}) => getSchedulingSlots(slotPage, slotSearch, signal),
        enabled: status !== 'CANCELLED' && !!user,
    });

    const applicationPages = appsData?.page.totalPages;
    const slotPages = slotsData?.page.totalPages;
    useEffect(() => {
        if (applicationPages !== undefined && applicationPage >= applicationPages) {
            setApplicationPage(Math.max(0, applicationPages - 1));
        }
    }, [applicationPage, applicationPages]);
    useEffect(() => {
        if (slotPages !== undefined && slotPage >= slotPages) {
            setSlotPage(Math.max(0, slotPages - 1));
        }
    }, [slotPage, slotPages]);

    const [recommendation, setRecommendation] = useState<InterviewRecommendation | ''>(
        interview?.recommendation ?? '',
    );
    const [isOnline, setIsOnline] = useState(interview?.isOnline ?? false);
    const [meetingUrl, setMeetingUrl] = useState(interview?.meetingUrl ?? '');
    const [notes, setNotes] = useState(interview?.notes ?? '');
    const [fieldError, setFieldError] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const pending = createInterview.isPending || updateInterview.isPending;

    const applicationOptions = [...(appsData?.applications ?? [])];
    if (selectedApplication && !applicationOptions.some((a) => a.id === selectedApplication.id)) {
        applicationOptions.unshift(selectedApplication);
    }
    const slotOptions = (slotsData?.slots ?? []).filter((slot) => slot.available);
    if (selectedSlot && !slotOptions.some((slot) => slot.id === selectedSlot.id)) {
        slotOptions.unshift(selectedSlot);
    }
    // A booked slot is absent from the available-slot search during editing.
    if (isEdit && interview?.slotId && !slotOptions.some((slot) => slot.id === interview.slotId)) {
        slotOptions.unshift({
            id: interview.slotId,
            interviewer: {id: '', fullName: interview.interviewer.fullName, email: ''},
            date: interview.schedule?.date ?? null,
            startTime: interview.schedule?.startTime ?? null,
            endTime: interview.schedule?.endTime ?? null,
            durationInMinutes: null,
            available: false,
        });
    }

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);
        setFieldError(null);

        if (!isEdit && !applicationId) {
            setFieldError(t('interviews.form.errors.applicationRequired'));
            return;
        }
        if (status !== 'CANCELLED' && !slotId) {
            setFieldError(t('interviews.form.errors.slotRequired'));
            return;
        }
        if (status === 'COMPLETED' && !recommendation) {
            setFieldError(t('interviews.form.errors.recommendationRequired'));
            return;
        }

        const values = {
            applicationId,
            slotId,
            status,
            recommendation: status === 'COMPLETED' ? (recommendation as InterviewRecommendation) : null,
            notes: notes.trim() || null,
            isOnline,
            meetingUrl: meetingUrl.trim() || null,
        };

        try {
            if (isEdit && interview) {
                await updateInterview.mutateAsync({id: interview.id, values});
                showSuccessToast(t('interviews.form.editSuccess'));
            } else {
                await createInterview.mutateAsync(values);
                showSuccessToast(t('interviews.form.createSuccess'));
            }
            onClose();
        } catch (err: unknown) {
            console.error('interview save failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    return (
        <form className="interview-form" onSubmit={handleSubmit} noValidate>
            <h2 className="interview-form-title">
                {isEdit ? t('interviews.form.editTitle') : t('interviews.form.createTitle')}
            </h2>

            {isEdit ? (
                // The application can't change once an interview exists; show it read-only.
                <div className="interview-field">
                    <span className="interview-field-label">{t('interviews.columns.candidate')}</span>
                    <div className="interview-field-static">
                        {interview?.candidate.fullName}
                        {interview?.offer?.title ? ` — ${interview.offer.title}` : ''}
                    </div>
                </div>
            ) : (
                <div className="interview-picker">
                    <label className="interview-field">
                        <span className="interview-field-label">{t('interviews.form.searchApplications')}</span>
                        <input className="interview-input" type="search" value={applicationSearch}
                               onChange={(e) => {setApplicationSearch(e.target.value); setApplicationPage(0);}}/>
                    </label>
                    <label className="interview-field">
                        <span className="interview-field-label">{t('interviews.form.application')} *</span>
                        <select className="interview-input" value={applicationId} disabled={appsLoading || appsError}
                                onChange={(e) => {
                                    setApplicationId(e.target.value);
                                    setSelectedApplication(applicationOptions.find((a) => a.id === e.target.value) ?? null);
                                }}>
                            <option value="" disabled>
                                {appsLoading ? t('app.loading') : t('interviews.form.applicationPlaceholder')}
                            </option>
                            {applicationOptions.map((a) => (
                                <option key={a.id} value={a.id}>
                                    {a.candidate.fullName}{a.offerTitle ? ` — ${a.offerTitle}` : ''}
                                </option>
                            ))}
                        </select>
                    </label>
                    {appsError && <div className="interview-picker-error" role="alert">
                        {t('interviews.form.optionsLoadError')}
                        <button type="button" onClick={() => void refetchApps()}>{t('common.tryAgain')}</button>
                    </div>}
                    {!appsLoading && !appsError && applicationOptions.length === 0 &&
                        <span className="interview-field-hint">{t('interviews.form.noApplications')}</span>}
                    <PaginationControls label={t('interviews.form.applicationPages')} page={applicationPage}
                                        totalPages={appsData?.page.totalPages ?? 0} onPageChange={setApplicationPage}/>
                </div>
            )}

            <div className="interview-picker">
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.searchSlots')}</span>
                    <input className="interview-input" type="search" value={slotSearch}
                           disabled={status === 'CANCELLED'}
                           onChange={(e) => {setSlotSearch(e.target.value); setSlotPage(0);}}/>
                </label>
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.slot')}{status !== 'CANCELLED' && ' *'}</span>
                    <select className="interview-input" value={slotId}
                            disabled={slotsLoading || slotsError || status === 'CANCELLED'}
                            onChange={(e) => {
                                setSlotId(e.target.value);
                                setSelectedSlot(slotOptions.find((slot) => slot.id === e.target.value) ?? null);
                            }}>
                        <option value="" disabled>
                            {slotsLoading ? t('app.loading') : t('interviews.form.slotPlaceholder')}
                        </option>
                        {slotOptions.map((slot) => (
                            <option key={slot.id} value={slot.id}>
                                {slotLabel(slot.interviewer.fullName, slot.date, slot.startTime, slot.endTime)}
                            </option>
                        ))}
                    </select>
                </label>
                {slotsError && status !== 'CANCELLED' && <div className="interview-picker-error" role="alert">
                    {t('interviews.form.optionsLoadError')}
                    <button type="button" onClick={() => void refetchSlots()}>{t('common.tryAgain')}</button>
                </div>}
                {status !== 'CANCELLED' && !slotsLoading && !slotsError && slotOptions.length === 0 &&
                    <span className="interview-field-hint">{t('interviews.form.noSlots')}</span>}
                {status !== 'CANCELLED' &&
                    <PaginationControls label={t('interviews.form.slotPages')} page={slotPage}
                                        totalPages={slotsData?.page.totalPages ?? 0} onPageChange={setSlotPage}/>}
            </div>

            {isEdit && (
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.columns.status')}</span>
                    <select
                        className="interview-input"
                        value={status}
                        onChange={(e) => setStatus(e.target.value as InterviewStatus)}
                    >
                        {STATUS_ORDER.map((s) => (
                            <option key={s} value={s}>{t(`interviews.status.${s}`)}</option>
                        ))}
                    </select>
                </label>
            )}

            {status === 'COMPLETED' && (
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.recommendation')} *</span>
                    <select
                        className="interview-input"
                        value={recommendation}
                        onChange={(e) => setRecommendation(e.target.value as InterviewRecommendation | '')}
                    >
                        <option value="" disabled>{t('interviews.form.recommendationPlaceholder')}</option>
                        {RECOMMENDATION_ORDER.map((r) => (
                            <option key={r} value={r}>{t(`interviews.recommendation.${r}`)}</option>
                        ))}
                    </select>
                </label>
            )}

            <label className="interview-check">
                <input type="checkbox" checked={isOnline} onChange={(e) => setIsOnline(e.target.checked)}/>
                <span>{t('interviews.form.isOnline')}</span>
            </label>

            {isOnline && (
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.meetingUrl')}</span>
                    <input
                        type="url"
                        className="interview-input"
                        value={meetingUrl}
                        maxLength={512}
                        onChange={(e) => setMeetingUrl(e.target.value)}
                        placeholder="https://…"
                    />
                </label>
            )}

            <label className="interview-field">
                <span className="interview-field-label">{t('interviews.form.notes')}</span>
                <textarea
                    className="interview-input interview-textarea"
                    value={notes}
                    rows={3}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder={t('interviews.form.notesPlaceholder')}
                />
            </label>

            {fieldError && <span className="interview-field-error">{fieldError}</span>}
            {errorMessage && <div className="interview-form-error">{errorMessage}</div>}

            <div className="interview-form-actions">
                <button type="button" className="interview-form-cancel" onClick={onClose}>
                    {t('common.cancel')}
                </button>
                <button type="submit" className="interview-form-submit" disabled={pending}>
                    {pending
                        ? t('app.loading')
                        : isEdit
                            ? t('interviews.form.editSubmit')
                            : t('interviews.form.createSubmit')}
                </button>
            </div>
        </form>
    );
};

export default InterviewForm;

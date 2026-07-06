import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {useQuery} from '@tanstack/react-query';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useCreateInterview, useUpdateInterview} from '@/features/interviews/hooks/useInterviews.ts';
import {getApplications} from '@/features/applications/services/ApplicationService.ts';
import {getSlots} from '@/features/slots/services/SlotService.ts';
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
    const isEdit = Boolean(interview);

    const createInterview = useCreateInterview();
    const updateInterview = useUpdateInterview();

    // Applications are only needed when scheduling (the application is immutable on edit).
    const {data: appsData, isLoading: appsLoading} = useQuery({
        queryKey: ['applications', 'picker'],
        queryFn: () => getApplications(0, 100),
        enabled: !isEdit,
    });
    const {data: slotsData, isLoading: slotsLoading} = useQuery({
        queryKey: ['slots', 'picker'],
        queryFn: () => getSlots(0, 100),
    });

    const [applicationId, setApplicationId] = useState(interview?.applicationId ?? '');
    const [slotId, setSlotId] = useState(interview?.slotId ?? '');
    const [status, setStatus] = useState<InterviewStatus>(interview?.status ?? 'SCHEDULED');
    const [recommendation, setRecommendation] = useState<InterviewRecommendation | ''>(
        interview?.recommendation ?? '',
    );
    const [isOnline, setIsOnline] = useState(interview?.isOnline ?? false);
    const [meetingUrl, setMeetingUrl] = useState(interview?.meetingUrl ?? '');
    const [notes, setNotes] = useState(interview?.notes ?? '');
    const [fieldError, setFieldError] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const pending = createInterview.isPending || updateInterview.isPending;

    const availableSlots = (slotsData?.slots ?? []).filter((s) => s.available);
    // In edit mode the booked slot is no longer "available", so surface it explicitly
    // alongside the free ones so the current value renders and can be kept.
    const slotOptions = [...availableSlots];
    if (isEdit && interview?.slotId && !slotOptions.some((s) => s.id === interview.slotId)) {
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
        if (!slotId) {
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
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.application')} *</span>
                    <select
                        className="interview-input"
                        value={applicationId}
                        disabled={appsLoading}
                        onChange={(e) => setApplicationId(e.target.value)}
                    >
                        <option value="" disabled>
                            {appsLoading ? t('app.loading') : t('interviews.form.applicationPlaceholder')}
                        </option>
                        {(appsData?.applications ?? []).map((a) => (
                            <option key={a.id} value={a.id}>
                                {a.candidate.fullName}{a.offerTitle ? ` — ${a.offerTitle}` : ''}
                            </option>
                        ))}
                    </select>
                </label>
            )}

            <label className="interview-field">
                <span className="interview-field-label">{t('interviews.form.slot')} *</span>
                <select
                    className="interview-input"
                    value={slotId}
                    disabled={slotsLoading}
                    onChange={(e) => setSlotId(e.target.value)}
                >
                    <option value="" disabled>
                        {slotsLoading ? t('app.loading') : t('interviews.form.slotPlaceholder')}
                    </option>
                    {slotOptions.map((s) => (
                        <option key={s.id} value={s.id}>
                            {slotLabel(s.interviewer.fullName, s.date, s.startTime, s.endTime)}
                        </option>
                    ))}
                </select>
                {!slotsLoading && slotOptions.length === 0 && (
                    <span className="interview-field-hint">{t('interviews.form.noSlots')}</span>
                )}
            </label>

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

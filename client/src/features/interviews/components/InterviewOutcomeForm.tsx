import {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {useRecordInterviewOutcome} from '@/features/interviews/hooks/useInterviews.ts';
import type {
    Interview,
    InterviewRecommendation,
    InterviewStatus,
} from '@/features/interviews/types/interview.types.ts';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';

interface InterviewOutcomeFormProps {
    interview: Interview;
    onClose: () => void;
}

const RECOMMENDATIONS: InterviewRecommendation[] = ['STRONG_YES', 'YES', 'NO', 'STRONG_NO'];

const InterviewOutcomeForm = ({interview, onClose}: InterviewOutcomeFormProps) => {
    const {t} = useTranslation();
    const {showSuccessToast} = useSuccessToast();
    const saveOutcome = useRecordInterviewOutcome();
    const [status, setStatus] = useState<InterviewStatus>(interview.status);
    const [recommendation, setRecommendation] = useState<InterviewRecommendation | ''>(
        interview.recommendation ?? '',
    );
    const [notes, setNotes] = useState(interview.notes ?? '');
    const [error, setError] = useState<string | null>(null);
    const statusOptions: InterviewStatus[] = interview.status === 'SCHEDULED'
        ? ['SCHEDULED', 'COMPLETED', 'NO_SHOW']
        : [interview.status];

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError(null);
        if (status === 'COMPLETED' && !recommendation) {
            setError(t('interviews.form.errors.recommendationRequired'));
            return;
        }

        try {
            await saveOutcome.mutateAsync({
                id: interview.id,
                values: {
                    status,
                    recommendation: status === 'COMPLETED' ? recommendation as InterviewRecommendation : null,
                    notes: notes.trim(),
                },
            });
            showSuccessToast(t('interviews.outcome.success'));
            onClose();
        } catch (failure: unknown) {
            setError(resolveServerError(t, failure));
        }
    };

    return (
        <form className="interview-form" onSubmit={handleSubmit}>
            <h2 className="interview-form-title">{t('interviews.outcome.title')}</h2>
            <label className="interview-field">
                <span className="interview-field-label">{t('interviews.columns.status')}</span>
                <select className="interview-input" value={status}
                        onChange={(event) => setStatus(event.target.value as InterviewStatus)}>
                    {statusOptions.map((option) => (
                        <option key={option} value={option}>{t(`interviews.status.${option}`)}</option>
                    ))}
                </select>
            </label>
            {status === 'COMPLETED' && (
                <label className="interview-field">
                    <span className="interview-field-label">{t('interviews.form.recommendation')} *</span>
                    <select className="interview-input" value={recommendation}
                            onChange={(event) => setRecommendation(event.target.value as InterviewRecommendation | '')}>
                        <option value="" disabled>{t('interviews.form.recommendationPlaceholder')}</option>
                        {RECOMMENDATIONS.map((option) => (
                            <option key={option} value={option}>{t(`interviews.recommendation.${option}`)}</option>
                        ))}
                    </select>
                </label>
            )}
            <label className="interview-field">
                <span className="interview-field-label">{t('interviews.form.notes')}</span>
                <textarea className="interview-input interview-textarea" value={notes} rows={3}
                          onChange={(event) => setNotes(event.target.value)}
                          placeholder={t('interviews.form.notesPlaceholder')}/>
            </label>
            {error && <div className="interview-form-error">{error}</div>}
            <div className="interview-form-actions">
                <button type="button" className="interview-form-cancel" onClick={onClose}>
                    {t('common.cancel')}
                </button>
                <button type="submit" className="interview-form-submit" disabled={saveOutcome.isPending}>
                    {saveOutcome.isPending ? t('app.loading') : t('interviews.outcome.submit')}
                </button>
            </div>
        </form>
    );
};

export default InterviewOutcomeForm;

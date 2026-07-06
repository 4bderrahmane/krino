import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useCreateSlot, useInterviewers, useUpdateSlot} from '@/features/slots/hooks/useSlots.ts';
import type {Slot} from '@/features/slots/types/slot.types.ts';

interface SlotFormProps {
    // When provided the form edits that slot's window; otherwise it creates one.
    slot?: Slot;
    onClose: () => void;
}

// <input type="time"> works in "HH:mm"; trim the seconds the backend returns.
const toInputTime = (value: string | null): string => (value ? value.slice(0, 5) : '');

const SlotForm: React.FC<SlotFormProps> = ({slot, onClose}) => {
    const {t} = useTranslation();
    const {showSuccessToast} = useSuccessToast();
    const isEdit = Boolean(slot);

    const createSlot = useCreateSlot();
    const updateSlot = useUpdateSlot();
    // Only the create form needs the interviewer picker (the window-only update
    // DTO can't reassign an existing slot's owner).
    const {data: interviewers, isLoading: interviewersLoading} = useInterviewers();

    const [interviewerId, setInterviewerId] = useState(slot?.interviewer.id ?? '');
    const [date, setDate] = useState(slot?.date ?? '');
    const [startTime, setStartTime] = useState(toInputTime(slot?.startTime ?? null));
    const [endTime, setEndTime] = useState(toInputTime(slot?.endTime ?? null));
    const [fieldError, setFieldError] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const pending = createSlot.isPending || updateSlot.isPending;

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);
        setFieldError(null);

        if (!isEdit && !interviewerId) {
            setFieldError(t('slots.create.errors.interviewerRequired'));
            return;
        }
        if (!date || !startTime || !endTime) {
            setFieldError(t('slots.create.errors.windowRequired'));
            return;
        }
        if (endTime <= startTime) {
            setFieldError(t('slots.create.errors.endAfterStart'));
            return;
        }

        try {
            if (isEdit && slot) {
                await updateSlot.mutateAsync({id: slot.id, values: {date, startTime, endTime}});
                showSuccessToast(t('slots.edit.success'));
            } else {
                await createSlot.mutateAsync({interviewerId, date, startTime, endTime});
                showSuccessToast(t('slots.create.success'));
            }
            onClose();
        } catch (err: unknown) {
            console.error('slot save failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    return (
        <form className="slot-form" onSubmit={handleSubmit} noValidate>
            <h2 className="slot-form-title">
                {isEdit ? t('slots.edit.title') : t('slots.create.title')}
            </h2>

            {!isEdit && (
                <label className="slot-field">
                    <span className="slot-field-label">{t('slots.create.interviewer')} *</span>
                    <select
                        className={`slot-input${fieldError && !interviewerId ? ' has-error' : ''}`}
                        value={interviewerId}
                        disabled={interviewersLoading}
                        onChange={(e) => setInterviewerId(e.target.value)}
                    >
                        <option value="" disabled>
                            {interviewersLoading
                                ? t('app.loading')
                                : t('slots.create.interviewerPlaceholder')}
                        </option>
                        {interviewers?.map((person) => (
                            <option key={person.id} value={person.id}>
                                {person.fullName} ({person.email})
                            </option>
                        ))}
                    </select>
                </label>
            )}

            <label className="slot-field">
                <span className="slot-field-label">{t('slots.create.date')} *</span>
                <input
                    type="date"
                    className="slot-input"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                />
            </label>

            <div className="slot-field-row">
                <label className="slot-field">
                    <span className="slot-field-label">{t('slots.create.startTime')} *</span>
                    <input
                        type="time"
                        className="slot-input"
                        value={startTime}
                        onChange={(e) => setStartTime(e.target.value)}
                    />
                </label>
                <label className="slot-field">
                    <span className="slot-field-label">{t('slots.create.endTime')} *</span>
                    <input
                        type="time"
                        className="slot-input"
                        value={endTime}
                        onChange={(e) => setEndTime(e.target.value)}
                    />
                </label>
            </div>

            {fieldError && <span className="slot-field-error">{fieldError}</span>}
            {errorMessage && <div className="slot-form-error">{errorMessage}</div>}

            <div className="slot-form-actions">
                <button type="button" className="slot-form-cancel" onClick={onClose}>
                    {t('common.cancel')}
                </button>
                <button type="submit" className="slot-form-submit" disabled={pending}>
                    {pending
                        ? t('app.loading')
                        : isEdit
                            ? t('slots.edit.submit')
                            : t('slots.create.submit')}
                </button>
            </div>
        </form>
    );
};

export default SlotForm;

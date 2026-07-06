import React, {useState} from 'react';
import {Navigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {useDeleteSlot, useSlots} from '@/features/slots/hooks/useSlots.ts';
import SlotForm from '@/features/slots/components/SlotForm.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import type {Slot} from '@/features/slots/types/slot.types.ts';
import '@/features/slots/styles/Slots.css';

const formatDate = (locale: string, date: string | null): string => {
    if (!date) return '—';
    const parsed = new Date(`${date}T00:00:00`);
    return Number.isNaN(parsed.getTime())
        ? date
        : parsed.toLocaleDateString(locale, {weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'});
};

const formatTime = (value: string | null): string => (value ? value.slice(0, 5) : '—');

const SlotsPage: React.FC = () => {
    const {t, i18n} = useTranslation();
    const {isStaff} = usePermissions();
    const {showSuccessToast} = useSuccessToast();
    const [page, setPage] = useState(0);
    const [showCreate, setShowCreate] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [confirmingDeleteId, setConfirmingDeleteId] = useState<string | null>(null);

    const {data, isLoading, isError, refetch} = useSlots(page, isStaff);
    const deleteSlot = useDeleteSlot();

    // Listing slots is an ADMIN/HR-only endpoint; bounce anyone else who lands here.
    if (!isStaff) {
        return <Navigate to="/dashboard" replace/>;
    }

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="slots-container">
                <div className="slots-state slots-error">
                    <p>{t('slots.loadError')}</p>
                    <button className="slots-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
            </div>
        );
    }

    const slots = data?.slots ?? [];
    const meta = data?.page;
    const pageCount = Math.max(1, meta?.totalPages ?? 1);
    const total = meta?.totalElements ?? slots.length;

    const handleDelete = async (slot: Slot) => {
        try {
            await deleteSlot.mutateAsync(slot.id);
            showSuccessToast(t('slots.delete.success'));
            setConfirmingDeleteId(null);
        } catch (err: unknown) {
            console.error('delete slot failed:', err);
            showSuccessToast(resolveServerError(t, err));
        }
    };

    return (
        <div className="slots-container">
            <header className="slots-header">
                <h1 className="slots-title">{t('slots.title')}</h1>
                <p className="slots-subtitle">{t('slots.count', {count: total})}</p>
            </header>

            <div className="slots-toolbar">
                {showCreate ? (
                    <SlotForm onClose={() => setShowCreate(false)}/>
                ) : (
                    <button
                        type="button"
                        className="slots-create-toggle"
                        onClick={() => setShowCreate(true)}
                    >
                        + {t('slots.create.newSlot')}
                    </button>
                )}
            </div>

            {slots.length === 0 ? (
                <div className="slots-state slots-empty">
                    <p>{t('slots.empty')}</p>
                </div>
            ) : (
                <>
                    <ul className="slots-list">
                        {slots.map((slot) => (
                            <li key={slot.id} className="slot-row">
                                {editingId === slot.id ? (
                                    <SlotForm slot={slot} onClose={() => setEditingId(null)}/>
                                ) : (
                                    <>
                                        <div className="slot-row-main">
                                            <div className="slot-row-top">
                                                <h2 className="slot-row-interviewer">{slot.interviewer.fullName}</h2>
                                                <span
                                                    className={`slot-badge ${slot.available ? 'slot-badge-available' : 'slot-badge-booked'}`}
                                                >
                                                    {slot.available ? t('slots.available') : t('slots.booked')}
                                                </span>
                                            </div>
                                            <p className="slot-row-when">
                                                {formatDate(i18n.language, slot.date)}
                                                {' · '}
                                                {formatTime(slot.startTime)}–{formatTime(slot.endTime)}
                                                {slot.durationInMinutes != null && (
                                                    <span className="slot-row-duration">
                                                        {' '}({t('slots.minutes', {count: slot.durationInMinutes})})
                                                    </span>
                                                )}
                                            </p>
                                        </div>
                                        <div className="slot-row-actions">
                                            {confirmingDeleteId === slot.id ? (
                                                <>
                                                    <span className="slot-confirm-label">{t('slots.delete.confirm')}</span>
                                                    <button
                                                        type="button"
                                                        className="slot-action slot-action-danger"
                                                        disabled={deleteSlot.isPending}
                                                        onClick={() => handleDelete(slot)}
                                                    >
                                                        {t('common.yes')}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="slot-action"
                                                        onClick={() => setConfirmingDeleteId(null)}
                                                    >
                                                        {t('common.no')}
                                                    </button>
                                                </>
                                            ) : (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="slot-action"
                                                        onClick={() => setEditingId(slot.id)}
                                                    >
                                                        {t('common.edit')}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="slot-action slot-action-danger"
                                                        onClick={() => setConfirmingDeleteId(slot.id)}
                                                    >
                                                        {t('common.delete')}
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </>
                                )}
                            </li>
                        ))}
                    </ul>

                    {pageCount > 1 && (
                        <nav className="slots-pagination" aria-label={t('slots.pagination')}>
                            <button
                                className="slots-page-button"
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                disabled={page === 0}
                            >
                                {t('common.previous')}
                            </button>
                            <span className="slots-page-indicator">
                                {t('slots.pageIndicator', {current: page + 1, total: pageCount})}
                            </span>
                            <button
                                className="slots-page-button"
                                onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
                                disabled={page >= pageCount - 1}
                            >
                                {t('common.next')}
                            </button>
                        </nav>
                    )}
                </>
            )}
        </div>
    );
};

export default SlotsPage;

import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {
    useDeleteInterview,
    useInterviews,
    type InterviewScope,
} from '@/features/interviews/hooks/useInterviews.ts';
import type {Interview, InterviewSchedule} from '@/features/interviews/types/interview.types.ts';
import InterviewStatusBadge from './InterviewStatusBadge.tsx';
import InterviewForm from './InterviewForm.tsx';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import '@/features/interviews/styles/Interviews.css';

const formatSchedule = (schedule: InterviewSchedule | null, locale: string): string => {
    if (!schedule?.date) {
        return '—';
    }
    const date = new Date(schedule.date).toLocaleDateString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    });
    const time = [schedule.startTime, schedule.endTime]
        .filter(Boolean)
        .map((value) => value!.slice(0, 5))
        .join(' – ');
    return time ? `${date} · ${time}` : date;
};

const InterviewsPage: React.FC = () => {
    const {t, i18n} = useTranslation();

    // Staff (ADMIN / HR_MANAGER) see every interview and can manage them; everyone
    // else sees only their own, read-only.
    const {isStaff} = usePermissions();
    const scope: InterviewScope = isStaff ? 'all' : 'mine';
    const {showSuccessToast} = useSuccessToast();

    const {data, isLoading, isError, refetch} = useInterviews(scope);
    const deleteInterview = useDeleteInterview();

    const [showCreate, setShowCreate] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [confirmingDeleteId, setConfirmingDeleteId] = useState<string | null>(null);

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="interviews-container">
                <div className="interviews-state interviews-error">
                    <p>{t('interviews.loadError')}</p>
                    <button className="interviews-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
            </div>
        );
    }

    const interviews = data?.interviews ?? [];
    const total = data?.page.totalElements ?? interviews.length;
    const editing = editingId ? interviews.find((i) => i.id === editingId) ?? null : null;

    const handleDelete = async (interview: Interview) => {
        try {
            await deleteInterview.mutateAsync(interview.id);
            showSuccessToast(t('interviews.form.deleteSuccess'));
            setConfirmingDeleteId(null);
        } catch (err: unknown) {
            console.error('delete interview failed:', err);
            showSuccessToast(resolveServerError(t, err));
        }
    };

    return (
        <div className="interviews-container">
            <header className="interviews-header">
                <h1 className="interviews-title">
                    {isStaff ? t('interviews.title') : t('interviews.myTitle')}
                </h1>
                <p className="interviews-subtitle">{t('interviews.count', {count: total})}</p>
            </header>

            {isStaff && (
                <div className="interviews-toolbar">
                    {editing ? (
                        <InterviewForm interview={editing} onClose={() => setEditingId(null)}/>
                    ) : showCreate ? (
                        <InterviewForm onClose={() => setShowCreate(false)}/>
                    ) : (
                        <button
                            type="button"
                            className="interviews-create-toggle"
                            onClick={() => setShowCreate(true)}
                        >
                            + {t('interviews.form.schedule')}
                        </button>
                    )}
                </div>
            )}

            {interviews.length === 0 ? (
                <div className="interviews-state interviews-empty">
                    <p>{isStaff ? t('interviews.empty') : t('interviews.emptyMine')}</p>
                </div>
            ) : (
                <div className="interviews-table-wrapper">
                    <table className="interviews-table">
                        <thead>
                            <tr>
                                <th>{t('interviews.columns.offer')}</th>
                                <th>{t('interviews.columns.candidate')}</th>
                                <th>{t('interviews.columns.interviewer')}</th>
                                <th>{t('interviews.columns.when')}</th>
                                <th>{t('interviews.columns.status')}</th>
                                <th>{t('interviews.columns.mode')}</th>
                                {isStaff && <th>{t('common.actions')}</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {interviews.map((interview) => (
                                <tr key={interview.id}>
                                    <td>
                                        <div className="cell-strong">{interview.offer?.title ?? '—'}</div>
                                        {interview.offer?.department && (
                                            <div className="cell-muted">{interview.offer.department}</div>
                                        )}
                                    </td>
                                    <td>
                                        <div className="cell-strong">{interview.candidate.fullName}</div>
                                        <div className="cell-muted">{interview.candidate.email}</div>
                                    </td>
                                    <td>{interview.interviewer.fullName}</td>
                                    <td>{formatSchedule(interview.schedule, i18n.language)}</td>
                                    <td>
                                        <InterviewStatusBadge status={interview.status}/>
                                    </td>
                                    <td>
                                        {interview.isOnline ? (
                                            interview.meetingUrl ? (
                                                <a
                                                    className="meeting-link"
                                                    href={interview.meetingUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                >
                                                    {t('interviews.join')}
                                                </a>
                                            ) : (
                                                t('interviews.online')
                                            )
                                        ) : (
                                            t('interviews.inPerson')
                                        )}
                                    </td>
                                    {isStaff && (
                                        <td>
                                            {confirmingDeleteId === interview.id ? (
                                                <div className="interview-row-actions">
                                                    <span className="cell-muted">{t('interviews.form.deleteConfirm')}</span>
                                                    <button
                                                        type="button"
                                                        className="interview-action interview-action-danger"
                                                        disabled={deleteInterview.isPending}
                                                        onClick={() => handleDelete(interview)}
                                                    >
                                                        {t('common.yes')}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="interview-action"
                                                        onClick={() => setConfirmingDeleteId(null)}
                                                    >
                                                        {t('common.no')}
                                                    </button>
                                                </div>
                                            ) : (
                                                <div className="interview-row-actions">
                                                    <button
                                                        type="button"
                                                        className="interview-action"
                                                        onClick={() => {
                                                            setShowCreate(false);
                                                            setEditingId(interview.id);
                                                        }}
                                                    >
                                                        {t('common.edit')}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="interview-action interview-action-danger"
                                                        onClick={() => setConfirmingDeleteId(interview.id)}
                                                    >
                                                        {t('common.delete')}
                                                    </button>
                                                </div>
                                            )}
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default InterviewsPage;

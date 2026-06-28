import React from 'react';
import {useTranslation} from 'react-i18next';
import {useInterviews, type InterviewScope} from '@/features/interviews/hooks/useInterviews.ts';
import type {InterviewSchedule} from '@/features/interviews/types/interview.types.ts';
import InterviewStatusBadge from './InterviewStatusBadge.tsx';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
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

    // Staff (ADMIN / HR_MANAGER) see every interview; everyone else sees their own.
    const {isStaff} = usePermissions();
    const scope: InterviewScope = isStaff ? 'all' : 'mine';

    const {data, isLoading, isError, refetch} = useInterviews(scope);

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

    return (
        <div className="interviews-container">
            <header className="interviews-header">
                <h1 className="interviews-title">
                    {isStaff ? t('interviews.title') : t('interviews.myTitle')}
                </h1>
                <p className="interviews-subtitle">{t('interviews.count', {count: total})}</p>
            </header>

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

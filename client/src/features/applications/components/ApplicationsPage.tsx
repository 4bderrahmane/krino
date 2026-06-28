import React from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router-dom';
import {useApplications, type ApplicationScope} from '@/features/applications/hooks/useApplications.ts';
import {useResumeDownload} from '@/features/applications/hooks/useResumeDownload.ts';
import ApplicationStatusBadge from './ApplicationStatusBadge.tsx';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
import '@/features/applications/styles/Applications.css';

const formatDate = (iso: string | null, locale: string): string =>
    iso
        ? new Date(iso).toLocaleDateString(locale, {year: 'numeric', month: 'short', day: 'numeric'})
        : '—';

const ApplicationsPage: React.FC = () => {
    const {t, i18n} = useTranslation();

    // Staff (ADMIN / HR_MANAGER) see every application; everyone else sees their own.
    const {isStaff} = usePermissions();
    const scope: ApplicationScope = isStaff ? 'all' : 'mine';

    const {data, isLoading, isError, refetch} = useApplications(scope);
    const navigate = useNavigate();

    // Résumé download lives behind an authenticated endpoint (see the hook).
    // Failures surface as an error toast from within the hook.
    const {downloadingId, openResume} = useResumeDownload();

    const openDetail = (id: string) => navigate(`/applications/${id}`);

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="applications-container">
                <div className="applications-state applications-error">
                    <p>{t('applications.loadError')}</p>
                    <button className="applications-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
            </div>
        );
    }

    const applications = data?.applications ?? [];
    const total = data?.page.totalElements ?? applications.length;

    return (
        <div className="applications-container">
            <header className="applications-header">
                <h1 className="applications-title">
                    {isStaff ? t('applications.title') : t('applications.myTitle')}
                </h1>
                <p className="applications-subtitle">{t('applications.count', {count: total})}</p>
            </header>

            {applications.length === 0 ? (
                <div className="applications-state applications-empty">
                    <p>{isStaff ? t('applications.empty') : t('applications.emptyMine')}</p>
                </div>
            ) : (
                <div className="applications-table-wrapper">
                        <table className="applications-table">
                            <thead>
                                <tr>
                                    {isStaff && <th>{t('applications.columns.candidate')}</th>}
                                    <th>{t('applications.columns.offer')}</th>
                                    <th>{t('applications.columns.status')}</th>
                                    <th>{t('applications.columns.appliedAt')}</th>
                                    <th>{t('applications.columns.resume')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {applications.map((application) => (
                                    <tr
                                        key={application.id}
                                        className="application-row"
                                        tabIndex={0}
                                        role="button"
                                        aria-label={t('applications.viewDetails')}
                                        onClick={() => openDetail(application.id)}
                                        onKeyDown={(e) => {
                                            if (e.key === 'Enter' || e.key === ' ') {
                                                e.preventDefault();
                                                openDetail(application.id);
                                            }
                                        }}
                                    >
                                        {isStaff && (
                                            <td>
                                                <div className="candidate-name">{application.candidate.fullName}</div>
                                                <div className="candidate-email">{application.candidate.email}</div>
                                            </td>
                                        )}
                                        <td>
                                            <div className="application-offer-title">
                                                {application.offerTitle || '—'}
                                            </div>
                                            {application.offerDepartment && (
                                                <div className="application-offer-dept">
                                                    {application.offerDepartment}
                                                </div>
                                            )}
                                        </td>
                                        <td>
                                            <ApplicationStatusBadge status={application.status}/>
                                        </td>
                                        <td>{formatDate(application.appliedAt, i18n.language)}</td>
                                        <td>
                                            {application.resume ? (
                                                <button
                                                    type="button"
                                                    className="resume-link"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        openResume(application.id);
                                                    }}
                                                    disabled={downloadingId === application.id}
                                                >
                                                    {downloadingId === application.id
                                                        ? t('applications.downloading')
                                                        : (application.resume.filename || t('applications.viewResume'))}
                                                </button>
                                            ) : (
                                                '—'
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

export default ApplicationsPage;

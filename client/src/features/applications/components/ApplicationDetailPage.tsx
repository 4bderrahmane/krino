import React from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useParams} from 'react-router-dom';
import {useApplication} from '@/features/applications/hooks/useApplications.ts';
import {useResumeDownload} from '@/features/applications/hooks/useResumeDownload.ts';
import ApplicationStatusBadge from './ApplicationStatusBadge.tsx';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import '@/features/applications/styles/Applications.css';

const formatDate = (iso: string | null, locale: string): string =>
    iso
        ? new Date(iso).toLocaleDateString(locale, {year: 'numeric', month: 'short', day: 'numeric'})
        : '—';

// Human-readable file size, e.g. "1.2 MB".
const formatFileSize = (bytes: number | null): string | null => {
    if (bytes == null) return null;
    if (bytes < 1024) return `${bytes} B`;
    const units = ['KB', 'MB', 'GB'];
    let size = bytes / 1024;
    let unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
        size /= 1024;
        unit += 1;
    }
    return `${size.toFixed(1)} ${units[unit]}`;
};

const ApplicationDetailPage: React.FC = () => {
    const {t, i18n} = useTranslation();
    const locale = i18n.language;
    const {id} = useParams<{id: string}>();

    const {data: application, isLoading, isError, refetch} = useApplication(id);
    const {downloadingId, openResume} = useResumeDownload();

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError || !application) {
        return (
            <div className="application-detail-container">
                <div className="applications-state applications-error">
                    <p>{t('applications.loadError')}</p>
                    <button className="applications-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
                <Link className="application-back" to="/applications">{t('applications.detail.back')}</Link>
            </div>
        );
    }

    const {resume} = application;
    const resumeSize = resume ? formatFileSize(resume.sizeBytes) : null;

    return (
        <div className="application-detail-container">
            <Link className="application-back" to="/applications">{t('applications.detail.back')}</Link>

            <article className="application-detail-card">
                <header className="application-detail-header">
                    {application.offerDepartment && (
                        <span className="application-detail-eyebrow">{application.offerDepartment}</span>
                    )}
                    <h1 className="application-detail-title">
                        {application.offerTitle || t('applications.detail.untitledOffer')}
                    </h1>
                    <div className="application-detail-status">
                        <ApplicationStatusBadge status={application.status}/>
                        <span className="application-detail-applied">
                            {t('applications.detail.appliedOn', {date: formatDate(application.appliedAt, locale)})}
                        </span>
                    </div>
                </header>

                <section className="application-detail-section">
                    <h2 className="application-detail-heading">{t('applications.detail.candidate')}</h2>
                    <dl className="application-detail-grid">
                        <div className="application-detail-row">
                            <dt>{t('applications.detail.name')}</dt>
                            <dd>{application.candidate.fullName || '—'}</dd>
                        </div>
                        <div className="application-detail-row">
                            <dt>{t('applications.detail.email')}</dt>
                            <dd>
                                <a href={`mailto:${application.candidate.email}`}>{application.candidate.email}</a>
                            </dd>
                        </div>
                    </dl>
                </section>

                <section className="application-detail-section">
                    <h2 className="application-detail-heading">{t('applications.detail.offer')}</h2>
                    <Link className="application-detail-offer-link" to={`/offers/${application.offerId}`}>
                        {application.offerTitle || t('applications.detail.untitledOffer')}
                        <span aria-hidden="true"> →</span>
                    </Link>
                </section>

                <section className="application-detail-section">
                    <h2 className="application-detail-heading">{t('applications.detail.resume')}</h2>
                    {resume ? (
                        <div className="application-detail-resume">
                            <div className="application-detail-resume-meta">
                                <span className="application-detail-resume-name">
                                    {resume.filename || t('applications.viewResume')}
                                </span>
                                <span className="application-detail-resume-sub">
                                    {[resumeSize, formatDate(resume.uploadedAt, locale)]
                                        .filter(Boolean)
                                        .join(' · ')}
                                </span>
                            </div>
                            <button
                                type="button"
                                className="application-detail-download"
                                onClick={() => openResume(application.id)}
                                disabled={downloadingId === application.id}
                            >
                                {downloadingId === application.id
                                    ? t('applications.downloading')
                                    : t('applications.detail.downloadResume')}
                            </button>
                        </div>
                    ) : (
                        <p className="application-detail-empty">{t('applications.detail.noResume')}</p>
                    )}
                </section>
            </article>
        </div>
    );
};

export default ApplicationDetailPage;

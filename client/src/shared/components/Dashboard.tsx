import React, {useEffect} from 'react';
import {Link} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {MdWork, MdAssignment, MdEvent, MdPerson, MdSettings, MdArrowForward} from 'react-icons/md';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import {useOffers} from '@/features/offers/hooks/useOffers.ts';
import {useApplications, type ApplicationScope} from '@/features/applications/hooks/useApplications.ts';
import {useInterviews, type InterviewScope} from '@/features/interviews/hooks/useInterviews.ts';
import '@/shared/styles/Dashboard.css';

const greetingPart = (hour: number): 'morning' | 'afternoon' | 'evening' => {
    if (hour < 12) return 'morning';
    if (hour < 18) return 'afternoon';
    return 'evening';
};

interface StatCardProps {
    to: string;
    icon: React.ReactNode;
    label: string;
    value?: number;
    isLoading: boolean;
    isError: boolean;
}

const StatCard: React.FC<StatCardProps> = ({to, icon, label, value, isLoading, isError}) => (
    <Link to={to} className="dashboard-stat">
        <span className="dashboard-stat-icon" aria-hidden="true">{icon}</span>
        <span className="dashboard-stat-body">
            {isLoading ? (
                <span className="dashboard-stat-skeleton" aria-hidden="true"/>
            ) : (
                <span className="dashboard-stat-value">{isError ? '—' : (value ?? 0)}</span>
            )}
            <span className="dashboard-stat-label">{label}</span>
        </span>
        <MdArrowForward className="dashboard-stat-arrow" aria-hidden="true"/>
    </Link>
);

const Dashboard: React.FC = () => {
    const {t, i18n} = useTranslation();
    const {user, justLoggedIn, clearJustLoggedIn} = useAuth();
    const {showSuccessToast} = useSuccessToast();

    useEffect(() => {
        if (justLoggedIn) {
            showSuccessToast(t('auth.success.loginSuccess'), 3000);
            clearJustLoggedIn();
        }
    }, [justLoggedIn, showSuccessToast, clearJustLoggedIn, t]);

    const {isStaff} = usePermissions();
    const appScope: ApplicationScope = isStaff ? 'all' : 'mine';
    const interviewScope: InterviewScope = isStaff ? 'all' : 'mine';

    // Counts are cheap: offers come back in full, applications/interviews expose
    // the true total via page.totalElements even though we only load one page.
    const offers = useOffers();
    const applications = useApplications(appScope);
    const interviews = useInterviews(interviewScope);

    if (!user) {
        return <LoadingSpinner/>;
    }

    const now = new Date();
    const dateLabel = now.toLocaleDateString(i18n.language, {weekday: 'long', month: 'long', day: 'numeric'});
    const greeting = t(`dashboard.greeting.${greetingPart(now.getHours())}`, {name: user.firstName});

    return (
        <div className="dashboard-container">
            <header className="dashboard-hero">
                <p className="dashboard-eyebrow">{dateLabel}</p>
                <h1 className="dashboard-greeting">{greeting}</h1>
                <p className="dashboard-subtitle">
                    {isStaff ? t('dashboard.subtitleStaff') : t('dashboard.subtitleCandidate')}
                </p>
            </header>

            <section className="dashboard-stats" aria-label={t('dashboard.overview')}>
                <StatCard
                    to="/offers"
                    icon={<MdWork/>}
                    label={t('dashboard.stats.offers')}
                    value={offers.data?.offers.length}
                    isLoading={offers.isLoading}
                    isError={offers.isError}
                />
                <StatCard
                    to="/applications"
                    icon={<MdAssignment/>}
                    label={isStaff ? t('dashboard.stats.applications') : t('dashboard.stats.myApplications')}
                    value={applications.data?.page.totalElements}
                    isLoading={applications.isLoading}
                    isError={applications.isError}
                />
                <StatCard
                    to="/interviews"
                    icon={<MdEvent/>}
                    label={isStaff ? t('dashboard.stats.interviews') : t('dashboard.stats.myInterviews')}
                    value={interviews.data?.page.totalElements}
                    isLoading={interviews.isLoading}
                    isError={interviews.isError}
                />
            </section>

            <section>
                <h2 className="dashboard-section-title">{t('dashboard.manage')}</h2>
                <div className="dashboard-links">
                    <Link to="/me" className="dashboard-link">
                        <span className="dashboard-link-icon" aria-hidden="true"><MdPerson/></span>
                        <span className="dashboard-link-text">
                            <span className="dashboard-link-title">{t('dashboard.links.profile')}</span>
                            <span className="dashboard-link-desc">{t('dashboard.links.profileDesc')}</span>
                        </span>
                    </Link>
                    <Link to="/settings" className="dashboard-link">
                        <span className="dashboard-link-icon" aria-hidden="true"><MdSettings/></span>
                        <span className="dashboard-link-text">
                            <span className="dashboard-link-title">{t('dashboard.links.settings')}</span>
                            <span className="dashboard-link-desc">{t('dashboard.links.settingsDesc')}</span>
                        </span>
                    </Link>
                </div>
            </section>
        </div>
    );
};

export default Dashboard;

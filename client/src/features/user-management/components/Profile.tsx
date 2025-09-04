import React, {useState, useEffect} from 'react';
import {useAuth} from '../../../shared/hooks/useAuth';
import '../styles/Profile.css';
import {useTranslation} from "react-i18next";

interface ActivityStats {
    totalInterviews: number;
    upcomingInterviews: number;
    completedInterviews: number;
    pendingApplications: number;
}

const Profile: React.FC = () => {
    const {t} = useTranslation();
    const {user} = useAuth();
    const [activityStats, setActivityStats] = useState<ActivityStats>({
        totalInterviews: 0,
        upcomingInterviews: 0,
        completedInterviews: 0,
        pendingApplications: 0,
    });
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (user) {
            fetchUserActivity();
        }
    }, [user]);

    const fetchUserActivity = async () => {
        try {
            await new Promise((resolve) => setTimeout(resolve, 800));

            setActivityStats({
                totalInterviews: 8,
                upcomingInterviews: 3,
                completedInterviews: 5,
                pendingApplications: 2,
            });
        } catch (error) {
            console.error('Error fetching user activity:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const getUserInitials = () => {
        if (user?.firstName && user?.lastName) {
            return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
        }
        if (user?.firstName) {
            return user.firstName.charAt(0).toUpperCase();
        }
        if (user?.username) {
            return user.username.charAt(0).toUpperCase();
        }
        return 'U';
    };

    const getFullName = () => {
        if (user?.firstName && user?.lastName) {
            return `${user.firstName} ${user.lastName}`;
        }
        return user?.firstName || user?.username || 'Unknown User';
    };

    const getRoleDisplay = () => {
        if (!user?.roles || user.roles.size === 0) return 'User';
        return Array.from(user.roles).join(', ');
    };

    if (!user) {
        return <div className="profile-empty">No user data available.</div>;
    }

    return (
        <div className="profile-container">
            <div className="profile-header">
                <h1 className="profile-title">{t("profile.myProfile")}</h1>
            </div>

            <div className="profile-content">
                <div className="profile-card">
                    <div className="profile-image-section">
                        <div className="profile-avatar">
                            {getUserInitials()}
                        </div>
                        <div className="profile-name">
                            <h2>{getFullName()}</h2>
                            <span className="profile-role">{getRoleDisplay()}</span>
                        </div>
                    </div>

                    <div className="profile-details">
                        <div className="profile-info">
                            <div className="info-row">
                                <span className="info-label">{t('profile.firstName')}</span>
                                <span className="info-value">{user.firstName || 'Not provided'}</span>
                            </div>
                            <div className="info-row">
                                <span className="info-label">{t('profile.lastName')}</span>
                                <span className="info-value">{user.lastName || 'Not provided'}</span>
                            </div>
                            <div className="info-row">
                                <span className="info-label">{t('profile.username')}</span>
                                <span className="info-value">{user.username}</span>
                            </div>
                            <div className="info-row">
                                <span className="info-label">{t('profile.email')}</span>
                                <span className="info-value">{user.email}</span>
                            </div>
                            <div className="info-row">
                                <span className="info-label">{t('profile.phoneNumber')}</span>
                                <span className="info-value">{'+212-'}{user.phoneNumber || 'Not provided'}</span>
                            </div>
                            <div className="info-row">
                                <span className="info-label">{t('profile.id')}</span>
                                <span className="info-value">#{user.id}</span>
                            </div>
                            <div className="info-note">
                                <p>
                                    To change your account details, please contact your administrator or visit the{' '}
                                    <a href="/settings" className="settings-link">Settings page</a>.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="activity-stats">
                    <h2 className="stats-title">Activity Summary</h2>
                    {isLoading ? (
                        <div className="stats-loading">Loading activity data...</div>
                    ) : (
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="stat-icon">📊</div>
                                <div className="stat-value">{activityStats.totalInterviews}</div>
                                <div className="stat-label">Total Interviews</div>
                            </div>
                            <div className="stat-card upcoming">
                                <div className="stat-icon">📅</div>
                                <div className="stat-value">{activityStats.upcomingInterviews}</div>
                                <div className="stat-label">Upcoming</div>
                            </div>
                            <div className="stat-card completed">
                                <div className="stat-icon">✅</div>
                                <div className="stat-value">{activityStats.completedInterviews}</div>
                                <div className="stat-label">Completed</div>
                            </div>
                            <div className="stat-card pending">
                                <div className="stat-icon">⏳</div>
                                <div className="stat-value">{activityStats.pendingApplications}</div>
                                <div className="stat-label">Pending Applications</div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Profile;

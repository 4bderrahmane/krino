import React, { useEffect, useState } from 'react';
import '../styles/Profile.css';
import {useTranslation} from "react-i18next";
import { Link } from 'react-router-dom';
import { getCurrentUser } from '../services/UserManagementService';
import type { UserResponseDTO } from '../../authentication/types/api.types';

const Profile: React.FC = () => {
    const {t} = useTranslation();
    const [user, setUser] = useState<UserResponseDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setLoading(true);
        getCurrentUser()
            .then(setUser)
            .catch(() => setError('Failed to load user data.'))
            .finally(() => setLoading(false));
    }, []);

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

    if (loading) {
        return <div className="profile-empty">{t('app.loading')}</div>;
    }
    if (error || !user) {
        return <div className="profile-empty">{error || 'No user data available.'}</div>;
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
                                    {t('profile.changeDataSentence')}
                                    {' '}
                                    <Link to="/settings" className="settings-link">{t('page.settings')}</Link>.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default Profile;

import React, {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import type {User} from "../types/types.ts";
import {useAuth} from '../hooks/useAuth';
import {useToast} from "../contexts/ToastContext.tsx";

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const {user: authUser, logout: authLogout, justLoggedIn, clearJustLoggedIn} = useAuth();
    const {showToast} = useToast();

    useEffect(() => {
        if (justLoggedIn) {
            showToast(t('auth.success.loginSuccess'), 3000);
            clearJustLoggedIn();
        }
    }, [justLoggedIn, showToast, clearJustLoggedIn, t]);

    if (!authUser) {
        return <div>Loading user data...</div>;
    }

    const dashboardUser: User = {
        username: authUser.username,
        email: authUser.email,
        firstName: authUser.firstName,
        lastName: authUser.lastName,
        phoneNumber: parseInt(authUser.phoneNumber) || 0,
        roles: new Set(authUser.roles),
    };

    function handleLogoutButtonClick() {
        handleLogout();
        handleShowToast();
    }

    const handleLogout = async () => {
        try {
            await import('../../features/authentication/services/AuthenticationService.ts').then(
                service => service.logout()
            );
        } catch (error) {
            console.error('Logout request failed:', error);
        }

        authLogout();
        navigate('/login');
    };

    const handleShowToast = () => {
        showToast("Logged in successfully!", 3000);
    };

    return (
        <div className="login-page-container">
            <div className="login-card">
                <h2>{t('auth.welcome')}</h2>
                <p style={{textAlign: 'center', marginBottom: '1rem', color: '#6b7280'}}>
                    {t('auth.loggedInAs')} <strong style={{color: '#1e3a8a'}}>{dashboardUser.email}</strong>
                </p>
                <button
                    onClick={handleLogoutButtonClick}
                    className="login-button"
                >
                    {t('auth.logout')}
                </button>
            </div>
        </div>
    );
};

export default Dashboard;
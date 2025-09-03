import React, {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import Navbar from "./NavBar.tsx";
import SuccessToast from './SuccessToast';
import type {User} from "../types/types.ts";
import {useAuth} from '../hooks/useAuth';

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const {user: authUser, logout: authLogout, justLoggedIn, clearJustLoggedIn} = useAuth();

    useEffect(() => {
        console.log('Dashboard useEffect - justLoggedIn:', justLoggedIn);
        // Don't clear the flag in cleanup - let the toast handle its own timing
    }, [justLoggedIn, clearJustLoggedIn]);


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

    const handleLogout = async () => {
        try {
            // Call the logout service to clear cookies on the backend
            await import('../../features/authentication/services/AuthenticationService.ts').then(
                service => service.logout()
            );
        } catch (error) {
            console.error('Logout request failed:', error);
            // Continue with local logout even if backend call fails
        }

        authLogout();
        navigate('/login');
    };

    const handleCloseToast = () => {
        clearJustLoggedIn();
    };

    const navbarProps = {
        username: dashboardUser.username,
        onLogout: handleLogout,
    };

    return (
        <>
            <div className="app-container">
                {dashboardUser && <Navbar {...navbarProps}/>}
                <div className="login-page-container">
                    <div className="login-card">
                        <h2>{t('auth.welcome')}</h2>
                        <p style={{textAlign: 'center', marginBottom: '1rem', color: '#6b7280'}}>
                            {t('auth.loggedInAs')} <strong style={{color: '#1e3a8a'}}>{dashboardUser.email}</strong>
                        </p>
                        <button
                            onClick={handleLogout}
                            className="login-button"
                        >
                            {t('auth.logout')}
                        </button>
                    </div>
                </div>
            </div>

            <SuccessToast
                message={t('auth.success.loginSuccess')}
                isVisible={justLoggedIn}
                onClose={handleCloseToast}
                duration={4000}
            />
        </>
    );
};

export default Dashboard;
import React, {useState} from 'react';
import {Link, useNavigate, useLocation} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '../styles/NavBar.css';
import LanguageSwitcher from './LanguageSwitcher';
import SuccessToast from './SuccessToast';
import Welcome from "./Welcome.tsx";
import {useSuccessToast} from '../hooks/useSuccessToast';
import {useAuth} from '../hooks/useAuth';

const Navbar: React.FC = () => {
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const {user, logout: authLogout} = useAuth();
    const {isVisible, message, showSuccess, hideSuccess} = useSuccessToast();

    const toggleDropdown = () => {
        setIsDropdownOpen(!isDropdownOpen);
    };

    const handleLogout = async () => {
        // Close dropdown first
        setIsDropdownOpen(false);

        // Show success toast
        showSuccess(t('auth.success.logoutSuccess'));

        try {
            // Call the logout service to clear cookies on the backend
            await import('../../features/authentication/services/AuthenticationService.ts').then(
                service => service.logout()
            );
        } catch (error) {
            console.error('Logout request failed:', error);
            // Continue with local logout even if backend call fails
        }

        // Perform logout and navigate immediately
        authLogout();
        navigate('/login');
    };

    const isCurrentPage = (path: string) => {
        return location.pathname === path;
    };

    const username = user?.username || 'User';

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <Link to="/dashboard">{
                    <Welcome/>
                }</Link>
            </div>

            <div className="navbar-menu">
                <ul className="navbar-links">
                    <li className={isCurrentPage('/dashboard') ? 'active' : ''}>
                        <Link to="/dashboard">{t('nav.dashboard')}</Link>
                    </li>
                    <li className={isCurrentPage('/applications') ? 'active' : ''}>
                        <Link to="/applications">{t('nav.applications')}</Link>
                    </li>
                    <li className={isCurrentPage('/jobs') ? 'active' : ''}>
                        <Link to="/jobs">{t('nav.jobs')}</Link>
                    </li>
                    <li className={isCurrentPage('/timeslots') ? 'active' : ''}>
                        <Link to="/timeslots">
                            {t('nav.availability')}
                        </Link>
                    </li>
                </ul>

                <div className="navbar-actions">
                    <LanguageSwitcher/>

                    <div className="navbar-user">
                        <div className="user-profile" onClick={toggleDropdown}>
                            <span className="user-name">{username}</span>
                            <span className="dropdown-icon">▼</span>
                        </div>

                        {isDropdownOpen && (
                            <div className="dropdown-menu">
                                <Link to="/me" className="dropdown-item">
                                    {t('nav.profile')}
                                </Link>
                                <Link to="/settings" className="dropdown-item">
                                    {t('nav.settings')}
                                </Link>
                                <div className="dropdown-divider"></div>
                                <button
                                    className="dropdown-item logout-button"
                                    onClick={handleLogout}
                                >
                                    {t('auth.logout')}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <SuccessToast
                isVisible={isVisible}
                message={message}
                onClose={hideSuccess}
            />
        </nav>
    );
};

export default Navbar;

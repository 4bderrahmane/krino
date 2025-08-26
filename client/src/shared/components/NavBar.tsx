import React, {useState} from 'react';
import {Link, useNavigate, useLocation} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '../styles/NavBar.css';
import LanguageSwitcher from './LanguageSwitcher';
import SuccessToast from './SuccessToast';
import type {NavbarProps} from "../types/types.ts";
import Welcome from "./Welcome.tsx";
import {useSuccessToast} from '../hooks/useSuccessToast';
import {useAuth} from '../contexts/AuthContext';

const Navbar: React.FC<NavbarProps> = ({username = 'User', onLogout}) => {
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const {logout: authLogout} = useAuth();
    const {isVisible, message, showSuccess, hideSuccess} = useSuccessToast();

    const toggleDropdown = () => {
        setIsDropdownOpen(!isDropdownOpen);
    };

    const handleLogout = async () => {
        // Close dropdown first
        setIsDropdownOpen(false);

        // Show success toast
        showSuccess(t('auth.success.logoutSuccess'));

        // Wait for toast to show before logout and navigation
        await new Promise((resolve) => setTimeout(resolve, 2000));

        // Perform logout and navigate
        authLogout();
        navigate('/login');
    };

    const isCurrentPage = (path: string) => {
        return location.pathname === path;
    };

    // const appTitle = t('app.title');

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
                    <li className={isCurrentPage('/bookings') ? 'active' : ''}>
                        <Link to="/bookings">{t('nav.bookings')}</Link>
                    </li>
                    <li className={isCurrentPage('/reserve') ? 'active' : ''}>
                        <Link to="/reserve">{t('nav.reserve')}</Link>
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
                                <Link to="/profile" className="dropdown-item">
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

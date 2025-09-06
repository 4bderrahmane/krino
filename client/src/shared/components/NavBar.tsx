import React, {useState} from 'react';
import {Link, useNavigate, useLocation} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '../styles/NavBar.css';
import LanguageSwitcher from './LanguageSwitcher';
import SuccessToast from './SuccessToast';
import Welcome from "./Welcome.tsx";
import {useSuccessToast} from '../hooks/useSuccessToast';
import {useAuth} from '../hooks/useAuth';
import useOutsideClick from '../hooks/useOutsideClick';
import {MdSettings, MdPerson, MdLogout} from "react-icons/md";
import {logout as logoutService} from '../../features/authentication/services/AuthenticationService';
import SettingsDropDown from "../../features/user-management/components/settings/SettingsDropDown.tsx";

const NavBar: React.FC = () => {

    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const {user, logout: authLogout} = useAuth();
    const {isVisible, message, showSuccess, hideSuccess} = useSuccessToast();

    const toggleDropdown = () => {
        setIsDropdownOpen(!isDropdownOpen);
    };

    const closeDropdown = () => {
        setIsDropdownOpen(false);
    };

    const dropdownRef = useOutsideClick(closeDropdown);

    const handleLogout = async () => {
        closeDropdown();

        showSuccess(t('auth.success.logoutSuccess'));

        try {
            // Call the logout service to clear cookies on the backend
            await logoutService();
        } catch (error) {
            console.error('Logout request failed:', error);
        }

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

                    <div className="navbar-user" ref={dropdownRef}>
                        <div className="user-profile" onClick={toggleDropdown}>
                            <span className="user-name">{username}</span>
                            <span className="dropdown-icon">▼</span>
                        </div>

                        {isDropdownOpen && (
                            <div className="dropdown-menu">
                                <Link to="/me" className="dropdown-item flex items-center gap-2"
                                      onClick={closeDropdown}>
                                    <MdPerson className="dropdown-icon-svg"/> <span>{t('nav.profile')}</span>
                                </Link>

                                <div className="dropdown-item dropdown-parent flex items-center gap-2 settings-parent">
                                    <MdSettings className="dropdown-icon-svg"/>
                                    <span>{t('nav.settings')}</span>
                                    <div className="dropdown-submenu">
                                        <SettingsDropDown onClose={closeDropdown}/>
                                    </div>
                                </div>

                                <div className="dropdown-divider"></div>

                                <button
                                    className="dropdown-item flex items-center gap-2 logout-button"
                                    onClick={handleLogout}
                                >
                                    <MdLogout className="dropdown-icon-svg"/> <span>{t('auth.logout')}</span>
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

export default NavBar;

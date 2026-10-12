import React, {useState} from 'react';
import {Link, useNavigate, useLocation} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '@/shared/styles/NavBar.css';
import LanguageSwitcher from './LanguageSwitcher';
import BrandLogo from "@/shared/components/BrandLogo.tsx";
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import useOutsideClick from '@/shared/hooks/useOutsideClick';
import ThemeToggle from '@/shared/components/ThemeToggle.tsx';
import {MdSettings, MdPerson, MdLogout} from "react-icons/md";
import {logout as logoutService} from '@/features/authentication/services/AuthenticationService';
import SettingsDropDown from "@/features/user-management/components/settings/SettingsDropDown.tsx";

const NavBar: React.FC = () => {

    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();
    const {user, logout: authLogout} = useAuth();
    const { showSuccessToast } = useSuccessToast();

    const toggleDropdown = () => {
        setIsDropdownOpen(!isDropdownOpen);
    };

    const closeDropdown = () => {
        setIsDropdownOpen(false);
    };

    const dropdownRef = useOutsideClick(closeDropdown);

    const handleLogout = async () => {
        closeDropdown();

        showSuccessToast(t('auth.success.logoutSuccess'));

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

    const {isAdmin, isStaff, isInterviewer} = usePermissions();

    const displayName = user?.firstName || 'User';

    // Signing in from a job page should come back to that job, not dump the
    // visitor on the dashboard having lost what they were reading.
    const signInTarget = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`;

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <Link to={user ? '/dashboard' : '/offers'} aria-label="KRINO">
                    <BrandLogo variant="navbar"/>
                </Link>
            </div>

            {/* A visitor gets the catalogue and nothing else: every other
                destination needs a session, and Departments is staff-only. */}
            <ul className="navbar-links">
                    {user && (
                        <li className={isCurrentPage('/dashboard') ? 'active' : ''}>
                            <Link to="/dashboard">
                                {t('nav.dashboard')}
                            </Link>
                        </li>
                    )}
                    {user && (
                        <li className={isCurrentPage('/applications') ? 'active' : ''}>
                            <Link to="/applications">
                                {t('nav.applications')}
                            </Link>
                        </li>
                    )}
                    <li className={isCurrentPage('/offers') ? 'active' : ''}>
                        <Link to="/offers">
                            {t('nav.offers')}
                        </Link>
                    </li>
                    {user && (
                        <li className={isCurrentPage('/interviews') ? 'active' : ''}>
                            <Link to="/interviews">
                                {t('nav.interviews')}
                            </Link>
                        </li>
                    )}
                    {/* Public, like the catalogue: a visitor sees the departments that
                        are hiring, staff see the whole directory behind the same link. */}
                    <li className={isCurrentPage('/departments') ? 'active' : ''}>
                        <Link to="/departments">
                            {t('nav.departments')}
                        </Link>
                    </li>
                    {(isStaff || isInterviewer) && (
                        <li className={isCurrentPage('/slots') ? 'active' : ''}>
                            <Link to="/slots">
                                {t('nav.availability')}
                            </Link>
                        </li>
                    )}
                    {isStaff && (
                        <li className={isCurrentPage('/admin/users') ? 'active' : ''}>
                            <Link to="/admin/users">
                                {t('nav.users')}
                            </Link>
                        </li>
                    )}
                    {isAdmin && (
                        <li className={isCurrentPage('/admin/staff') ? 'active' : ''}>
                            <Link to="/admin/staff">
                                {t('nav.staff')}
                            </Link>
                        </li>
                    )}
                </ul>

                <div className="navbar-actions">
                    <ThemeToggle/>

                    <LanguageSwitcher/>

                    {user ? (
                        <div className="navbar-user" ref={dropdownRef}>
                            <button className="user-profile" onClick={toggleDropdown}>
                                <span className="user-name">{displayName}</span>
                                <span className="dropdown-icon">▼</span>
                            </button>

                            {isDropdownOpen && (
                                <div className="dropdown-menu">
                                    <Link to="/me" className="dropdown-item"
                                          onClick={closeDropdown}>
                                        <MdPerson className="dropdown-icon-svg"/> <span>{t('nav.profile')}</span>
                                    </Link>

                                    <div className="dropdown-item dropdown-parent settings-parent">
                                        <MdSettings className="dropdown-icon-svg"/>
                                        <span>{t('nav.settings')}</span>
                                        <div className="dropdown-submenu">
                                            <SettingsDropDown onClose={closeDropdown}/>
                                        </div>
                                    </div>

                                    <div className="dropdown-divider"></div>

                                    <button
                                        className="dropdown-item logout-button"
                                        onClick={handleLogout}
                                    >
                                        <MdLogout className="dropdown-icon-svg"/> <span>{t('auth.logout')}</span>
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        // Signed out: the way in, rather than a menu for an account
                        // that does not exist yet.
                        <div className="navbar-auth">
                            <Link className="navbar-signin" to={signInTarget}>{t('auth.signIn')}</Link>
                            <Link className="navbar-signup" to="/register">{t('auth.signUp')}</Link>
                        </div>
                    )}
                </div>
        </nav>
    );
};

export default NavBar;

import React from 'react';
import {useTranslation} from 'react-i18next';
import {MdOutlineLightMode, MdOutlineDarkMode} from 'react-icons/md';
import {useTheme} from '@/shared/hooks/useTheme';
import '@/shared/styles/ThemeToggle.css';

interface ThemeToggleProps {
    className?: string;
}

// Light/dark switch. Self-contained (imports its own styles) so it works both in
// the authenticated navbar and on the auth pages, which don't load the navbar.
const ThemeToggle: React.FC<ThemeToggleProps> = ({className}) => {
    const {t} = useTranslation();
    const {theme, toggleTheme} = useTheme();
    const label = t(theme === 'dark' ? 'theme.switchToLight' : 'theme.switchToDark');

    return (
        <button
            type="button"
            className={className ? `theme-toggle ${className}` : 'theme-toggle'}
            onClick={toggleTheme}
            aria-label={label}
            title={label}
        >
            {theme === 'dark'
                ? <MdOutlineLightMode className="theme-toggle-icon" aria-hidden="true"/>
                : <MdOutlineDarkMode className="theme-toggle-icon" aria-hidden="true"/>}
        </button>
    );
};

export default ThemeToggle;

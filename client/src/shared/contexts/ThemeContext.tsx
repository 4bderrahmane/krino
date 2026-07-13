import React, {useCallback, useEffect, useState} from 'react';
import {ThemeContext, type Theme} from './themeContext.ts';

const STORAGE_KEY = 'krino-theme';

// The theme is normally resolved by the inline boot script in index.html, which
// sets data-theme on <html> before first paint to avoid a flash of the wrong
// theme. We read that back here; the localStorage / media-query fallbacks only
// matter if that script didn't run (e.g. SSR, script stripped).
const getInitialTheme = (): Theme => {
    const attr = document.documentElement.getAttribute('data-theme');
    if (attr === 'light' || attr === 'dark') return attr;
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'light' || stored === 'dark') return stored;
    } catch {
        /* localStorage unavailable (private mode, etc.); fall through */
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

interface ThemeProviderProps {
    children: React.ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({children}) => {
    const [theme, setThemeState] = useState<Theme>(getInitialTheme);

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch {
            /* ignore persistence failures */
        }
    }, [theme]);

    const setTheme = useCallback((next: Theme) => setThemeState(next), []);
    const toggleTheme = useCallback(
        () => setThemeState((prev) => (prev === 'dark' ? 'light' : 'dark')),
        [],
    );

    return (
        <ThemeContext.Provider value={{theme, toggleTheme, setTheme}}>
            {children}
        </ThemeContext.Provider>
    );
};

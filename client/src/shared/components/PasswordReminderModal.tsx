import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '@/shared/hooks/useAuth';
import '@/shared/styles/PasswordReminder.css';

const PASSWORD_PATH = '/settings/password';

// Shown to staff still on their admin-generated initial password
// (user.mustChangePassword). "Change now" sends them to the password page;
// "Later" snoozes it for the rest of the browser session. Once they actually
// change the password, the server clears the flag and this stops appearing.
const PasswordReminderModal: React.FC = () => {
    const {t} = useTranslation();
    const {user} = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const storageKey = user ? `krino:pw-reminder-snoozed:${user.id}` : null;
    const [snoozed, setSnoozed] = useState(false);

    // Re-read the per-session snooze whenever the signed-in user changes.
    useEffect(() => {
        if (!storageKey) {
            setSnoozed(false);
            return;
        }
        try {
            setSnoozed(sessionStorage.getItem(storageKey) === '1');
        } catch {
            setSnoozed(false);
        }
    }, [storageKey]);

    // Hide while the user is already on the password page, so it never blocks
    // the very form it points to.
    if (!user?.mustChangePassword || snoozed || location.pathname === PASSWORD_PATH) {
        return null;
    }

    const snooze = () => {
        if (storageKey) {
            try {
                sessionStorage.setItem(storageKey, '1');
            } catch {
                /* storage unavailable — fall back to in-memory snooze only */
            }
        }
        setSnoozed(true);
    };

    const handleChangeNow = () => {
        snooze();
        navigate(PASSWORD_PATH);
    };

    return (
        <div className="pw-reminder-overlay" role="dialog" aria-modal="true" aria-labelledby="pw-reminder-title">
            <div className="pw-reminder-modal">
                <h2 id="pw-reminder-title" className="pw-reminder-title">
                    {t('passwordReminder.title')}
                </h2>
                <p className="pw-reminder-text">{t('passwordReminder.body')}</p>
                <div className="pw-reminder-actions">
                    <button type="button" className="pw-reminder-later" onClick={snooze}>
                        {t('passwordReminder.later')}
                    </button>
                    <button type="button" className="pw-reminder-change" onClick={handleChangeNow}>
                        {t('passwordReminder.changeNow')}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PasswordReminderModal;

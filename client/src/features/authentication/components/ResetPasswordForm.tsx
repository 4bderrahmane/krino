import React, {useState, type FormEvent, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import '@/features/authentication/styles/LoginForm.css';
import '@/features/authentication/styles/PasswordReset.css';
import {resetPassword} from '@/features/authentication/services/AuthenticationService.ts';
import {getServerErrorCode, resolveServerError} from '@/shared/services/errors.ts';
import LanguageSwitcher from '@/shared/components/LanguageSwitcher.tsx';
import ThemeToggle from '@/shared/components/ThemeToggle.tsx';
import BrandLogo from '@/shared/components/BrandLogo.tsx';

const MIN_PASSWORD_LENGTH = 8;
// Seconds before we send the user to login after a successful reset.
const REDIRECT_DELAY_MS = 3500;

const ButtonSpinner: React.FC = () => (
    <svg className="spinner-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
        <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
);

const LockIcon: React.FC = () => (
    <span className="input-icon" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="#9ca3af">
            <path d="M17 8h-1V6a4 4 0 10-8 0v2H7a2 2 0 00-2 2v8a2 2 0 002 2h10a2 2 0 002-2v-8a2 2 0 00-2-2zm-6 0V6a2 2 0 114 0v2h-4z"/>
        </svg>
    </span>
);

const ResetPasswordForm: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token') ?? '';

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [succeeded, setSucceeded] = useState(false);
    // A missing token means the link was mistyped/truncated — treat it as invalid up front.
    const [invalidLink, setInvalidLink] = useState(!token);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    useEffect(() => {
        setErrorMessage(null);
    }, [i18n.language]);

    // Once the reset succeeds, take the user to login automatically (they can also click through).
    useEffect(() => {
        if (!succeeded) return;
        const timer = setTimeout(() => navigate('/login'), REDIRECT_DELAY_MS);
        return () => clearTimeout(timer);
    }, [succeeded, navigate]);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            setErrorMessage(t('auth.validation.password'));
            return;
        }
        if (newPassword !== confirmPassword) {
            setErrorMessage(t('auth.validation.passwordMismatch'));
            return;
        }

        setLoading(true);
        try {
            await resetPassword({token, newPassword, confirmNewPassword: confirmPassword});
            setSucceeded(true);
        } catch (err: unknown) {
            console.error('Password reset failed:', err);
            // A bad/expired/used token gets its own dead-end screen with a way to start over.
            if (getServerErrorCode(err) === 'INVALID_TOKEN') {
                setInvalidLink(true);
            } else {
                setErrorMessage(resolveServerError(t, err, {context: 'resetPassword'}));
            }
        } finally {
            setLoading(false);
        }
    };

    const renderBody = () => {
        if (invalidLink) {
            return (
                <div className="auth-panel">
                    <h1 className="auth-title">{t('auth.passwordReset.invalidLinkTitle')}</h1>
                    <p className="auth-subtitle">{t('auth.passwordReset.invalidLinkMessage')}</p>
                    <Link to="/forgot-password" className="login-button link-button">
                        {t('auth.passwordReset.requestNewLink')}
                    </Link>
                </div>
            );
        }

        if (succeeded) {
            return (
                <div className="auth-panel">
                    <span className="auth-success-icon" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M20 6 9 17l-5-5"/>
                        </svg>
                    </span>
                    <h1 className="auth-title">{t('auth.passwordReset.successTitle')}</h1>
                    <p className="auth-subtitle">{t('auth.passwordReset.successMessage')}</p>
                    <Link to="/login" className="login-button link-button">
                        {t('auth.passwordReset.goToLogin')}
                    </Link>
                </div>
            );
        }

        return (
            <>
                <div className="auth-panel">
                    <h1 className="auth-title">{t('auth.passwordReset.resetTitle')}</h1>
                    <p className="auth-subtitle">{t('auth.passwordReset.resetSubtitle')}</p>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="input-wrapper">
                        <LockIcon/>
                        <input
                            id="newPassword"
                            name="newPassword"
                            type="password"
                            value={newPassword}
                            onChange={(e) => {
                                setNewPassword(e.target.value);
                                if (errorMessage) setErrorMessage(null);
                            }}
                            required
                            autoComplete="new-password"
                            className="form-input with-icon"
                            placeholder={t('auth.passwordReset.newPassword')}
                        />
                    </div>

                    <div className="input-wrapper">
                        <LockIcon/>
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => {
                                setConfirmPassword(e.target.value);
                                if (errorMessage) setErrorMessage(null);
                            }}
                            required
                            autoComplete="new-password"
                            className="form-input with-icon"
                            placeholder={t('auth.passwordReset.confirmPassword')}
                        />
                    </div>

                    {errorMessage && <div className="error-message">{errorMessage}</div>}

                    <button type="submit" disabled={loading} className="login-button">
                        {loading ? <ButtonSpinner/> : t('auth.passwordReset.resetButton')}
                    </button>
                </form>

                <p className="signup-cta">
                    <Link to="/login">{t('auth.passwordReset.backToLogin')}</Link>
                </p>
            </>
        );
    };

    return (
        <>
            <div className="language-switcher-fixed">
                <ThemeToggle/>
                <LanguageSwitcher/>
            </div>
            <div className="login-page-container white-bg">
                <div className="welcome-block">
                    <BrandLogo variant="auth"/>
                </div>
                {renderBody()}
            </div>
        </>
    );
};

export default ResetPasswordForm;

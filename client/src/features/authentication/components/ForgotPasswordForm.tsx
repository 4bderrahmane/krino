import React, {useState, type FormEvent, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {Link} from 'react-router-dom';
import '@/features/authentication/styles/LoginForm.css';
import '@/features/authentication/styles/PasswordReset.css';
import {requestPasswordReset} from '@/features/authentication/services/AuthenticationService.ts';
import {resolveServerError} from '@/shared/services/errors.ts';
import LanguageSwitcher from '@/shared/components/LanguageSwitcher.tsx';
import BrandLogo from '@/shared/components/BrandLogo.tsx';

const ButtonSpinner: React.FC = () => (
    <svg className="spinner-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
        <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
);

const ForgotPasswordForm: React.FC = () => {
    const {t, i18n} = useTranslation();

    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Drop the (already localized) error when the language changes.
    useEffect(() => {
        setErrorMessage(null);
    }, [i18n.language]);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setErrorMessage(null);

        try {
            await requestPasswordReset(email.trim());
            // The API responds the same whether or not the email exists, so we always show the
            // neutral confirmation and never reveal whether an account is registered.
            setSubmitted(true);
        } catch (err: unknown) {
            console.error('Password reset request failed:', err);
            setErrorMessage(resolveServerError(t, err));
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="language-switcher-fixed">
                <LanguageSwitcher/>
            </div>
            <div className="login-page-container white-bg">
                <div className="welcome-block">
                    <BrandLogo variant="auth"/>
                </div>

                {submitted ? (
                    <div className="auth-panel">
                        <span className="auth-success-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24"
                                 fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                 strokeLinejoin="round">
                                <path d="M22 2 11 13"/>
                                <path d="m22 2-7 20-4-9-9-4 20-7z"/>
                            </svg>
                        </span>
                        <h1 className="auth-title">{t('auth.passwordReset.sentTitle')}</h1>
                        <p className="auth-subtitle">{t('auth.passwordReset.sentMessage')}</p>
                        <Link to="/login" className="login-button link-button">
                            {t('auth.passwordReset.backToLogin')}
                        </Link>
                    </div>
                ) : (
                    <>
                        <div className="auth-panel">
                            <h1 className="auth-title">{t('auth.passwordReset.forgotTitle')}</h1>
                            <p className="auth-subtitle">{t('auth.passwordReset.forgotSubtitle')}</p>
                        </div>

                        <form onSubmit={handleSubmit} className="login-form">
                            <div className="input-wrapper">
                                <span className="input-icon" aria-hidden="true">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24"
                                         fill="#9ca3af">
                                        <path d="M12 12c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm0 2c-3.87 0-7 3.13-7 7h2a5 5 0 0110 0h2c0-3.87-3.13-7-7-7z"/>
                                    </svg>
                                </span>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={email}
                                    onChange={(e) => {
                                        setEmail(e.target.value);
                                        if (errorMessage) setErrorMessage(null);
                                    }}
                                    required
                                    className="form-input with-icon"
                                    placeholder={t('auth.email')}
                                />
                            </div>

                            {errorMessage && <div className="error-message">{errorMessage}</div>}

                            <button type="submit" disabled={loading} className="login-button">
                                {loading ? <ButtonSpinner/> : t('auth.passwordReset.sendLink')}
                            </button>
                        </form>

                        <p className="signup-cta">
                            <Link to="/login">{t('auth.passwordReset.backToLogin')}</Link>
                        </p>
                    </>
                )}
            </div>
        </>
    );
};

export default ForgotPasswordForm;

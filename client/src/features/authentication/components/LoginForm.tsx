import React, {useState, type FormEvent, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router-dom';
import type {UserLoginDTO} from '@/features/authentication/types/api.types';
import '@/features/authentication/styles/LoginForm.css';
import {login, resendVerificationEmail} from "@/features/authentication/services/AuthenticationService.ts";
import {getServerErrorCode, resolveServerError} from "@/shared/services/errors.ts";
import {Link} from "react-router-dom";
import LanguageSwitcher from "@/shared/components/LanguageSwitcher.tsx";
import ThemeToggle from '@/shared/components/ThemeToggle.tsx';
import {useAuth} from "@/shared/hooks/useAuth.ts";
import BrandLogo from "@/shared/components/BrandLogo.tsx";

const LoginForm: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const {login: authLogin} = useAuth();

    const [credentials, setCredentials] = useState<UserLoginDTO>({
        email: '',
        password: '',
    });

    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    // Set when login is blocked because the email isn't verified yet; offers a resend action.
    const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);
    const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle');

    // Clear the (already localized) error when the user switches language so we
    // never show a message left over in the previous language.
    useEffect(() => {
        setErrorMessage(null);
    }, [i18n.language]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setCredentials((prev) => ({
            ...prev,
            [name]: value,
        }));
        if (errorMessage) {
            setErrorMessage(null);
        }
        setUnverifiedEmail(null);
        setResendState('idle');
    };

    const handleResendVerification = async () => {
        if (!unverifiedEmail || resendState === 'sending') return;
        setResendState('sending');
        try {
            await resendVerificationEmail(unverifiedEmail);
            setResendState('sent');
        } catch (err: unknown) {
            console.error('Resending the verification email failed:', err);
            setResendState('idle');
            setErrorMessage(resolveServerError(t, err, {context: 'verifyEmail'}));
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setErrorMessage(null);

        try {
            const data = await login(credentials);
            authLogin(data.user);

            // Navigate immediately to dashboard where toast will appear
            navigate('/dashboard');
        } catch (err: unknown) {
            console.error('Login failed:', err);
            setErrorMessage(resolveServerError(t, err, {context: 'login'}));
            // Correct credentials but unverified email: surface a one-click resend.
            if (getServerErrorCode(err) === 'EMAIL_NOT_VERIFIED') {
                setUnverifiedEmail(credentials.email.trim());
                setResendState('idle');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = () => {
        navigate('/forgot-password');
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
                <form onSubmit={handleSubmit} className="login-form">
                    <div className="input-wrapper">
                        <span className="input-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24"
                                 fill="#9ca3af">
                                <path
                                    d="M12 12c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm0 2c-3.87 0-7 3.13-7 7h2a5 5 0 0110 0h2c0-3.87-3.13-7-7-7z"/>
                            </svg>
                        </span>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={credentials.email}
                            onChange={handleChange}
                            required
                            className="form-input with-icon"
                            placeholder={t('auth.email')}
                        />
                    </div>

                    <div className="input-wrapper">
                        <span className="input-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24"
                                 fill="#9ca3af">
                                <path
                                    d="M17 8h-1V6a4 4 0 10-8 0v2H7a2 2 0 00-2 2v8a2 2 0 002 2h10a2 2 0 002-2v-8a2 2 0 00-2-2zm-6 0V6a2 2 0 114 0v2h-4z"/>
                            </svg>
                        </span>
                        <input
                            id="password"
                            name="password"
                            type="password"
                            value={credentials.password}
                            onChange={handleChange}
                            required
                            className="form-input with-icon"
                            placeholder={t('auth.password')}
                        />
                    </div>

                    {errorMessage && <div className="error-message">{errorMessage}</div>}

                    {unverifiedEmail && (
                        resendState === 'sent' ? (
                            <p className="resend-verification-confirmation" role="status">
                                {t('auth.verifyEmail.resendSentMessage')}
                            </p>
                        ) : (
                            <button
                                type="button"
                                className="forgot-password-link resend-verification-button"
                                onClick={handleResendVerification}
                                disabled={resendState === 'sending'}
                            >
                                {resendState === 'sending'
                                    ? t('app.loading')
                                    : t('auth.verifyEmail.resendButton')}
                            </button>
                        )
                    )}

                    <button type="submit" disabled={loading} className="login-button">
                        {loading ? (
                            <svg
                                className="spinner-icon"
                                xmlns="http://www.w3.org/2000/svg"
                                fill="none"
                                viewBox="0 0 24 24"
                            >
                                <circle
                                    className="opacity-25"
                                    cx="12"
                                    cy="12"
                                    r="10"
                                    stroke="currentColor"
                                    strokeWidth="4"
                                ></circle>
                                <path
                                    className="opacity-75"
                                    fill="currentColor"
                                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                ></path>
                            </svg>
                        ) : (
                            t('auth.login')
                        )}
                    </button>
                </form>

                <a
                    href="#"
                    onClick={(e) => {
                        e.preventDefault();
                        handleForgotPassword();
                    }}
                    className="forgot-password-link"
                >
                    {t('auth.forgotPassword')}
                </a>

                <p className="signup-cta">
                    {t('auth.noAccount')}{' '}
                    <Link to="/register">{t('auth.signUp')}</Link>
                </p>
            </div>
        </>
    );
};

export default LoginForm;

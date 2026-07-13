import React, {useState, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import '@/features/authentication/styles/LoginForm.css';
import '@/features/authentication/styles/PasswordReset.css';
import {verifyEmail, resendVerificationEmail} from '@/features/authentication/services/AuthenticationService.ts';
import {getServerErrorCode, resolveServerError} from '@/shared/services/errors.ts';
import LanguageSwitcher from '@/shared/components/LanguageSwitcher.tsx';
import ThemeToggle from '@/shared/components/ThemeToggle.tsx';
import BrandLogo from '@/shared/components/BrandLogo.tsx';

// Seconds before we send the user to login after a successful verification.
const REDIRECT_DELAY_MS = 3500;

const ButtonSpinner: React.FC = () => (
    <svg className="spinner-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
        <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
);

const VerifyEmailPage: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token') ?? '';

    const [loading, setLoading] = useState(false);
    const [succeeded, setSucceeded] = useState(false);
    // A missing token means the link was mistyped/truncated — treat it as invalid up front.
    const [invalidLink, setInvalidLink] = useState(!token);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Resend form shown on the invalid-link screen (the old link may simply have expired).
    const [resendEmail, setResendEmail] = useState('');
    const [resendLoading, setResendLoading] = useState(false);
    const [resendSent, setResendSent] = useState(false);

    useEffect(() => {
        setErrorMessage(null);
    }, [i18n.language]);

    // Once verified, take the user to login automatically (they can also click through).
    useEffect(() => {
        if (!succeeded) return;
        const timer = setTimeout(() => navigate('/login'), REDIRECT_DELAY_MS);
        return () => clearTimeout(timer);
    }, [succeeded, navigate]);

    // Verification is an explicit click, not an automatic effect: the token is single-use,
    // so firing it from a mount effect would burn it on double-mounts or link prefetches.
    const handleVerify = async () => {
        setErrorMessage(null);
        setLoading(true);
        try {
            await verifyEmail(token);
            setSucceeded(true);
        } catch (err: unknown) {
            console.error('Email verification failed:', err);
            // A bad/expired/used token gets its own dead-end screen with a way to start over.
            if (getServerErrorCode(err) === 'INVALID_TOKEN') {
                setInvalidLink(true);
            } else {
                setErrorMessage(resolveServerError(t, err, {context: 'verifyEmail'}));
            }
        } finally {
            setLoading(false);
        }
    };

    const handleResend = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setResendLoading(true);
        try {
            await resendVerificationEmail(resendEmail.trim());
            setResendSent(true);
        } catch (err: unknown) {
            console.error('Resending the verification email failed:', err);
            setErrorMessage(resolveServerError(t, err, {context: 'verifyEmail'}));
        } finally {
            setResendLoading(false);
        }
    };

    const renderBody = () => {
        if (succeeded) {
            return (
                <div className="auth-panel">
                    <span className="auth-success-icon" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M20 6 9 17l-5-5"/>
                        </svg>
                    </span>
                    <h1 className="auth-title">{t('auth.verifyEmail.successTitle')}</h1>
                    <p className="auth-subtitle">{t('auth.verifyEmail.successMessage')}</p>
                    <Link to="/login" className="login-button link-button">
                        {t('auth.verifyEmail.goToLogin')}
                    </Link>
                </div>
            );
        }

        if (invalidLink) {
            if (resendSent) {
                return (
                    <div className="auth-panel">
                        <h1 className="auth-title">{t('auth.verifyEmail.resendSentTitle')}</h1>
                        <p className="auth-subtitle">{t('auth.verifyEmail.resendSentMessage')}</p>
                        <Link to="/login" className="login-button link-button">
                            {t('auth.verifyEmail.backToLogin')}
                        </Link>
                    </div>
                );
            }

            return (
                <>
                    <div className="auth-panel">
                        <h1 className="auth-title">{t('auth.verifyEmail.invalidLinkTitle')}</h1>
                        <p className="auth-subtitle">{t('auth.verifyEmail.invalidLinkMessage')}</p>
                    </div>

                    <form onSubmit={handleResend} className="login-form">
                        <div className="input-wrapper">
                            <input
                                id="email"
                                name="email"
                                type="email"
                                value={resendEmail}
                                onChange={(e) => {
                                    setResendEmail(e.target.value);
                                    if (errorMessage) setErrorMessage(null);
                                }}
                                required
                                autoComplete="email"
                                className="form-input"
                                placeholder={t('auth.email')}
                            />
                        </div>

                        {errorMessage && <div className="error-message">{errorMessage}</div>}

                        <button type="submit" disabled={resendLoading} className="login-button">
                            {resendLoading ? <ButtonSpinner/> : t('auth.verifyEmail.resendButton')}
                        </button>
                    </form>

                    <p className="signup-cta">
                        <Link to="/login">{t('auth.verifyEmail.backToLogin')}</Link>
                    </p>
                </>
            );
        }

        return (
            <div className="auth-panel">
                <h1 className="auth-title">{t('auth.verifyEmail.title')}</h1>
                <p className="auth-subtitle">{t('auth.verifyEmail.subtitle')}</p>

                {errorMessage && <div className="error-message">{errorMessage}</div>}

                <button type="button" onClick={handleVerify} disabled={loading} className="login-button">
                    {loading ? <ButtonSpinner/> : t('auth.verifyEmail.verifyButton')}
                </button>
            </div>
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

export default VerifyEmailPage;

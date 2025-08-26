import React, {useState, type FormEvent, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router-dom';
import type {UserLoginDTO, AuthErrorCode} from '../types/api.types';
import '../styles/LoginForm.css';
import {login} from "../services/AuthService.ts";
import {Link} from "react-router-dom";
import Welcome from './Welcome.tsx';
import LanguageSwitcher from "../../../shared/components/LanguageSwitcher.tsx";
import SuccessToast from "../../../shared/components/SuccessToast.tsx";
import {useAuth} from "../../../shared/contexts/AuthContext";
import {useSuccessToast} from "../../../shared/hooks/useSuccessToast.ts";

const LoginForm: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const {login: authLogin} = useAuth();
    const {isVisible, message, showSuccess, hideSuccess} = useSuccessToast();

    const [credentials, setCredentials] = useState<UserLoginDTO>({
        email: '',
        password: '',
    });

    const [loading, setLoading] = useState(false);
    const [errorCode, setErrorCode] = useState<AuthErrorCode | null>(null);

    useEffect(() => {

        setErrorCode(null);
    }, [i18n.language]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setCredentials((prev) => ({
            ...prev,
            [name]: value,
        }));
        // Clear error when user starts typing
        if (errorCode) {
            setErrorCode(null);
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setErrorCode(null);

        try {
            const data = await login(credentials);
            console.log('User Response: ', data.user);
            console.log('JWT: ', data.token);

            // Show success toast
            showSuccess(t('auth.success.loginSuccess'));

            // Wait for toast to show before navigating
            await new Promise((resolve) => setTimeout(resolve, 2000));

            // Use AuthContext instead of localStorage
            authLogin(data.user, data.token);
            navigate('/dashboard');

        } catch (err: any) {
            console.error('Login failed:', err);

            if (err.errorCode) {
                setErrorCode(err.errorCode as AuthErrorCode);
            } else {
                setErrorCode('UNEXPECTED_ERROR');
            }
        } finally {
            setLoading(false);
        }
    };

    const getErrorMessage = (): string => {
        if (!errorCode) return '';

        const loginErrorPath = `auth.errors.login.${errorCode}`;
        const loginError = t(loginErrorPath);

        if (loginError !== loginErrorPath) {
            return loginError;
        }

        const commonErrorPath = `auth.errors.common.${errorCode}`;
        const commonError = t(commonErrorPath);

        if (commonError !== commonErrorPath) {
            return commonError;
        }

        // Final fallback
        return t('auth.errors.common.UNEXPECTED_ERROR');
    };

    const handleForgotPassword = () => {
        alert('Forgot Password clicked');
    };

    return (
        <>
            <div className="language-switcher-fixed">
                <LanguageSwitcher/>
            </div>
            <div className="login-page-container white-bg">
                <div className="welcome-block">
                    <Welcome/>
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
                            placeholder={t('auth.username')}
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

                    {errorCode && <div className="error-message">{getErrorMessage()}</div>}

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

            {/* Success Toast */}
            <SuccessToast
                isVisible={isVisible}
                message={message}
                onClose={hideSuccess}
                duration={3000}
            />
        </>
    );
};

export default LoginForm;
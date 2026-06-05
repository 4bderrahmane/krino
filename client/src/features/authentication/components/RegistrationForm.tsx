import React, {useState, type FormEvent, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate, Link} from 'react-router-dom';
import type {UserRegistrationDTO, AuthErrorCode} from '../types/api.types';
import '../styles/RegistrationForm.css';
import {register} from "../services/AuthenticationService.ts";
import LanguageSwitcher from "../../../shared/components/LanguageSwitcher.tsx";
import {useSuccessToast} from "../../../shared/hooks/useSuccessToast.ts";

const RegistrationForm: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const { showSuccessToast } = useSuccessToast();

    const [credentials, setCredentials] = useState<UserRegistrationDTO>({
        email: '',
        username: '',
        firstName: '',
        lastName: '',
        phoneNumber: '',
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
        if (errorCode) {
            setErrorCode(null);
        }
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setErrorCode(null);

        try {
            const data = await register(credentials);
            console.log('User Response: ', data);

            showSuccessToast(t('auth.success.registrationSuccess'));

            await new Promise((resolve) => setTimeout(resolve, 2000));

            navigate('/login');
        } catch (err: any) {
            console.error('registration failed:', err);

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

        const registrationErrorPath = `auth.errors.registration.${errorCode}`;
        const registrationError = t(registrationErrorPath);

        if (registrationError !== registrationErrorPath) {
            return registrationError;
        }

        const commonErrorPath = `auth.errors.common.${errorCode}`;
        const commonError = t(commonErrorPath);

        if (commonError !== commonErrorPath) {
            return commonError;
        }

        return t('auth.errors.common.UNEXPECTED_ERROR');
    };

    return (
        <>
            <div className="language-switcher-fixed">
                <LanguageSwitcher/>
            </div>
            <div className="registration-page-container">
                <form onSubmit={handleSubmit} className="registration-form">
                    <div className="form-row">
                        <div className="form-group">
                            <input
                                id="firstName"
                                name="firstName"
                                type="text"
                                value={credentials.firstName}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.firstName')}
                            />
                        </div>
                        <div className="form-group">
                            <input
                                id="lastName"
                                name="lastName"
                                type="text"
                                value={credentials.lastName}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.lastName')}
                            />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <input
                                id="username"
                                name="username"
                                type="text"
                                value={credentials.username}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.username')}
                            />
                        </div>
                        <div className="form-group">
                            <input
                                id="email"
                                name="email"
                                type="email"
                                value={credentials.email}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.email')}
                            />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <input
                                id="phoneNumber"
                                name="phoneNumber"
                                type="tel"
                                value={credentials.phoneNumber}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.phoneNumber')}
                            />
                        </div>
                        <div className="form-group">
                            <input
                                id="password"
                                name="password"
                                type="password"
                                value={credentials.password}
                                onChange={handleChange}
                                required
                                className="form-input"
                                placeholder={t('auth.password')}
                            />
                        </div>
                    </div>

                    {errorCode && <div className="error-message">{getErrorMessage()}</div>}

                    <button type="submit" disabled={loading} className="registration-button">
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
                            t('auth.signUp')
                        )}
                    </button>
                </form>

                <p className="signup-footer-cta">
                    {t('auth.haveAccount')}{' '}
                    <Link to="/login">{t('auth.signIn')}</Link>
                </p>
            </div>
        </>
    );
};

export default RegistrationForm;
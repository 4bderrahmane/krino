import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import type {UserLoginDTO, LoginComponentProps} from '../types/api.types';
import '../styles/LoginForm.css';
// import { login } from "../services/authService";

const LoginComponent: React.FC<LoginComponentProps> = ({onLoginSuccess}) => {
    const {t} = useTranslation();
    const [credentials, setCredentials] = useState<UserLoginDTO>({
        email: '',
        password: '',
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setCredentials((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            // Simulate an async API call
            await new Promise((resolve) => setTimeout(resolve, 1000));

            // Call the success callback with the credentials.
            onLoginSuccess(credentials);
        } catch (err) {
            console.error('Login failed:', err);
            setError(t('auth.loginFailed'));
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = () => {
        alert('Forgot Password clicked');
    };

    return (
        <>
            <div className="login-card">
                <h2>{t('auth.signIn')}</h2>
                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={credentials.email}
                            onChange={handleChange}
                            required
                            className="form-input"
                            placeholder={t('auth.emailOrPhone')}
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
                    </div>

                    {error && <div className="error-message">{error}</div>}

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
                            t('auth.signIn')
                        )}
                    </button>
                </form>
            </div>
            <div className="login-footer">
                <p>
                    {t('auth.noAccount')}{' '}
                    <a href="/register">{t('auth.signUp')}</a>
                </p>
            </div>
        </>
    );
};

export default LoginComponent;
import React, {useState, type FormEvent, useEffect, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate, Link} from 'react-router-dom';
import type {UserRegistrationDTO} from '@/features/authentication/types/api.types';
import '@/features/authentication/styles/RegistrationForm.css';
import {register} from "@/features/authentication/services/AuthenticationService.ts";
import LanguageSwitcher from "@/shared/components/LanguageSwitcher.tsx";
import {useSuccessToast} from "@/shared/hooks/useSuccessToast.ts";
import {resolveServerError} from "@/shared/services/errors.ts";
import BrandLogo from "@/shared/components/BrandLogo.tsx";
import {validateCvFile, type CvFileError} from "@/shared/utils/cvFile.ts";

// Mirror the backend constraints (UserRegistrationDTO + CvStorageService) so the user
// gets immediate feedback instead of a round-trip rejection.
const NAME_MIN = 2;
const NAME_MAX = 50;
const PASSWORD_MIN = 8;
const PHONE_PATTERN = /^\d{9}$/;
const CV_ERROR_KEYS: Record<CvFileError, string> = {
    required: 'auth.validation.cvRequired',
    type: 'auth.validation.cvType',
    size: 'auth.validation.cvSize',
};

type FieldName =
    | 'firstName'
    | 'lastName'
    | 'email'
    | 'phoneNumber'
    | 'password'
    | 'confirmPassword'
    | 'resume';

type FieldErrors = Partial<Record<FieldName, string>>;

const RegistrationForm: React.FC = () => {
    const {t, i18n} = useTranslation();
    const navigate = useNavigate();
    const {showSuccessToast} = useSuccessToast();

    const [credentials, setCredentials] = useState<UserRegistrationDTO>({
        email: '',
        firstName: '',
        lastName: '',
        phoneNumber: '',
        password: '',
    });
    const [confirmPassword, setConfirmPassword] = useState('');
    const [resume, setResume] = useState<File | null>(null);
    const [showPassword, setShowPassword] = useState(false);

    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // Clear the (already localized) error when the user switches language so we
    // never show a message left over in the previous language.
    useEffect(() => {
        setErrorMessage(null);
    }, [i18n.language]);

    const clearError = (field: FieldName) => {
        setFieldErrors((prev) => {
            if (!prev[field]) return prev;
            const next = {...prev};
            delete next[field];
            return next;
        });
        if (errorMessage) setErrorMessage(null);
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setCredentials((prev) => ({...prev, [name]: value}));
        clearError(name as FieldName);
    };

    // The backend stores the local 9-digit number (the +212 country code is fixed),
    // so we keep only digits in state and cap the length at 9.
    const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const digits = e.target.value.replace(/\D/g, '').slice(0, 9);
        setCredentials((prev) => ({...prev, phoneNumber: digits}));
        clearError('phoneNumber');
    };

    const handleConfirmChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setConfirmPassword(e.target.value);
        clearError('confirmPassword');
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0] ?? null;
        setResume(file);
        clearError('resume');
    };

    const validate = (): FieldErrors => {
        const errors: FieldErrors = {};
        const firstName = credentials.firstName.trim();
        const lastName = credentials.lastName.trim();
        const phone = credentials.phoneNumber.trim();

        if (firstName.length < NAME_MIN || firstName.length > NAME_MAX) {
            errors.firstName = t('auth.validation.firstName');
        }
        if (lastName.length < NAME_MIN || lastName.length > NAME_MAX) {
            errors.lastName = t('auth.validation.lastName');
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(credentials.email.trim())) {
            errors.email = t('auth.validation.email');
        }
        if (!PHONE_PATTERN.test(phone)) {
            errors.phoneNumber = t('auth.validation.phone');
        }
        if (credentials.password.length < PASSWORD_MIN) {
            errors.password = t('auth.validation.password');
        }
        if (confirmPassword !== credentials.password) {
            errors.confirmPassword = t('auth.validation.passwordMismatch');
        }
        const cvError = validateCvFile(resume);
        if (cvError) {
            errors.resume = t(CV_ERROR_KEYS[cvError]);
        }
        return errors;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        const errors = validate();
        if (Object.keys(errors).length > 0 || !resume) {
            setFieldErrors(errors);
            return;
        }

        setLoading(true);
        try {
            await register({...credentials, email: credentials.email.trim()}, resume);

            showSuccessToast(t('auth.success.registrationVerifyEmail'));

            await new Promise((resolve) => setTimeout(resolve, 2000));

            navigate('/login');
        } catch (err: unknown) {
            console.error('registration failed:', err);
            setErrorMessage(resolveServerError(t, err, {context: 'register'}));
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="language-switcher-fixed">
                <LanguageSwitcher/>
            </div>
            <div className="registration-page-container">
                <div className="registration-brand-block">
                    <BrandLogo variant="auth"/>
                </div>
                <form onSubmit={handleSubmit} className="registration-form" noValidate>
                    <div className="form-row">
                        <div className="form-group">
                            <input
                                id="firstName"
                                name="firstName"
                                type="text"
                                value={credentials.firstName}
                                onChange={handleChange}
                                className="form-input"
                                placeholder={t('auth.firstName')}
                                aria-invalid={!!fieldErrors.firstName}
                            />
                            {fieldErrors.firstName && <span className="field-error">{fieldErrors.firstName}</span>}
                        </div>
                        <div className="form-group">
                            <input
                                id="lastName"
                                name="lastName"
                                type="text"
                                value={credentials.lastName}
                                onChange={handleChange}
                                className="form-input"
                                placeholder={t('auth.lastName')}
                                aria-invalid={!!fieldErrors.lastName}
                            />
                            {fieldErrors.lastName && <span className="field-error">{fieldErrors.lastName}</span>}
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <input
                                id="email"
                                name="email"
                                type="email"
                                value={credentials.email}
                                onChange={handleChange}
                                className="form-input"
                                placeholder={t('auth.email')}
                                aria-invalid={!!fieldErrors.email}
                            />
                            {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
                        </div>
                        <div className="form-group">
                            <div className="phone-field">
                                <span className="phone-prefix" aria-hidden="true">+212-</span>
                                <input
                                    id="phoneNumber"
                                    name="phoneNumber"
                                    type="tel"
                                    inputMode="numeric"
                                    maxLength={9}
                                    value={credentials.phoneNumber}
                                    onChange={handlePhoneChange}
                                    className="form-input phone-input"
                                    placeholder="612345678"
                                    aria-invalid={!!fieldErrors.phoneNumber}
                                />
                            </div>
                            {fieldErrors.phoneNumber && <span className="field-error">{fieldErrors.phoneNumber}</span>}
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <div className="password-field">
                                <input
                                    id="password"
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    value={credentials.password}
                                    onChange={handleChange}
                                    className="form-input"
                                    placeholder={t('auth.password')}
                                    aria-invalid={!!fieldErrors.password}
                                />
                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() => setShowPassword((s) => !s)}
                                    aria-label={showPassword ? t('auth.hide') : t('auth.show')}
                                >
                                    {showPassword ? t('auth.hide') : t('auth.show')}
                                </button>
                            </div>
                            {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
                        </div>
                        <div className="form-group">
                            <input
                                id="confirmPassword"
                                name="confirmPassword"
                                type={showPassword ? 'text' : 'password'}
                                value={confirmPassword}
                                onChange={handleConfirmChange}
                                className="form-input"
                                placeholder={t('auth.confirmPassword')}
                                aria-invalid={!!fieldErrors.confirmPassword}
                            />
                            {fieldErrors.confirmPassword &&
                                <span className="field-error">{fieldErrors.confirmPassword}</span>}
                        </div>
                    </div>

                    <div className="form-group cv-group">
                        <input
                            ref={fileInputRef}
                            id="resume"
                            name="resume"
                            type="file"
                            accept="application/pdf"
                            onChange={handleFileChange}
                            className="cv-input-hidden"
                            aria-invalid={!!fieldErrors.resume}
                        />
                        <div className="cv-row">
                            <button
                                type="button"
                                className="cv-button"
                                onClick={() => fileInputRef.current?.click()}
                            >
                                {t('auth.cvButton')}
                            </button>
                            <span className="cv-filename">
                                {resume ? resume.name : t('auth.cvNone')}
                            </span>
                        </div>
                        <p className="cv-hint">{t('auth.cvHint')}</p>
                        {fieldErrors.resume && <span className="field-error">{fieldErrors.resume}</span>}
                    </div>

                    {errorMessage && <div className="error-message">{errorMessage}</div>}

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

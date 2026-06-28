import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {Navigate} from 'react-router-dom';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {resolveServerError} from '@/shared/services/errors';
import {createStaff} from '@/features/administration/services/AdminService.ts';
import type {StaffCreateRequest, StaffCreationResponse, StaffRole} from '@/features/administration/types/admin.types.ts';
import '@/features/administration/styles/Administration.css';

// Mirror the backend constraints (StaffCreateDTO).
const NAME_MIN = 2;
const NAME_MAX = 50;
const PHONE_PATTERN = /^\d{9}$/;

const STAFF_ROLES: StaffRole[] = ['HR_MANAGER', 'INTERVIEWER'];

type FieldErrors = Partial<Record<'firstName' | 'lastName' | 'email' | 'phoneNumber', string>>;

const emptyForm: StaffCreateRequest = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    role: 'HR_MANAGER',
};

const StaffManagementPage: React.FC = () => {
    const {t} = useTranslation();
    const {isAdmin} = usePermissions();

    const [form, setForm] = useState<StaffCreateRequest>(emptyForm);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [created, setCreated] = useState<StaffCreationResponse | null>(null);
    const [copied, setCopied] = useState(false);

    // Non-admins never reach the endpoint anyway (403), but redirect for a clean UX.
    if (!isAdmin) {
        return <Navigate to="/dashboard" replace/>;
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const {name, value} = e.target;
        setForm((prev) => ({...prev, [name]: value}));
        setFieldErrors((prev) => ({...prev, [name]: undefined}));
        if (errorMessage) setErrorMessage(null);
    };

    const validate = (): FieldErrors => {
        const errors: FieldErrors = {};
        const firstName = form.firstName.trim();
        const lastName = form.lastName.trim();
        const phone = form.phoneNumber?.trim() ?? '';

        if (firstName.length < NAME_MIN || firstName.length > NAME_MAX) {
            errors.firstName = t('admin.validation.firstName');
        }
        if (lastName.length < NAME_MIN || lastName.length > NAME_MAX) {
            errors.lastName = t('admin.validation.lastName');
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
            errors.email = t('admin.validation.email');
        }
        if (phone && !PHONE_PATTERN.test(phone)) {
            errors.phoneNumber = t('admin.validation.phone');
        }
        return errors;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);
        setCreated(null);

        const errors = validate();
        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            return;
        }

        setLoading(true);
        try {
            const payload: StaffCreateRequest = {
                firstName: form.firstName.trim(),
                lastName: form.lastName.trim(),
                email: form.email.trim(),
                role: form.role,
                ...(form.phoneNumber?.trim() ? {phoneNumber: form.phoneNumber.trim()} : {}),
            };
            const result = await createStaff(payload);
            setCreated(result);
            setForm(emptyForm);
            setCopied(false);
        } catch (err: unknown) {
            console.error('staff creation failed:', err);
            setErrorMessage(resolveServerError(t, err, {context: 'register'}));
        } finally {
            setLoading(false);
        }
    };

    const handleCopyPassword = async () => {
        if (!created) return;
        try {
            await navigator.clipboard.writeText(created.initialPassword);
            setCopied(true);
        } catch {
            // Clipboard may be unavailable (e.g. non-secure context); the password is shown anyway.
            setCopied(false);
        }
    };

    return (
        <div className="admin-staff-container">
            <header className="admin-staff-header">
                <h1 className="admin-staff-title">{t('admin.title')}</h1>
                <p className="admin-staff-subtitle">{t('admin.subtitle')}</p>
            </header>

            {created && (
                <div className="admin-credentials" role="status">
                    <p className="admin-credentials-title">
                        {t('admin.created', {
                            name: `${created.user.firstName} ${created.user.lastName}`,
                        })}
                    </p>
                    <div className="admin-credentials-row">
                        <span className="admin-credentials-label">{t('admin.fields.email')}</span>
                        <span className="admin-credentials-value">{created.user.email}</span>
                    </div>
                    <div className="admin-credentials-row">
                        <span className="admin-credentials-label">{t('admin.initialPassword')}</span>
                        <code className="admin-credentials-password">{created.initialPassword}</code>
                        <button type="button" className="admin-copy-button" onClick={handleCopyPassword}>
                            {copied ? t('admin.copied') : t('admin.copy')}
                        </button>
                    </div>
                    <p className="admin-credentials-hint">{t('admin.passwordHint')}</p>
                </div>
            )}

            <form className="admin-staff-form" onSubmit={handleSubmit} noValidate>
                <div className="admin-field">
                    <label className="admin-label" htmlFor="firstName">{t('admin.fields.firstName')}</label>
                    <input
                        id="firstName"
                        name="firstName"
                        type="text"
                        className="admin-input"
                        value={form.firstName}
                        onChange={handleChange}
                        aria-invalid={!!fieldErrors.firstName}
                    />
                    {fieldErrors.firstName && <span className="admin-field-error">{fieldErrors.firstName}</span>}
                </div>

                <div className="admin-field">
                    <label className="admin-label" htmlFor="lastName">{t('admin.fields.lastName')}</label>
                    <input
                        id="lastName"
                        name="lastName"
                        type="text"
                        className="admin-input"
                        value={form.lastName}
                        onChange={handleChange}
                        aria-invalid={!!fieldErrors.lastName}
                    />
                    {fieldErrors.lastName && <span className="admin-field-error">{fieldErrors.lastName}</span>}
                </div>

                <div className="admin-field">
                    <label className="admin-label" htmlFor="email">{t('admin.fields.email')}</label>
                    <input
                        id="email"
                        name="email"
                        type="email"
                        className="admin-input"
                        value={form.email}
                        onChange={handleChange}
                        aria-invalid={!!fieldErrors.email}
                    />
                    {fieldErrors.email && <span className="admin-field-error">{fieldErrors.email}</span>}
                </div>

                <div className="admin-field">
                    <label className="admin-label" htmlFor="phoneNumber">
                        {t('admin.fields.phoneNumber')} <span className="admin-optional">({t('common.optional')})</span>
                    </label>
                    <input
                        id="phoneNumber"
                        name="phoneNumber"
                        type="tel"
                        className="admin-input"
                        value={form.phoneNumber}
                        onChange={handleChange}
                        aria-invalid={!!fieldErrors.phoneNumber}
                    />
                    {fieldErrors.phoneNumber && <span className="admin-field-error">{fieldErrors.phoneNumber}</span>}
                </div>

                <div className="admin-field">
                    <label className="admin-label" htmlFor="role">{t('admin.fields.role')}</label>
                    <select
                        id="role"
                        name="role"
                        className="admin-input admin-select"
                        value={form.role}
                        onChange={handleChange}
                    >
                        {STAFF_ROLES.map((role) => (
                            <option key={role} value={role}>{t(`admin.roles.${role}`)}</option>
                        ))}
                    </select>
                </div>

                {errorMessage && <div className="admin-error">{errorMessage}</div>}

                <button type="submit" className="admin-submit" disabled={loading}>
                    {loading ? t('app.loading') : t('admin.submit')}
                </button>
            </form>
        </div>
    );
};

export default StaffManagementPage;

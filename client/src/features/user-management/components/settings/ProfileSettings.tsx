import React, {useState, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {getCurrentUser, updatePartialProfile, updateFullProfile} from '../../services/UserManagementService';
import '../../styles/settings/SettingsForm.css';
import {useSuccessToast} from "../../../../shared/hooks/useSuccessToast.ts";

const ProfileSettings = () => {
    const {t} = useTranslation();
    const [form, setForm] = useState({
        username: '',
        firstName: '',
        lastName: '',
        email: '',
        phoneNumber: ''
    });
    const [initialForm, setInitialForm] = useState(form);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const {showSuccessToast} = useSuccessToast();

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const user = await getCurrentUser();
                const userData = {
                    username: user.username || '',
                    firstName: user.firstName || '',
                    lastName: user.lastName || '',
                    email: user.email || '',
                    phoneNumber: user.phoneNumber || ''
                };
                setForm(userData);
                setInitialForm(userData);
            } catch {
                setError(t('profile.fetchError'));
            }
        };

        fetchUserData();
    }, [t]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setForm(prev => ({...prev, [name]: value}));
    };
    const handleShowToast = (text: string, duration: number): void => {
        showSuccessToast(text, duration);
    };

    const handleProfileSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        const changedData: Partial<typeof form> = {};
        const formKeys = Object.keys(form) as Array<keyof typeof form>;
        let changedCount = 0;

        formKeys.forEach((key) => {
            if (form[key] !== initialForm[key]) {
                changedData[key] = form[key];
                changedCount++;
            }
        });

        if (changedCount === 0) {
            setLoading(false);
            return;
        }

        try {
            if (changedCount === formKeys.length) {
                await updateFullProfile(form);
            } else {
                await updatePartialProfile(changedData);
            }
            setInitialForm(form);
            handleShowToast(t('profile.updateSuccess'), 3000);
        } catch {
            setError(t('profile.updateError'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="settings-center-container">
            <form className="settings-card" onSubmit={handleProfileSubmit}>
                <h2>{t('settings.updateProfile')}</h2>
                <div className="form-content">
                    <div className="info-row">
                        <label className="info-label" htmlFor="username">{t('profile.username') || 'Username'}</label>
                        <input
                            className="info-value"
                            type="text"
                            id="username"
                            name="username"
                            value={form.username}
                            onChange={handleChange}
                            autoComplete="username"
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="firstName">{t('profile.firstName')}</label>
                        <input
                            className="info-value"
                            type="text"
                            id="firstName"
                            name="firstName"
                            value={form.firstName}
                            onChange={handleChange}
                            autoComplete="given-name"
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="lastName">{t('profile.lastName')}</label>
                        <input
                            className="info-value"
                            type="text"
                            id="lastName"
                            name="lastName"
                            value={form.lastName}
                            onChange={handleChange}
                            autoComplete="family-name"
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="email">{t('profile.email')}</label>
                        <input
                            className="info-value"
                            type="email"
                            id="email"
                            name="email"
                            value={form.email}
                            onChange={handleChange}
                            autoComplete="email"
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="phoneNumber">{t('profile.phoneNumber')}</label>
                        <input
                            className="info-value"
                            type="tel"
                            id="phoneNumber"
                            name="phoneNumber"
                            value={form.phoneNumber}
                            onChange={handleChange}
                            autoComplete="tel"
                        />
                    </div>
                </div>
                <div className="form-footer">
                    <button type="submit" className="settings-button" disabled={loading}>
                        {loading ? t('app.loading') : t('profile.saveChanges')}
                    </button>
                </div>
                {error && <div className="settings-error">{error}</div>}
            </form>
        </div>
    );
};

export default ProfileSettings;
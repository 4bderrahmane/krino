import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getCurrentUser, updatePartialProfile } from '../../services/UserManagementService';
// import './SettingsForms.css';
import '../../styles/settings/SettingsForm.css';

const ProfileSettings = () => {
    const { t } = useTranslation();
    const [form, setForm] = useState({
        firstName: '',
        lastName: '',
        email: '',
        phoneNumber: ''
    });
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    // Fetch user data on mount
    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const user = await getCurrentUser();
                setForm({
                    firstName: user.firstName || '',
                    lastName: user.lastName || '',
                    email: user.email || '',
                    phoneNumber: user.phoneNumber || ''
                });
            } catch (err) {
                setError(t('profile.fetchError'));
            }
        };

        fetchUserData();
    }, [t]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
    };

    const handleProfileSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');
        setError('');

        try {
            await updatePartialProfile(form);
            setSuccess(t('profile.updateSuccess'));
        } catch (err) {
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
                {success && <div className="settings-success">{success}</div>}
                {error && <div className="settings-error">{error}</div>}
            </form>
        </div>
    );
};

export default ProfileSettings;
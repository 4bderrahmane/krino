import React, {useState} from 'react';
import {useAuth} from '../../../shared/hooks/useAuth';
import {useTranslation} from 'react-i18next';
import '../styles/Profile.css';

const Settings: React.FC = () => {
    const {t} = useTranslation();
    const {user} = useAuth();

    const [form, setForm] = useState({
        firstName: user?.firstName || '',
        lastName: user?.lastName || '',
        email: user?.email || '',
        phoneNumber: user?.phoneNumber || '',
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
    });

    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setForm({...form, [e.target.name]: e.target.value});
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');
        setError('');
        // TODO: Replace with real update logic
        setTimeout(() => {
            setLoading(false);
            setSuccess(t('profile.profileUpdated'));
        }, 1000);
    };

    return (
        <div className="profile-container">
            <div className="profile-header">
                <h1 className="profile-title">{t('page.settings') || 'Settings'}</h1>
            </div>
            <form className="profile-card" onSubmit={handleSubmit}>
                <div className="profile-details">
                    <div className="profile-info">
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
                        <div className="info-row">
                            <label className="info-label"
                                   htmlFor="currentPassword">{t('profile.currentPassword')}</label>
                            <input
                                className="info-value"
                                type="password"
                                id="currentPassword"
                                name="currentPassword"
                                value={form.currentPassword}
                                onChange={handleChange}
                                autoComplete="current-password"
                            />
                        </div>
                        <div className="info-row">
                            <label className="info-label" htmlFor="newPassword">{t('profile.newPassword')}</label>
                            <input
                                className="info-value"
                                type="password"
                                id="newPassword"
                                name="newPassword"
                                value={form.newPassword}
                                onChange={handleChange}
                                autoComplete="new-password"
                            />
                        </div>
                        <div className="info-row">
                            <label className="info-label"
                                   htmlFor="confirmNewPassword">{t('profile.confirmNewPassword')}</label>
                            <input
                                className="info-value"
                                type="password"
                                id="confirmNewPassword"
                                name="confirmNewPassword"
                                value={form.confirmNewPassword}
                                onChange={handleChange}
                                autoComplete="new-password"
                            />
                        </div>
                    </div>
                </div>
                <div style={{marginTop: 24}}>
                    <button type="submit" className="settings-link" disabled={loading}>
                        {loading ? t('app.loading') : t('profile.saveChanges')}
                    </button>
                </div>
                {success && <div style={{color: 'green', marginTop: 12}}>{success}</div>}
                {error && <div style={{color: 'red', marginTop: 12}}>{error}</div>}
            </form>
        </div>
    );
};

export default Settings;


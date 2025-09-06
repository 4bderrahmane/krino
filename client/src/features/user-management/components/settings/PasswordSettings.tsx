import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changePassword } from '../../services/UserManagementService';
import './SettingsForms.css';

const PasswordSettings = () => {
    const { t } = useTranslation();
    const [passwordForm, setPasswordForm] = useState({
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: '',
    });

    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setPasswordForm({ ...passwordForm, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');
        setError('');

        if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
            setError(t('settings.passwordsDoNotMatch'));
            setLoading(false);
            return;
        }

        try {
            await changePassword({
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
                confirmNewPassword: passwordForm.confirmNewPassword
            });
            setSuccess(t('settings.passwordChangedSuccess'));
            setPasswordForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
        } catch (err: any) {
            setError(t('settings.passwordChangeFailed'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="settings-center-container">
            <form className="settings-card" onSubmit={handleSubmit}>
                <h2>{t('settings.changePassword')}</h2>
                <div className="form-content">
                    <div className="info-row">
                        <label className="info-label" htmlFor="currentPassword">{t('settings.currentPassword')}</label>
                        <input
                            className="info-value"
                            type="password"
                            id="currentPassword"
                            name="currentPassword"
                            value={passwordForm.currentPassword}
                            onChange={handleChange}
                            autoComplete="current-password"
                            required
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="newPassword">{t('settings.newPassword')}</label>
                        <input
                            className="info-value"
                            type="password"
                            id="newPassword"
                            name="newPassword"
                            value={passwordForm.newPassword}
                            onChange={handleChange}
                            autoComplete="new-password"
                            required
                        />
                    </div>
                    <div className="info-row">
                        <label className="info-label" htmlFor="confirmNewPassword">{t('settings.confirmNewPassword')}</label>
                        <input
                            className="info-value"
                            type="password"
                            id="confirmNewPassword"
                            name="confirmNewPassword"
                            value={passwordForm.confirmNewPassword}
                            onChange={handleChange}
                            autoComplete="new-password"
                            required
                        />
                    </div>
                </div>
                <div className="form-footer">
                    <button type="submit" className="settings-button" disabled={loading}>
                        {loading ? t('app.loading') : t('settings.changePassword')}
                    </button>
                </div>
                {success && <div className="settings-success">{success}</div>}
                {error && <div className="settings-error">{error}</div>}
            </form>
        </div>
    );
};
export default PasswordSettings;
import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router-dom';
import './SettingsForms.css';
import {deleteAccount} from "../../services/UserManagementService.ts";
import {useAuth} from "../../../../shared/hooks/useAuth";
import {useSuccessToast} from "../../../../shared/hooks/useSuccessToast";

const DeleteAccount = () => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {logout} = useAuth();
    const {showSuccessToast} = useSuccessToast();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [password, setPassword] = useState('');
    const [showConfirm, setShowConfirm] = useState(false);

    const handleDelete = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            await deleteAccount(password);
            showSuccessToast(t('settings.accountDeletedSuccess'), 3000);
            logout();
            navigate('/login');
        } catch (err: any) {
            setError(t('settings.deleteFailed'));
        } finally {
            setLoading(false);
        }
    };

    if (!showConfirm) {
        return (
            <div className="settings-center-container">
                <div className="settings-card danger-zone">
                    <h2 className="danger-header">{t('settings.deleteAccount')}</h2>
                    <p>{t('settings.deleteWarning')}</p>
                    <div className="form-footer">
                        <button className="settings-button danger-button" onClick={() => setShowConfirm(true)}>
                            {t('settings.deleteAccount')}
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="settings-center-container">
            <form className="settings-card danger-zone" onSubmit={handleDelete}>
                <h2 className="danger-header">{t('settings.confirmDeletion')}</h2>
                <p>{t('settings.confirmDeletionWarning')}</p>
                <div className="form-content">
                    <div className="info-row">
                        <label className="info-label" htmlFor="deletePassword">{t('common.password')}</label>
                        <input
                            className="info-value"
                            type="password"
                            id="deletePassword"
                            name="deletePassword"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                    </div>
                </div>
                <div className="form-footer button-group">
                    <button type="button" className="settings-button secondary-button"
                            onClick={() => setShowConfirm(false)}>
                        {t('common.cancel')}
                    </button>
                    <button type="submit" className="settings-button danger-button" disabled={loading}>
                        {loading ? t('app.loading') : t('common.submit')}
                    </button>
                </div>
                {error && <div className="settings-error">{error}</div>}
            </form>
        </div>
    );
};
export default DeleteAccount;
import React from 'react';
import {Link} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import '@/shared/styles/NotFoundPage.css';

const NotFoundPage: React.FC = () => {
    const {t} = useTranslation();

    return (
        <>
            <div className="login-page-container white-bg">
                <div className="login-card">
                    <div className="not-found-container">
                        <h1 className="not-found-number">
                            404
                        </h1>
                        <h2 className="not-found-title">
                            {t('common.pageNotFound')}
                        </h2>
                        <p className="not-found-description">
                            {t('common.pageNotFoundDescription')}
                        </p>
                    </div>

                    <div className="not-found-actions">
                        <Link
                            to="/dashboard"
                            className="login-button not-found-primary-button"
                        >
                            {t('common.goToDashboard')}
                        </Link>

                        <Link
                            to="/login"
                            className="forgot-password-link not-found-secondary-link"
                        >
                            {t('common.goToLogin')}
                        </Link>
                    </div>
                </div>
            </div>
        </>
    );
};

export default NotFoundPage;

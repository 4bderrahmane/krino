import React from "react";
import "../styles/LoadingSpinner.css";
import {useTranslation} from 'react-i18next';

interface LoadingSpinnerProps {
    message?: string;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ message }) => {
    const {t} = useTranslation();

    return (
        <div className="spinner-container">
            <div className="spinner"></div>
            <p className="spinner-text">{message || t('app.loading')}</p>
        </div>
    );
};

export default LoadingSpinner;

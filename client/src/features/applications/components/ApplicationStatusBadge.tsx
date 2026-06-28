import React from 'react';
import {useTranslation} from 'react-i18next';
import type {ApplicationStatus} from '@/features/applications/types/application.types.ts';

interface ApplicationStatusBadgeProps {
    status: ApplicationStatus;
}

const ApplicationStatusBadge: React.FC<ApplicationStatusBadgeProps> = ({status}) => {
    const {t} = useTranslation();
    return (
        <span className={`status-badge status-${status.toLowerCase()}`}>
            {t(`applications.status.${status}`)}
        </span>
    );
};

export default ApplicationStatusBadge;

import React from 'react';
import {useTranslation} from 'react-i18next';
import type {InterviewStatus} from '@/features/interviews/types/interview.types.ts';

interface InterviewStatusBadgeProps {
    status: InterviewStatus;
}

const InterviewStatusBadge: React.FC<InterviewStatusBadgeProps> = ({status}) => {
    const {t} = useTranslation();
    return (
        <span className={`status-badge status-${status.toLowerCase()}`}>
            {t(`interviews.status.${status}`)}
        </span>
    );
};

export default InterviewStatusBadge;

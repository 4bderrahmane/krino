import React from 'react';
import {useTranslation} from 'react-i18next';
import type {OfferStatus} from '@/features/offers/types/offer.types.ts';

interface OfferStatusBadgeProps {
    status: OfferStatus;
}

const OfferStatusBadge: React.FC<OfferStatusBadgeProps> = ({status}) => {
    const {t} = useTranslation();
    return (
        <span className={`status-badge status-${status.toLowerCase()}`}>
            {t(`offers.status.${status}`)}
        </span>
    );
};

export default OfferStatusBadge;

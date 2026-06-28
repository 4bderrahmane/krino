import React from 'react';
import {useTranslation} from 'react-i18next';
import {Link} from 'react-router-dom';

// Staff-only call-to-action tile shown as the lead item in the offers grid.
// Deliberately styled as an "empty slot" (dashed, quiet) so it reads as an
// action rather than another posting, while keeping the grid rhythm intact.
const CreateOfferCard: React.FC = () => {
    const {t} = useTranslation();

    return (
        <Link to="/offers/new" className="offer-create-card">
            <span className="offer-create-plus" aria-hidden="true">+</span>
            <span className="offer-create-title">{t('offers.create.cardTitle')}</span>
            <span className="offer-create-hint">{t('offers.create.cardHint')}</span>
        </Link>
    );
};

export default CreateOfferCard;

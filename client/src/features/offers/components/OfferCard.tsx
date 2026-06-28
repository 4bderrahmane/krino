import React from 'react';
import {useTranslation} from 'react-i18next';
import {Link} from 'react-router-dom';
import type {Offer} from '@/features/offers/types/offer.types.ts';
import {formatDate, formatNumber} from '@/features/offers/utils/offerFormat.ts';
import OfferStatusBadge from '@/features/offers/components/OfferStatusBadge.tsx';
import '@/features/offers/styles/OfferCard.css';

const MAX_SKILLS = 6;

interface OfferCardProps {
    offer: Offer;
}

const OfferCard: React.FC<OfferCardProps> = ({offer}) => {
    const {t, i18n} = useTranslation();
    const locale = i18n.language;

    // Classification facts, read left-to-right: where → arrangement → type.
    // City is only added when present (fully-remote roles have none), so we
    // never print "Remote · Remote".
    const facts: string[] = [];
    if (offer.location) facts.push(t(`offers.cities.${offer.location}`));
    facts.push(t(`offers.remotePolicy.${offer.remotePolicy}`));
    facts.push(t(`offers.employmentType.${offer.employmentType}`));
    facts.push(t(`offers.contractType.${offer.contractType}`));
    if (offer.experienceLevel) facts.push(t(`offers.experienceLevel.${offer.experienceLevel}`));

    // Required skills lead; both kinds are capped so the card stays compact.
    const skills = [
        ...offer.skills.filter((s) => s.importance === 'REQUIRED'),
        ...offer.skills.filter((s) => s.importance === 'PREFERRED'),
    ];
    const shownSkills = skills.slice(0, MAX_SKILLS);
    const hiddenSkillCount = skills.length - shownSkills.length;

    const deadline = offer.applyingDeadline ? formatDate(offer.applyingDeadline, locale) : null;

    const renderSalary = () => {
        const {salaryMin: min, salaryMax: max, salaryCurrency: cur, salaryPeriod: per, salaryVisible} = offer;
        if (!salaryVisible || cur == null || (min == null && max == null)) {
            return (
                <p className="offer-card-salary offer-card-salary-muted">
                    {t('offers.card.salaryNotDisclosed')}
                </p>
            );
        }

        let amount: string;
        if (min != null && max != null) {
            amount = `${formatNumber(min, locale)} – ${formatNumber(max, locale)} ${cur}`;
        } else if (min != null) {
            amount = t('offers.card.salaryFrom', {amount: `${formatNumber(min, locale)} ${cur}`});
        } else {
            amount = t('offers.card.salaryUpTo', {amount: `${formatNumber(max as number, locale)} ${cur}`});
        }

        return (
            <p className="offer-card-salary">
                <span className="offer-card-salary-amount">{amount}</span>
                {per && <span className="offer-card-salary-period"> {t(`offers.salaryPeriod.${per}`)}</span>}
            </p>
        );
    };

    return (
        <Link to={`/offers/${offer.id}`} className={`offer-card status-spine-${offer.status.toLowerCase()}`}>
            <div className="offer-card-top">
                <span className="offer-card-eyebrow">{offer.department.name}</span>
                <OfferStatusBadge status={offer.status}/>
            </div>

            <h2 className="offer-card-title">{offer.title}</h2>

            <ul className="offer-card-facts">
                {facts.map((fact) => (
                    <li key={fact} className="offer-card-fact">{fact}</li>
                ))}
            </ul>

            {renderSalary()}

            {offer.description && (
                <p className="offer-card-description">{offer.description}</p>
            )}

            {skills.length > 0 && (
                <ul className="offer-card-skills">
                    {shownSkills.map((skill) => (
                        <li
                            key={skill.slug}
                            className={`offer-skill offer-skill-${skill.importance.toLowerCase()}`}
                            title={t(`offers.skillImportance.${skill.importance}`)}
                        >
                            {skill.name}
                        </li>
                    ))}
                    {hiddenSkillCount > 0 && (
                        <li className="offer-skill offer-skill-more">
                            {t('offers.card.skillsMore', {count: hiddenSkillCount})}
                        </li>
                    )}
                </ul>
            )}

            <div className="offer-card-footer">
                <div className="offer-card-footer-meta">
                    <span className="offer-card-footer-item">
                        {t('offers.card.openPositions', {count: offer.openPositions})}
                    </span>
                    {deadline && (
                        <span className="offer-card-footer-item">
                            {t('offers.card.applyBy', {date: deadline})}
                        </span>
                    )}
                </div>
                <span className="offer-card-cta">
                    {t('offers.card.viewDetails')}
                    <span className="offer-card-cta-arrow" aria-hidden="true">→</span>
                </span>
            </div>
        </Link>
    );
};

export default OfferCard;

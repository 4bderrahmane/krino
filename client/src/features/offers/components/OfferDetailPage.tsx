import React, {useState, type FormEvent, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useParams} from 'react-router-dom';
import {useOffer} from '@/features/offers/hooks/useOffers.ts';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {
    applyWithBaseCv,
    createApplication,
    uploadApplicationResume,
} from '@/features/applications/services/ApplicationService.ts';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import {formatDate, formatNumber} from '@/features/offers/utils/offerFormat.ts';
import {validateCvFile, type CvFileError} from '@/shared/utils/cvFile.ts';
import '@/features/offers/styles/Offers.css';

const CV_ERROR_KEYS: Record<CvFileError, string> = {
    required: 'offers.apply.cvRequired',
    type: 'offers.apply.cvType',
    size: 'offers.apply.cvSize',
};

type CvChoice = 'base' | 'new';

const OfferDetailPage: React.FC = () => {
    const {t, i18n} = useTranslation();
    const locale = i18n.language;
    const {id} = useParams<{id: string}>();
    const {user} = useAuth();
    const {showSuccessToast} = useSuccessToast();

    const {data: offer, isLoading, isError, refetch} = useOffer(id);

    const {isCandidate} = usePermissions();
    const baseCvName = user?.resumeFilename ?? null;

    const [cvChoice, setCvChoice] = useState<CvChoice>(baseCvName ? 'base' : 'new');
    const [file, setFile] = useState<File | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [applied, setApplied] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError || !offer) {
        return (
            <div className="offer-detail-container">
                <div className="offers-state offers-error">
                    <p>{t('offers.loadError')}</p>
                    <button className="offers-retry" onClick={() => refetch()}>{t('common.tryAgain')}</button>
                </div>
                <Link className="offer-back" to="/offers">{t('offers.detail.back')}</Link>
            </div>
        );
    }

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFile(e.target.files?.[0] ?? null);
        setErrorMessage(null);
    };

    const handleApply = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        if (cvChoice === 'new') {
            const cvError = validateCvFile(file);
            if (cvError) {
                setErrorMessage(t(CV_ERROR_KEYS[cvError]));
                return;
            }
        }

        setSubmitting(true);
        try {
            // Two steps: create the application, then attach the chosen CV to it.
            const application = await createApplication(offer.id);
            if (cvChoice === 'base') {
                await applyWithBaseCv(application.id);
            } else if (file) {
                await uploadApplicationResume(application.id, file);
            }
            setApplied(true);
            showSuccessToast(t('offers.apply.success'));
        } catch (err: unknown) {
            console.error('apply failed:', err);
            setErrorMessage(resolveServerError(t, err, {context: 'application'}));
        } finally {
            setSubmitting(false);
        }
    };

    // Salary, mirroring the offer card: only shown when disclosed.
    const salaryText = (): string => {
        const {salaryMin: min, salaryMax: max, salaryCurrency: cur, salaryPeriod: per, salaryVisible} = offer;
        if (!salaryVisible || cur == null || (min == null && max == null)) {
            return t('offers.card.salaryNotDisclosed');
        }
        let amount: string;
        if (min != null && max != null) {
            amount = `${formatNumber(min, locale)} – ${formatNumber(max, locale)} ${cur}`;
        } else if (min != null) {
            amount = t('offers.card.salaryFrom', {amount: `${formatNumber(min, locale)} ${cur}`});
        } else {
            amount = t('offers.card.salaryUpTo', {amount: `${formatNumber(max as number, locale)} ${cur}`});
        }
        return per ? `${amount} ${t(`offers.salaryPeriod.${per}`)}` : amount;
    };

    // Label/value rows for the overview block. Null entries are dropped.
    const overview: ({label: string; value: string} | null)[] = [
        {label: t('offers.detail.status'), value: t(`offers.status.${offer.status}`)},
        offer.location
            ? {label: t('offers.detail.location'), value: t(`offers.cities.${offer.location}`)}
            : null,
        {label: t('offers.detail.workArrangement'), value: t(`offers.remotePolicy.${offer.remotePolicy}`)},
        {label: t('offers.detail.employmentType'), value: t(`offers.employmentType.${offer.employmentType}`)},
        {label: t('offers.detail.contractType'), value: t(`offers.contractType.${offer.contractType}`)},
        offer.experienceLevel
            ? {label: t('offers.detail.experienceLevel'), value: t(`offers.experienceLevel.${offer.experienceLevel}`)}
            : null,
        {label: t('offers.detail.openPositions'), value: formatNumber(offer.openPositions, locale)},
        offer.applyingDeadline
            ? {label: t('offers.detail.deadline'), value: formatDate(offer.applyingDeadline, locale)}
            : null,
        {label: t('offers.detail.salary'), value: salaryText()},
    ];
    const overviewRows = overview.filter((row): row is {label: string; value: string} => row !== null);

    // Required skills first, then preferred (same ordering as the card).
    const skills = [
        ...offer.skills.filter((s) => s.importance === 'REQUIRED'),
        ...offer.skills.filter((s) => s.importance === 'PREFERRED'),
    ];

    return (
        <div className="offer-detail-container">
            <Link className="offer-back" to="/offers">{t('offers.detail.back')}</Link>

            <article className="offer-detail-card">
                <header className="offer-detail-header">
                    <span className="offer-department">{offer.department.name}</span>
                    <h1 className="offer-detail-title">{offer.title}</h1>
                </header>

                <section className="offer-detail-section">
                    <h2 className="offer-detail-heading">{t('offers.detail.overview')}</h2>
                    <dl className="offer-detail-overview">
                        {overviewRows.map((row) => (
                            <div key={row.label} className="offer-detail-overview-row">
                                <dt>{row.label}</dt>
                                <dd>{row.value}</dd>
                            </div>
                        ))}
                    </dl>
                </section>

                <section className="offer-detail-section">
                    <h2 className="offer-detail-heading">{t('offers.detail.description')}</h2>
                    <p className="offer-detail-description">
                        {offer.description || t('offers.detail.noDescription')}
                    </p>
                </section>

                <section className="offer-detail-section">
                    <h2 className="offer-detail-heading">{t('offers.detail.skills')}</h2>
                    {skills.length > 0 ? (
                        <ul className="offer-detail-skills">
                            {skills.map((skill) => (
                                <li
                                    key={skill.slug}
                                    className={`offer-detail-skill offer-detail-skill-${skill.importance.toLowerCase()}`}
                                    title={t(`offers.skillImportance.${skill.importance}`)}
                                >
                                    {skill.name}
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="offer-detail-description">{t('offers.detail.noSkills')}</p>
                    )}
                </section>

                {offer.department.description && (
                    <section className="offer-detail-section">
                        <h2 className="offer-detail-heading">{t('offers.detail.department')}</h2>
                        <p className="offer-detail-description">{offer.department.description}</p>
                    </section>
                )}

                {/* The apply form is candidate-only. Everyone else simply doesn't see
                    it — the offer details above are fully visible to all roles. */}
                {isCandidate && (
                    <section className="offer-detail-section">
                        <h2 className="offer-detail-heading">{t('offers.detail.applyHeading')}</h2>

                        {applied ? (
                            <div className="offer-apply-success">{t('offers.apply.submitted')}</div>
                        ) : (
                            <form className="offer-apply-form" onSubmit={handleApply} noValidate>
                            {baseCvName && (
                                <label className="offer-apply-option">
                                    <input
                                        type="radio"
                                        name="cvChoice"
                                        value="base"
                                        checked={cvChoice === 'base'}
                                        onChange={() => setCvChoice('base')}
                                    />
                                    <span>{t('offers.apply.useBaseCv', {name: baseCvName})}</span>
                                </label>
                            )}

                            <label className="offer-apply-option">
                                <input
                                    type="radio"
                                    name="cvChoice"
                                    value="new"
                                    checked={cvChoice === 'new'}
                                    onChange={() => setCvChoice('new')}
                                />
                                <span>{t('offers.apply.uploadNew')}</span>
                            </label>

                            {cvChoice === 'new' && (
                                <div className="offer-apply-upload">
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="application/pdf"
                                        onChange={handleFileChange}
                                        className="cv-input-hidden"
                                        id="apply-resume"
                                    />
                                    <button
                                        type="button"
                                        className="offer-apply-file-button"
                                        onClick={() => fileInputRef.current?.click()}
                                    >
                                        {t('offers.apply.choosePdf')}
                                    </button>
                                    <span className="offer-apply-filename">
                                        {file ? file.name : t('offers.apply.noFile')}
                                    </span>
                                </div>
                            )}

                            {errorMessage && <div className="offer-apply-error">{errorMessage}</div>}

                            <button type="submit" className="offer-apply-submit" disabled={submitting}>
                                {submitting ? t('app.loading') : t('offers.apply.submit')}
                            </button>
                        </form>
                        )}
                    </section>
                )}
            </article>
        </div>
    );
};

export default OfferDetailPage;

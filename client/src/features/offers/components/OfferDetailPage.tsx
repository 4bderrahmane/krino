import React, {useState, type FormEvent, useRef} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useParams} from 'react-router-dom';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {useOffer} from '@/features/offers/hooks/useOffers.ts';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {
    applyWithBaseCv,
    createApplication,
    getMyApplicationForJob,
    uploadApplicationResume,
} from '@/features/applications/services/ApplicationService.ts';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import OfferManagePanel from '@/features/offers/components/OfferManagePanel.tsx';
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
    const queryClient = useQueryClient();

    const {data: offer, isLoading, isError, refetch} = useOffer(id);

    const {isCandidate, isStaff} = usePermissions();
    const baseCvName = user?.resumeFilename ?? null;

    // Not seeded from baseCvName at mount: this page is public, so it renders before
    // the session probe resolves and `user` is still null on the first paint. A
    // useState initial value would freeze "no CV on file" for a candidate who has
    // one, leaving them on "upload a new CV" and refusing their submit. null means
    // "the candidate has not chosen", so the default follows the data once it lands.
    const [chosenCv, setChosenCv] = useState<CvChoice | null>(null);
    const cvChoice: CvChoice = chosenCv ?? (baseCvName ? 'base' : 'new');
    const [file, setFile] = useState<File | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [createdForOffer, setCreatedForOffer] = useState<{offerId: string; userId: string; applicationId: string} | null>(null);
    const [appliedForOffer, setAppliedForOffer] = useState<{offerId: string; userId: string} | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const applicationLookup = useQuery({
        queryKey: ['application', 'by-job', id, user?.id],
        queryFn: ({signal}) => getMyApplicationForJob(id as string, signal),
        enabled: isCandidate && !!id && !!user,
    });

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

    const existingApplication = applicationLookup.data;
    const createdApplicationId = createdForOffer?.offerId === offer.id && createdForOffer.userId === user?.id
        ? createdForOffer.applicationId : null;
    const attachmentApplicationId = createdApplicationId ?? existingApplication?.id ?? null;
    const appliedNow = appliedForOffer?.offerId === offer.id && appliedForOffer.userId === user?.id;
    const alreadyApplied = !appliedNow && !!existingApplication?.resume;
    const attachmentPending = !!attachmentApplicationId && !appliedNow && !alreadyApplied;
    const applicationCacheKey = ['application', 'by-job', offer.id, user?.id];

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFile(e.target.files?.[0] ?? null);
        setErrorMessage(null);
    };

    const handleApply = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);
        if (!user?.id || applicationLookup.isLoading || applicationLookup.isError || alreadyApplied || appliedNow) return;
        const userId = user.id;

        if (cvChoice === 'new') {
            const cvError = validateCvFile(file);
            if (cvError) {
                setErrorMessage(t(CV_ERROR_KEYS[cvError]));
                return;
            }
        }

        setSubmitting(true);
        try {
            let targetId = attachmentApplicationId;
            if (!targetId) {
                try {
                    const created = await createApplication(offer.id);
                    targetId = created.id;
                    setCreatedForOffer({offerId: offer.id, userId, applicationId: created.id});
                    queryClient.setQueryData(applicationCacheKey, created);
                    void queryClient.invalidateQueries({queryKey: ['applications']});
                } catch (createError: unknown) {
                    // The create response may be lost after the server commits. Look up
                    // the candidate's application before allowing another create attempt.
                    try {
                        const recovered = await getMyApplicationForJob(offer.id);
                        if (recovered) {
                            setCreatedForOffer({offerId: offer.id, userId, applicationId: recovered.id});
                            queryClient.setQueryData(applicationCacheKey, recovered);
                        } else {
                            setErrorMessage(resolveServerError(t, createError, {context: 'application'}));
                        }
                    } catch {
                        setErrorMessage(resolveServerError(t, createError, {context: 'application'}));
                    }
                    return;
                }
            }

            const attached = cvChoice === 'base'
                ? await applyWithBaseCv(targetId)
                : await uploadApplicationResume(targetId, file as File);
            queryClient.setQueryData(applicationCacheKey, attached);
            queryClient.setQueryData(['application', attached.id, user?.id], attached);
            void queryClient.invalidateQueries({queryKey: ['applications']});
            setAppliedForOffer({offerId: offer.id, userId});
            showSuccessToast(t('offers.apply.success'));
        } catch (error: unknown) {
            setErrorMessage(resolveServerError(t, error, {context: 'application'}));
        } finally {
            setSubmitting(false);
        }
    };

    // Salary, mirroring the offer card: only shown when a salary is set.
    const salaryText = (): string => {
        const {salaryMin: min, salaryMax: max, salaryCurrency: cur, salaryPeriod: per} = offer;
        if (cur == null || (min == null && max == null)) {
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

                {isStaff && <OfferManagePanel offer={offer}/>}

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

                        {appliedNow || alreadyApplied ? (
                            <div className="offer-apply-success">
                                <p>{t(appliedNow ? 'offers.apply.submitted' : 'offers.apply.alreadyApplied')}</p>
                                {attachmentApplicationId &&
                                    <Link to={`/applications/${attachmentApplicationId}`}>
                                        {t('offers.apply.viewApplication')}
                                    </Link>}
                            </div>
                        ) : applicationLookup.isLoading ? (
                            <p className="offer-detail-description">{t('app.loading')}</p>
                        ) : applicationLookup.isError ? (
                            <div className="offer-apply-error" role="alert">
                                {t('offers.apply.lookupError')}
                                <button type="button" onClick={() => void applicationLookup.refetch()}>
                                    {t('common.tryAgain')}
                                </button>
                            </div>
                        ) : (
                            <form className="offer-apply-form" onSubmit={handleApply} noValidate>
                            {attachmentPending && (
                                <div className="offer-apply-partial" role="status">
                                    <p>{t('offers.apply.resumeMissing')}</p>
                                    <Link to={`/applications/${attachmentApplicationId}`}>
                                        {t('offers.apply.viewApplication')}
                                    </Link>
                                </div>
                            )}
                            {baseCvName && (
                                <label className="offer-apply-option">
                                    <input
                                        type="radio"
                                        name="cvChoice"
                                        value="base"
                                        checked={cvChoice === 'base'}
                                        onChange={() => setChosenCv('base')}
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
                                    onChange={() => setChosenCv('new')}
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
                                {submitting ? t('app.loading') : t(attachmentPending ? 'offers.apply.retryAttachment' : 'offers.apply.submit')}
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

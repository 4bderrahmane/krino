import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, Navigate, useNavigate, useParams} from 'react-router-dom';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useOffer, useUpdateOffer} from '@/features/offers/hooks/useOffers.ts';
import {useAllDepartments} from '@/features/departments/hooks/useDepartments.ts';
import {canEdit} from '@/features/offers/utils/offerTransitions.ts';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import OfferFormFields from '@/features/offers/components/OfferFormFields.tsx';
import {
    formToEditInput,
    offerToFormState,
    validateOfferForm,
    type FormState,
    type SkillRow,
} from '@/features/offers/utils/offerForm.ts';
import '@/features/offers/styles/CreateOffer.css';

const EditOfferPage: React.FC = () => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {id} = useParams<{id: string}>();
    const {isStaff} = usePermissions();
    const {showSuccessToast} = useSuccessToast();

    const {data: offer, isLoading, isError, refetch} = useOffer(id);
    const {data: departments, isLoading: departmentsLoading} = useAllDepartments();
    const updateOffer = useUpdateOffer();

    // Seeded from the offer once it loads; null until then.
    const [form, setForm] = useState<FormState | null>(null);
    const [skills, setSkills] = useState<SkillRow[]>([]);
    const [seededId, setSeededId] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

    // Only ADMIN / HR_MANAGER may edit offers (backend: CAN_UPDATE_JOB).
    if (!isStaff) {
        return <Navigate to="/offers" replace/>;
    }

    // Seed the form state from the loaded offer exactly once (guard against the
    // render-phase setState looping by keying on the offer id).
    if (offer && seededId !== offer.id) {
        const seed = offerToFormState(offer);
        setForm(seed.form);
        setSkills(seed.skills);
        setSeededId(offer.id);
    }

    if (isLoading || (offer && !form)) {
        return <LoadingSpinner/>;
    }

    if (isError || !offer || !form) {
        return (
            <div className="offer-form-container">
                <div className="offers-state offers-error">
                    <p>{t('offers.loadError')}</p>
                    <button className="offers-retry" onClick={() => refetch()}>{t('common.tryAgain')}</button>
                </div>
                <Link className="offer-back" to="/offers">{t('offers.detail.back')}</Link>
            </div>
        );
    }

    // An archived posting can't be modified; send the user back to its detail page.
    if (!canEdit(offer.status)) {
        return <Navigate to={`/offers/${offer.id}`} replace/>;
    }

    const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
        setForm((prev) => (prev ? {...prev, [key]: value} : prev));

    const updateSkill = (index: number, patch: Partial<SkillRow>) =>
        setSkills((prev) => prev.map((row, i) => (i === index ? {...row, ...patch} : row)));
    const addSkill = () => setSkills((prev) => [...prev, {name: '', importance: 'REQUIRED'}]);
    const removeSkill = (index: number) =>
        setSkills((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== index)));

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        const errors = validateOfferForm(form, t);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        try {
            await updateOffer.mutateAsync({id: offer.id, input: formToEditInput(form, skills)});
            showSuccessToast(t('offers.edit.success'));
            navigate(`/offers/${offer.id}`);
        } catch (err: unknown) {
            console.error('update offer failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    return (
        <div className="offer-form-container">
            <Link className="offer-back" to={`/offers/${offer.id}`}>{t('offers.detail.back')}</Link>

            <header className="offer-form-header">
                <h1 className="offer-form-title">{t('offers.edit.title')}</h1>
                <p className="offer-form-subtitle">{t('offers.edit.subtitle')}</p>
            </header>

            <form className="offer-form" onSubmit={handleSubmit} noValidate>
                <OfferFormFields
                    form={form}
                    set={set}
                    skills={skills}
                    updateSkill={updateSkill}
                    addSkill={addSkill}
                    removeSkill={removeSkill}
                    fieldErrors={fieldErrors}
                    departments={departments}
                    departmentsLoading={departmentsLoading}
                    mode="edit"
                />

                {errorMessage && <div className="offer-apply-error">{errorMessage}</div>}

                <div className="offer-form-actions">
                    <Link to={`/offers/${offer.id}`} className="offer-form-cancel">{t('common.cancel')}</Link>
                    <button type="submit" className="offer-form-submit" disabled={updateOffer.isPending}>
                        {updateOffer.isPending ? t('app.loading') : t('offers.edit.submit')}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default EditOfferPage;

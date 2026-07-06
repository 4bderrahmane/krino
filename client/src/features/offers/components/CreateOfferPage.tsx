import React, {useEffect, useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, Navigate, useNavigate} from 'react-router-dom';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useCreateOffer} from '@/features/offers/hooks/useOffers.ts';
import {useAllDepartments} from '@/features/departments/hooks/useDepartments.ts';
import {clearDraft, loadDraft, saveDraft} from '@/features/offers/utils/offerDraftStorage.ts';
import OfferFormFields from '@/features/offers/components/OfferFormFields.tsx';
import {
    formToCreateInput,
    initialSkills,
    initialState,
    validateOfferForm,
    type FormState,
    type SkillRow,
} from '@/features/offers/utils/offerForm.ts';
import '@/features/offers/styles/CreateOffer.css';

// What we persist to localStorage between visits.
interface OfferDraft {
    form: FormState;
    skills: SkillRow[];
}

// A serialised pristine form, used to tell "nothing typed yet" from a real
// draft — so we never resurrect an empty form as a "restored draft".
const PRISTINE = JSON.stringify({form: initialState, skills: initialSkills});
const isPristine = (form: FormState, skills: SkillRow[]): boolean =>
    JSON.stringify({form, skills}) === PRISTINE;

const CreateOfferPage: React.FC = () => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {user} = useAuth();
    const {isStaff} = usePermissions();
    const {showSuccessToast} = useSuccessToast();
    const {data: departments, isLoading: departmentsLoading} = useAllDepartments();
    const createOffer = useCreateOffer();

    // Drafts are scoped per user so each admin/HR keeps their own on this browser.
    const draftKey = user?.id ? `krino:offer-draft:${user.id}` : null;

    // Read any saved draft exactly once, at mount, to seed the form.
    const [restored] = useState(() => loadDraft<OfferDraft>(draftKey));
    const [form, setForm] = useState<FormState>(() => restored?.form ?? initialState);
    const [skills, setSkills] = useState<SkillRow[]>(() => restored?.skills ?? initialSkills);
    const [draftRestored, setDraftRestored] = useState(() => restored != null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

    // Autosave on every edit: store a real draft, or wipe a draft that has been
    // emptied back to pristine so it doesn't linger.
    useEffect(() => {
        if (!draftKey) return;
        if (isPristine(form, skills)) clearDraft(draftKey);
        else saveDraft<OfferDraft>(draftKey, {form, skills});
    }, [draftKey, form, skills]);

    // Only ADMIN / HR_MANAGER may create offers (backend: CAN_CREATE_JOB).
    if (!isStaff) {
        return <Navigate to="/offers" replace/>;
    }

    const discardDraft = () => {
        setForm(initialState);
        setSkills(initialSkills);
        setFieldErrors({});
        setErrorMessage(null);
        setDraftRestored(false);
        clearDraft(draftKey);
    };

    const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
        setForm((prev) => ({...prev, [key]: value}));

    const updateSkill = (index: number, patch: Partial<SkillRow>) =>
        setSkills((prev) => prev.map((row, i) => (i === index ? {...row, ...patch} : row)));
    const addSkill = () => setSkills((prev) => [...prev, {name: '', importance: 'REQUIRED'}]);
    const removeSkill = (index: number) => setSkills((prev) => prev.filter((_, i) => i !== index));

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        const errors = validateOfferForm(form, t);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        try {
            const created = await createOffer.mutateAsync(formToCreateInput(form, skills));
            clearDraft(draftKey); // the offer now lives server-side; drop the local draft
            showSuccessToast(t('offers.create.success'));
            navigate(`/offers/${created.id}`);
        } catch (err: unknown) {
            console.error('create offer failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    return (
        <div className="offer-form-container">
            <Link className="offer-back" to="/offers">{t('offers.detail.back')}</Link>

            <header className="offer-form-header">
                <h1 className="offer-form-title">{t('offers.create.title')}</h1>
                <p className="offer-form-subtitle">{t('offers.create.subtitle')}</p>
            </header>

            {draftRestored && (
                <div className="offer-draft-banner" role="status">
                    <span className="offer-draft-text">{t('offers.create.draftRestored')}</span>
                    <button type="button" className="offer-draft-discard" onClick={discardDraft}>
                        {t('offers.create.discardDraft')}
                    </button>
                </div>
            )}

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
                    mode="create"
                />

                {errorMessage && <div className="offer-apply-error">{errorMessage}</div>}

                <div className="offer-form-actions">
                    <Link to="/offers" className="offer-form-cancel">{t('common.cancel')}</Link>
                    <button type="submit" className="offer-form-submit" disabled={createOffer.isPending}>
                        {createOffer.isPending ? t('app.loading') : t('offers.create.submit')}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default CreateOfferPage;

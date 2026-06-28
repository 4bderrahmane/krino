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
import {
    CONTRACT_TYPE_ORDER,
    EMPLOYMENT_TYPE_ORDER,
    EXPERIENCE_LEVEL_ORDER,
    MOROCCAN_CITY_ORDER,
    REMOTE_POLICY_ORDER,
    SALARY_CURRENCY_ORDER,
    SALARY_PERIOD_ORDER,
    SKILL_IMPORTANCE_ORDER,
} from '@/features/offers/utils/offerEnums.ts';
import type {
    ContractType,
    CreateOfferInput,
    EmploymentType,
    ExperienceLevel,
    MoroccanCity,
    RemotePolicy,
    SalaryCurrency,
    SalaryPeriod,
    SkillImportance,
} from '@/features/offers/types/offer.types.ts';
import '@/features/offers/styles/CreateOffer.css';

interface SkillRow {
    name: string;
    importance: SkillImportance;
}

// The form keeps everything as strings (what the inputs emit) and converts to a
// typed CreateOfferInput on submit. '' means "not set" for every optional field.
interface FormState {
    department: string;
    title: string;
    description: string;
    employmentType: EmploymentType;
    contractType: ContractType;
    experienceLevel: ExperienceLevel | '';
    minimumExperienceYears: string;
    openPositions: string;
    remotePolicy: RemotePolicy;
    location: MoroccanCity | '';
    salaryMin: string;
    salaryMax: string;
    salaryCurrency: SalaryCurrency | '';
    salaryPeriod: SalaryPeriod | '';
    salaryVisible: boolean;
    salaryNegotiable: boolean;
    applyingDeadline: string;
    plannedStartDate: string;
}

const initialState: FormState = {
    department: '',
    title: '',
    description: '',
    employmentType: 'FULL_TIME',
    contractType: 'PERMANENT',
    experienceLevel: '',
    minimumExperienceYears: '',
    openPositions: '1',
    remotePolicy: 'ON_SITE',
    location: '',
    salaryMin: '',
    salaryMax: '',
    salaryCurrency: '',
    salaryPeriod: '',
    salaryVisible: true,
    salaryNegotiable: false,
    applyingDeadline: '',
    plannedStartDate: '',
};

const initialSkills: SkillRow[] = [{name: '', importance: 'REQUIRED'}];

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

// Empty string -> null; otherwise a non-negative integer (NaN -> null).
const toIntOrNull = (value: string): number | null => {
    if (value.trim() === '') return null;
    const n = Number(value);
    return Number.isFinite(n) ? Math.trunc(n) : null;
};

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

    const validate = (): Record<string, string> => {
        const errors: Record<string, string> = {};
        if (!form.department) errors.department = t('offers.create.errors.departmentRequired');
        if (!form.title.trim()) errors.title = t('offers.create.errors.titleRequired');
        else if (form.title.trim().length > 100) errors.title = t('offers.create.errors.titleTooLong');

        if (!form.applyingDeadline) {
            errors.applyingDeadline = t('offers.create.errors.deadlineRequired');
        } else if (new Date(form.applyingDeadline).getTime() <= Date.now()) {
            errors.applyingDeadline = t('offers.create.errors.deadlineFuture');
        }

        const min = toIntOrNull(form.salaryMin);
        const max = toIntOrNull(form.salaryMax);
        if (min != null && max != null && min > max) {
            errors.salaryMax = t('offers.create.errors.salaryRange');
        }

        const positions = toIntOrNull(form.openPositions);
        if (positions == null || positions < 1) {
            errors.openPositions = t('offers.create.errors.openPositions');
        }
        return errors;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        const cleanedSkills = skills
            .filter((s) => s.name.trim() !== '')
            .map((s) => ({name: s.name.trim(), importance: s.importance}));

        const input: CreateOfferInput = {
            department: form.department,
            title: form.title.trim(),
            description: form.description.trim() || null,
            employmentType: form.employmentType,
            contractType: form.contractType,
            experienceLevel: form.experienceLevel || null,
            minimumExperienceYears: toIntOrNull(form.minimumExperienceYears),
            openPositions: toIntOrNull(form.openPositions) ?? 1,
            location: form.location || null,
            remotePolicy: form.remotePolicy,
            salaryMin: toIntOrNull(form.salaryMin),
            salaryMax: toIntOrNull(form.salaryMax),
            salaryCurrency: form.salaryCurrency || null,
            salaryPeriod: form.salaryPeriod || null,
            salaryVisible: form.salaryVisible,
            salaryNegotiable: form.salaryNegotiable,
            applyingDeadline: new Date(form.applyingDeadline).toISOString(),
            plannedStartDate: form.plannedStartDate || null,
            skills: cleanedSkills,
        };

        try {
            const created = await createOffer.mutateAsync(input);
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
                {/* Basics ---------------------------------------------------- */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.basics')}</legend>

                    <div className="offer-form-grid">
                        <label className="offer-field">
                            <span className="offer-field-label">
                                {t('offers.create.fields.department')} *
                            </span>
                            <select
                                className={`offer-input${fieldErrors.department ? ' has-error' : ''}`}
                                value={form.department}
                                onChange={(e) => set('department', e.target.value)}
                                disabled={departmentsLoading}
                            >
                                <option value="">
                                    {departmentsLoading
                                        ? t('app.loading')
                                        : t('offers.create.fields.departmentPlaceholder')}
                                </option>
                                {(departments ?? []).map((d) => (
                                    <option key={d.id} value={d.name}>{d.name}</option>
                                ))}
                            </select>
                            {fieldErrors.department && (
                                <span className="offer-field-error">{fieldErrors.department}</span>
                            )}
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.title')} *</span>
                            <input
                                type="text"
                                className={`offer-input${fieldErrors.title ? ' has-error' : ''}`}
                                value={form.title}
                                maxLength={100}
                                onChange={(e) => set('title', e.target.value)}
                                placeholder={t('offers.create.fields.titlePlaceholder')}
                            />
                            {fieldErrors.title && (
                                <span className="offer-field-error">{fieldErrors.title}</span>
                            )}
                        </label>
                    </div>

                    <label className="offer-field">
                        <span className="offer-field-label">{t('offers.create.fields.description')}</span>
                        <textarea
                            className="offer-input offer-textarea"
                            value={form.description}
                            maxLength={4000}
                            rows={5}
                            onChange={(e) => set('description', e.target.value)}
                            placeholder={t('offers.create.fields.descriptionPlaceholder')}
                        />
                    </label>
                </fieldset>

                {/* Classification -------------------------------------------- */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.classification')}</legend>

                    <div className="offer-form-grid">
                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.employmentType')} *</span>
                            <select
                                className="offer-input"
                                value={form.employmentType}
                                onChange={(e) => set('employmentType', e.target.value as EmploymentType)}
                            >
                                {EMPLOYMENT_TYPE_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.employmentType.${v}`)}</option>
                                ))}
                            </select>
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.contractType')} *</span>
                            <select
                                className="offer-input"
                                value={form.contractType}
                                onChange={(e) => set('contractType', e.target.value as ContractType)}
                            >
                                {CONTRACT_TYPE_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.contractType.${v}`)}</option>
                                ))}
                            </select>
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.experienceLevel')}</span>
                            <select
                                className="offer-input"
                                value={form.experienceLevel}
                                onChange={(e) => set('experienceLevel', e.target.value as ExperienceLevel | '')}
                            >
                                <option value="">{t('offers.create.fields.notSpecified')}</option>
                                {EXPERIENCE_LEVEL_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.experienceLevel.${v}`)}</option>
                                ))}
                            </select>
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.minimumExperienceYears')}</span>
                            <input
                                type="number"
                                min={0}
                                className="offer-input"
                                value={form.minimumExperienceYears}
                                onChange={(e) => set('minimumExperienceYears', e.target.value)}
                                placeholder="0"
                            />
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.openPositions')} *</span>
                            <input
                                type="number"
                                min={1}
                                className={`offer-input${fieldErrors.openPositions ? ' has-error' : ''}`}
                                value={form.openPositions}
                                onChange={(e) => set('openPositions', e.target.value)}
                            />
                            {fieldErrors.openPositions && (
                                <span className="offer-field-error">{fieldErrors.openPositions}</span>
                            )}
                        </label>
                    </div>
                </fieldset>

                {/* Location & arrangement ------------------------------------ */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.location')}</legend>

                    <div className="offer-form-grid">
                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.remotePolicy')} *</span>
                            <select
                                className="offer-input"
                                value={form.remotePolicy}
                                onChange={(e) => set('remotePolicy', e.target.value as RemotePolicy)}
                            >
                                {REMOTE_POLICY_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.remotePolicy.${v}`)}</option>
                                ))}
                            </select>
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.city')}</span>
                            <select
                                className="offer-input"
                                value={form.location}
                                onChange={(e) => set('location', e.target.value as MoroccanCity | '')}
                            >
                                <option value="">{t('offers.create.fields.notSpecified')}</option>
                                {MOROCCAN_CITY_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.cities.${v}`)}</option>
                                ))}
                            </select>
                        </label>
                    </div>
                </fieldset>

                {/* Compensation ---------------------------------------------- */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.compensation')}</legend>

                    <div className="offer-form-grid">
                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.salaryMin')}</span>
                            <input
                                type="number"
                                min={0}
                                className="offer-input"
                                value={form.salaryMin}
                                onChange={(e) => set('salaryMin', e.target.value)}
                            />
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.salaryMax')}</span>
                            <input
                                type="number"
                                min={0}
                                className={`offer-input${fieldErrors.salaryMax ? ' has-error' : ''}`}
                                value={form.salaryMax}
                                onChange={(e) => set('salaryMax', e.target.value)}
                            />
                            {fieldErrors.salaryMax && (
                                <span className="offer-field-error">{fieldErrors.salaryMax}</span>
                            )}
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.salaryCurrency')}</span>
                            <select
                                className="offer-input"
                                value={form.salaryCurrency}
                                onChange={(e) => set('salaryCurrency', e.target.value as SalaryCurrency | '')}
                            >
                                <option value="">{t('offers.create.fields.notSpecified')}</option>
                                {SALARY_CURRENCY_ORDER.map((v) => (
                                    <option key={v} value={v}>{v}</option>
                                ))}
                            </select>
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.salaryPeriod')}</span>
                            <select
                                className="offer-input"
                                value={form.salaryPeriod}
                                onChange={(e) => set('salaryPeriod', e.target.value as SalaryPeriod | '')}
                            >
                                <option value="">{t('offers.create.fields.notSpecified')}</option>
                                {SALARY_PERIOD_ORDER.map((v) => (
                                    <option key={v} value={v}>{t(`offers.salaryPeriod.${v}`)}</option>
                                ))}
                            </select>
                        </label>
                    </div>

                    <div className="offer-form-checks">
                        <label className="offer-check">
                            <input
                                type="checkbox"
                                checked={form.salaryVisible}
                                onChange={(e) => set('salaryVisible', e.target.checked)}
                            />
                            <span>{t('offers.create.fields.salaryVisible')}</span>
                        </label>
                        <label className="offer-check">
                            <input
                                type="checkbox"
                                checked={form.salaryNegotiable}
                                onChange={(e) => set('salaryNegotiable', e.target.checked)}
                            />
                            <span>{t('offers.create.fields.salaryNegotiable')}</span>
                        </label>
                    </div>
                </fieldset>

                {/* Timeline -------------------------------------------------- */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.timeline')}</legend>

                    <div className="offer-form-grid">
                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.applyingDeadline')} *</span>
                            <input
                                type="datetime-local"
                                className={`offer-input${fieldErrors.applyingDeadline ? ' has-error' : ''}`}
                                value={form.applyingDeadline}
                                onChange={(e) => set('applyingDeadline', e.target.value)}
                            />
                            {fieldErrors.applyingDeadline && (
                                <span className="offer-field-error">{fieldErrors.applyingDeadline}</span>
                            )}
                        </label>

                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.plannedStartDate')}</span>
                            <input
                                type="date"
                                className="offer-input"
                                value={form.plannedStartDate}
                                onChange={(e) => set('plannedStartDate', e.target.value)}
                            />
                        </label>
                    </div>
                </fieldset>

                {/* Skills ---------------------------------------------------- */}
                <fieldset className="offer-form-section">
                    <legend className="offer-form-legend">{t('offers.create.sections.skills')}</legend>
                    <p className="offer-form-hint">{t('offers.create.skillsHint')}</p>

                    <ul className="offer-skill-rows">
                        {skills.map((skill, index) => (
                            <li key={index} className="offer-skill-row">
                                <input
                                    type="text"
                                    className="offer-input offer-skill-name"
                                    value={skill.name}
                                    maxLength={100}
                                    onChange={(e) => updateSkill(index, {name: e.target.value})}
                                    placeholder={t('offers.create.fields.skillName')}
                                />
                                <select
                                    className="offer-input offer-skill-importance"
                                    value={skill.importance}
                                    onChange={(e) => updateSkill(index, {importance: e.target.value as SkillImportance})}
                                >
                                    {SKILL_IMPORTANCE_ORDER.map((v) => (
                                        <option key={v} value={v}>{t(`offers.skillImportance.${v}`)}</option>
                                    ))}
                                </select>
                                <button
                                    type="button"
                                    className="offer-skill-remove"
                                    onClick={() => removeSkill(index)}
                                    disabled={skills.length === 1}
                                    aria-label={t('offers.create.removeSkill')}
                                >
                                    ×
                                </button>
                            </li>
                        ))}
                    </ul>

                    <button
                        type="button"
                        className="offer-skill-add"
                        onClick={addSkill}
                        disabled={skills.length >= 30}
                    >
                        + {t('offers.create.addSkill')}
                    </button>
                </fieldset>

                {errorMessage && <div className="offer-apply-error">{errorMessage}</div>}

                <div className="offer-form-actions">
                    <Link to="/offers" className="offer-form-cancel">{t('common.cancel')}</Link>
                    <button
                        type="submit"
                        className="offer-form-submit"
                        disabled={createOffer.isPending}
                    >
                        {createOffer.isPending ? t('app.loading') : t('offers.create.submit')}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default CreateOfferPage;

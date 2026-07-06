import React from 'react';
import {useTranslation} from 'react-i18next';
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
import type {FormState, SkillRow} from '@/features/offers/utils/offerForm.ts';
import type {
    ContractType,
    EmploymentType,
    ExperienceLevel,
    MoroccanCity,
    RemotePolicy,
    SalaryCurrency,
    SalaryPeriod,
    SkillImportance,
} from '@/features/offers/types/offer.types.ts';
import type {Department} from '@/features/departments/types/department.types.ts';

interface OfferFormFieldsProps {
    form: FormState;
    set: <K extends keyof FormState>(key: K, value: FormState[K]) => void;
    skills: SkillRow[];
    updateSkill: (index: number, patch: Partial<SkillRow>) => void;
    addSkill: () => void;
    removeSkill: (index: number) => void;
    fieldErrors: Record<string, string>;
    departments: Department[] | undefined;
    departmentsLoading: boolean;
    // 'edit' hides the three fields the read model can't surface (so a PATCH leaves
    // them untouched); 'create' shows everything.
    mode: 'create' | 'edit';
}

// Presentational field set shared by the create and edit offer forms. It owns no
// state — the parent passes `form`/`set` and decides how to persist on submit.
const OfferFormFields: React.FC<OfferFormFieldsProps> = ({
    form,
    set,
    skills,
    updateSkill,
    addSkill,
    removeSkill,
    fieldErrors,
    departments,
    departmentsLoading,
    mode,
}) => {
    const {t} = useTranslation();
    const showHidden = mode === 'create';

    return (
        <>
            {/* Basics ---------------------------------------------------- */}
            <fieldset className="offer-form-section">
                <legend className="offer-form-legend">{t('offers.create.sections.basics')}</legend>

                <div className="offer-form-grid">
                    <label className="offer-field">
                        <span className="offer-field-label">{t('offers.create.fields.department')} *</span>
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
                        {fieldErrors.title && <span className="offer-field-error">{fieldErrors.title}</span>}
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

                    {showHidden && (
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
                    )}

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
                    {showHidden && (
                        <label className="offer-check">
                            <input
                                type="checkbox"
                                checked={form.salaryNegotiable}
                                onChange={(e) => set('salaryNegotiable', e.target.checked)}
                            />
                            <span>{t('offers.create.fields.salaryNegotiable')}</span>
                        </label>
                    )}
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

                    {showHidden && (
                        <label className="offer-field">
                            <span className="offer-field-label">{t('offers.create.fields.plannedStartDate')}</span>
                            <input
                                type="date"
                                className="offer-input"
                                value={form.plannedStartDate}
                                onChange={(e) => set('plannedStartDate', e.target.value)}
                            />
                        </label>
                    )}
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
        </>
    );
};

export default OfferFormFields;

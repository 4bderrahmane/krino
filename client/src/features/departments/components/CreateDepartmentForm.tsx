import React, {useState, type FormEvent} from 'react';
import {useTranslation} from 'react-i18next';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useCreateDepartment} from '@/features/departments/hooks/useDepartments.ts';

interface CreateDepartmentFormProps {
    onClose: () => void;
}

// Inline create form for departments — a two-field card revealed above the list.
// No draft persistence (departments are quick to fill, unlike offers).
const CreateDepartmentForm: React.FC<CreateDepartmentFormProps> = ({onClose}) => {
    const {t} = useTranslation();
    const {showSuccessToast} = useSuccessToast();
    const createDepartment = useCreateDepartment();

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [nameError, setNameError] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setErrorMessage(null);

        const trimmed = name.trim();
        if (!trimmed) {
            setNameError(t('departments.create.errors.nameRequired'));
            return;
        }
        if (trimmed.length > 100) {
            setNameError(t('departments.create.errors.nameTooLong'));
            return;
        }
        setNameError(null);

        try {
            await createDepartment.mutateAsync({
                name: trimmed,
                description: description.trim() || null,
            });
            showSuccessToast(t('departments.create.success'));
            onClose();
        } catch (err: unknown) {
            console.error('create department failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    return (
        <form className="department-form" onSubmit={handleSubmit} noValidate>
            <h2 className="department-form-title">{t('departments.create.title')}</h2>

            <label className="department-field">
                <span className="department-field-label">{t('departments.create.name')} *</span>
                <input
                    type="text"
                    className={`department-input${nameError ? ' has-error' : ''}`}
                    value={name}
                    maxLength={100}
                    autoFocus
                    onChange={(e) => setName(e.target.value)}
                    placeholder={t('departments.create.namePlaceholder')}
                />
                {nameError && <span className="department-field-error">{nameError}</span>}
            </label>

            <label className="department-field">
                <span className="department-field-label">{t('departments.create.description')}</span>
                <textarea
                    className="department-input department-textarea"
                    value={description}
                    maxLength={255}
                    rows={3}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder={t('departments.create.descriptionPlaceholder')}
                />
            </label>

            {errorMessage && <div className="department-form-error">{errorMessage}</div>}

            <div className="department-form-actions">
                <button type="button" className="department-form-cancel" onClick={onClose}>
                    {t('common.cancel')}
                </button>
                <button
                    type="submit"
                    className="department-form-submit"
                    disabled={createDepartment.isPending}
                >
                    {createDepartment.isPending ? t('app.loading') : t('departments.create.submit')}
                </button>
            </div>
        </form>
    );
};

export default CreateDepartmentForm;

import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useDepartments} from '@/features/departments/hooks/useDepartments.ts';
import CreateDepartmentForm from '@/features/departments/components/CreateDepartmentForm.tsx';
import {usePermissions} from '@/shared/hooks/usePermissions';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import '@/features/departments/styles/Departments.css';

const DepartmentsPage: React.FC = () => {
    const {t} = useTranslation();
    const {isStaff} = usePermissions();
    const [page, setPage] = useState(0);
    const [showForm, setShowForm] = useState(false);
    const {data, isLoading, isError, refetch} = useDepartments(page);

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="departments-container">
                <div className="departments-state departments-error">
                    <p>{t('departments.loadError')}</p>
                    <button className="departments-retry" onClick={() => refetch()}>
                        {t('common.tryAgain')}
                    </button>
                </div>
            </div>
        );
    }

    const departments = data?.departments ?? [];
    const meta = data?.page;
    const pageCount = Math.max(1, meta?.totalPages ?? 1);
    const total = meta?.totalElements ?? departments.length;

    return (
        <div className="departments-container">
            <header className="departments-header">
                <h1 className="departments-title">{t('departments.title')}</h1>
                <p className="departments-subtitle">{t('departments.count', {count: total})}</p>
            </header>

            {isStaff && (
                <div className="departments-toolbar">
                    {showForm ? (
                        <CreateDepartmentForm onClose={() => setShowForm(false)}/>
                    ) : (
                        <button
                            type="button"
                            className="departments-create-toggle"
                            onClick={() => setShowForm(true)}
                        >
                            + {t('departments.create.newDepartment')}
                        </button>
                    )}
                </div>
            )}

            {departments.length === 0 ? (
                <div className="departments-state departments-empty">
                    <p>{t('departments.empty')}</p>
                </div>
            ) : (
                <>
                    <ul className="departments-list">
                        {departments.map((department) => (
                            <li key={department.id} className="department-row">
                                <div className="department-row-main">
                                    <h2 className="department-row-title">{department.name}</h2>
                                    {department.description && (
                                        <p className="department-row-description">{department.description}</p>
                                    )}
                                </div>
                            </li>
                        ))}
                    </ul>

                    {pageCount > 1 && (
                        <nav className="departments-pagination" aria-label={t('departments.pagination')}>
                            <button
                                className="departments-page-button"
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                disabled={page === 0}
                            >
                                {t('common.previous')}
                            </button>
                            <span className="departments-page-indicator">
                                {t('departments.pageIndicator', {current: page + 1, total: pageCount})}
                            </span>
                            <button
                                className="departments-page-button"
                                onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
                                disabled={page >= pageCount - 1}
                            >
                                {t('common.next')}
                            </button>
                        </nav>
                    )}
                </>
            )}
        </div>
    );
};

export default DepartmentsPage;

import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Navigate} from 'react-router-dom';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useAuth} from '@/shared/hooks/useAuth';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {useSetUserApproval, useUsers} from '@/features/administration/hooks/useUsers.ts';
import LoadingSpinner from '@/shared/components/LoadingSpinner.tsx';
import type {DirectoryUser} from '@/features/administration/types/admin.types.ts';
import '@/features/administration/styles/Administration.css';

const UserManagementPage: React.FC = () => {
    const {t} = useTranslation();
    const {isStaff} = usePermissions();
    const {user: currentUser} = useAuth();
    const {showSuccessToast} = useSuccessToast();

    const {data: users, isLoading, isError, refetch} = useUsers();
    const setApproval = useSetUserApproval();

    const [pendingOnly, setPendingOnly] = useState(false);
    const [confirmingRevokeId, setConfirmingRevokeId] = useState<string | null>(null);

    // Listing users is an ADMIN/HR-only endpoint; bounce anyone else.
    if (!isStaff) {
        return <Navigate to="/dashboard" replace/>;
    }

    if (isLoading) {
        return <LoadingSpinner/>;
    }

    if (isError) {
        return (
            <div className="admin-users-container">
                <div className="admin-users-state">
                    <p>{t('admin.users.loadError')}</p>
                    <button className="admin-submit" onClick={() => refetch()}>{t('common.tryAgain')}</button>
                </div>
            </div>
        );
    }

    const all = users ?? [];
    const visible = pendingOnly ? all.filter((u) => !u.approved) : all;
    const pendingCount = all.filter((u) => !u.approved).length;

    const apply = async (target: DirectoryUser, approved: boolean) => {
        try {
            await setApproval.mutateAsync({id: target.id, approved});
            showSuccessToast(approved ? t('admin.users.approveSuccess') : t('admin.users.revokeSuccess'));
            setConfirmingRevokeId(null);
        } catch (err: unknown) {
            console.error('set approval failed:', err);
            showSuccessToast(resolveServerError(t, err));
        }
    };

    return (
        <div className="admin-users-container">
            <header className="admin-staff-header">
                <h1 className="admin-staff-title">{t('admin.users.title')}</h1>
                <p className="admin-staff-subtitle">{t('admin.users.subtitle')}</p>
            </header>

            <label className="admin-users-filter">
                <input
                    type="checkbox"
                    checked={pendingOnly}
                    onChange={(e) => setPendingOnly(e.target.checked)}
                />
                <span>{t('admin.users.pendingOnly', {count: pendingCount})}</span>
            </label>

            {visible.length === 0 ? (
                <div className="admin-users-state">
                    <p>{pendingOnly ? t('admin.users.noPending') : t('admin.users.empty')}</p>
                </div>
            ) : (
                <ul className="admin-users-list">
                    {visible.map((u) => {
                        const isSelf = currentUser?.id === u.id;
                        return (
                            <li key={u.id} className="admin-user-row">
                                <div className="admin-user-main">
                                    <div className="admin-user-name">{u.fullName}</div>
                                    <div className="admin-user-email">{u.email}</div>
                                    <div className="admin-user-roles">
                                        {u.roles.map((role) => (
                                            <span key={role} className="admin-user-role">{t(`admin.roles.${role}`)}</span>
                                        ))}
                                    </div>
                                </div>

                                <span
                                    className={`admin-user-status ${u.approved ? 'is-approved' : 'is-pending'}`}
                                >
                                    {u.approved ? t('admin.users.approved') : t('admin.users.notApproved')}
                                </span>

                                <div className="admin-user-actions">
                                    {isSelf ? (
                                        // Guard against locking yourself out of your own account.
                                        <span className="admin-user-self">{t('admin.users.you')}</span>
                                    ) : u.approved ? (
                                        confirmingRevokeId === u.id ? (
                                            <>
                                                <span className="admin-user-confirm">{t('admin.users.revokeConfirm')}</span>
                                                <button
                                                    type="button"
                                                    className="admin-user-button admin-user-danger"
                                                    disabled={setApproval.isPending}
                                                    onClick={() => apply(u, false)}
                                                >
                                                    {t('common.yes')}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="admin-user-button"
                                                    onClick={() => setConfirmingRevokeId(null)}
                                                >
                                                    {t('common.no')}
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                type="button"
                                                className="admin-user-button admin-user-danger"
                                                disabled={setApproval.isPending}
                                                onClick={() => setConfirmingRevokeId(u.id)}
                                            >
                                                {t('admin.users.revoke')}
                                            </button>
                                        )
                                    ) : (
                                        <button
                                            type="button"
                                            className="admin-user-button admin-user-primary"
                                            disabled={setApproval.isPending}
                                            onClick={() => apply(u, true)}
                                        >
                                            {t('admin.users.approve')}
                                        </button>
                                    )}
                                </div>
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};

export default UserManagementPage;

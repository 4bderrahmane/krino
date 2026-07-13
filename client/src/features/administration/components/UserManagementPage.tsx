import React, {useMemo, useState} from 'react';
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

type SortKey = 'name' | 'email' | 'status';
type SortDir = 'asc' | 'desc';

// The whole directory is already held in memory (getAllUsers walks every page), so
// searching and sorting are done client-side for instant feedback. Comparators return
// the ascending order; the direction toggle flips the sign.
const compareUsers = (a: DirectoryUser, b: DirectoryUser, key: SortKey): number => {
    switch (key) {
        case 'email':
            return a.email.localeCompare(b.email);
        case 'status':
            // Pending (not approved) first when ascending, so the actionable rows surface.
            return Number(a.approved) - Number(b.approved);
        case 'name':
        default:
            return a.fullName.localeCompare(b.fullName);
    }
};

const UserManagementPage: React.FC = () => {
    const {t} = useTranslation();
    const {isStaff} = usePermissions();
    const {user: currentUser} = useAuth();
    const {showSuccessToast} = useSuccessToast();

    const {data: users, isLoading, isError, refetch} = useUsers();
    const setApproval = useSetUserApproval();

    const [pendingOnly, setPendingOnly] = useState(false);
    const [search, setSearch] = useState('');
    const [sortKey, setSortKey] = useState<SortKey>('name');
    const [sortDir, setSortDir] = useState<SortDir>('asc');
    const [confirmingRevokeId, setConfirmingRevokeId] = useState<string | null>(null);

    const all = useMemo(() => users ?? [], [users]);
    const pendingCount = all.filter((u) => !u.approved).length;

    // Filter (pending toggle + free-text search over name/email) then sort. Memoised so
    // typing/toggling does not re-sort the list on every unrelated render.
    const visible = useMemo(() => {
        const query = search.trim().toLowerCase();
        const dir = sortDir === 'asc' ? 1 : -1;

        return all
            .filter((u) => (pendingOnly ? !u.approved : true))
            .filter((u) =>
                query === '' ||
                u.fullName.toLowerCase().includes(query) ||
                u.email.toLowerCase().includes(query),
            )
            .sort((a, b) => dir * compareUsers(a, b, sortKey));
    }, [all, pendingOnly, search, sortKey, sortDir]);

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

            <div className="admin-users-toolbar">
                <input
                    type="search"
                    className="admin-input admin-users-search"
                    placeholder={t('admin.users.searchPlaceholder')}
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    aria-label={t('admin.users.searchPlaceholder')}
                />

                <div className="admin-users-sort">
                    <label className="admin-users-sort-label" htmlFor="user-sort">{t('common.sort')}</label>
                    <select
                        id="user-sort"
                        className="admin-input admin-select admin-users-sort-select"
                        value={sortKey}
                        onChange={(e) => setSortKey(e.target.value as SortKey)}
                    >
                        <option value="name">{t('admin.users.sortByName')}</option>
                        <option value="email">{t('admin.users.sortByEmail')}</option>
                        <option value="status">{t('admin.users.sortByStatus')}</option>
                    </select>
                    <button
                        type="button"
                        className="admin-users-sort-dir"
                        onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
                        aria-label={t(sortDir === 'asc' ? 'admin.users.sortAsc' : 'admin.users.sortDesc')}
                        title={t(sortDir === 'asc' ? 'admin.users.sortAsc' : 'admin.users.sortDesc')}
                    >
                        {sortDir === 'asc' ? '↑' : '↓'}
                    </button>
                </div>
            </div>

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
                    <p>
                        {search.trim()
                            ? t('admin.users.noResults')
                            : pendingOnly
                                ? t('admin.users.noPending')
                                : t('admin.users.empty')}
                    </p>
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

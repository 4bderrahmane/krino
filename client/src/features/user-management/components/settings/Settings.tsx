// import React, {useEffect, useState} from 'react';
// import {useTranslation} from 'react-i18next';
// import '../../styles/settings/Settings.css';
//
// import {
//     getCurrentUser,
//     updatePartialProfile,
//     updateFullProfile,
//     changePassword,
//     deleteAccount
// } from '../../services/UserManagementService.ts';
//
// import { useLocation, useNavigate } from 'react-router-dom';
//
// interface SettingsProps {
//     section?: 'profile' | 'password' | 'delete';
// }
//
// const Settings: React.FC<SettingsProps> = ({ section }) => {
//     const {t} = useTranslation();
//     const location = useLocation();
//     const navigate = useNavigate();
//
//     const sectionFromPath = section ||
//         (location.pathname.endsWith('/password') ? 'password' :
//          location.pathname.endsWith('/delete') ? 'delete' :
//          'profile');
//
//     const [form, setForm] = useState({
//         firstName: '',
//         lastName: '',
//         email: '',
//         phoneNumber: '',
//     });
//
//     const [loading, setLoading] = useState(false);
//     const [success, setSuccess] = useState('');
//     const [error, setError] = useState('');
//     const [updateType, setUpdateType] = useState<'patch' | 'put'>('patch');
//     const [passwordForm, setPasswordForm] = useState({
//         currentPassword: '',
//         newPassword: '',
//         confirmNewPassword: '',
//     });
//
//     const [passwordLoading, setPasswordLoading] = useState(false);
//     const [passwordSuccess, setPasswordSuccess] = useState('');
//     const [passwordError, setPasswordError] = useState('');
//     const [deleteLoading, setDeleteLoading] = useState(false);
//     const [deleteError, setDeleteError] = useState('');
//     const [deleteSuccess, setDeleteSuccess] = useState('');
//     const [deletePassword, setDeletePassword] = useState('');
//     const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
//
//     useEffect(() => {
//         getCurrentUser().then(user => {
//             setForm({
//                 firstName: user.firstName || '',
//                 lastName: user.lastName || '',
//                 email: user.email || '',
//                 phoneNumber: user.phoneNumber || '',
//             });
//         });
//     }, []);
//
//     const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
//         setForm({...form, [e.target.name]: e.target.value});
//     };
//
//     const handleProfileSubmit = async (e: React.FormEvent) => {
//         e.preventDefault();
//         setLoading(true);
//         setSuccess('');
//         setError('');
//         try {
//             if (updateType === 'patch') {
//                 await updatePartialProfile(form);
//             } else {
//                 await updateFullProfile(form);
//             }
//             setSuccess(t('profile.profileUpdated'));
//         } catch (err: any) {
//             setError(t('profile.updateFailed'));
//         } finally {
//             setLoading(false);
//         }
//     };
//
//     const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
//         setPasswordForm({...passwordForm, [e.target.name]: e.target.value});
//     };
//
//     const handlePasswordSubmit = async (e: React.FormEvent) => {
//         e.preventDefault();
//         setPasswordLoading(true);
//         setPasswordSuccess('');
//         setPasswordError('');
//         if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
//             setPasswordError(t('profile.changePasswordFailed'));
//             setPasswordLoading(false);
//             return;
//         }
//         try {
//             await changePassword({
//                 currentPassword: passwordForm.currentPassword,
//                 newPassword: passwordForm.newPassword,
//                 confirmNewPassword: passwordForm.confirmNewPassword
//             });
//             setPasswordSuccess(t('profile.passwordChanged'));
//             setPasswordForm({currentPassword: '', newPassword: '', confirmNewPassword: ''});
//         } catch (err: any) {
//             setPasswordError(t('profile.changePasswordFailed'));
//         } finally {
//             setPasswordLoading(false);
//         }
//     };
//
//     const handleDeleteAccount = async (e: React.FormEvent) => {
//         e.preventDefault();
//         setDeleteLoading(true);
//         setDeleteError('');
//         setDeleteSuccess('');
//         try {
//             await deleteAccount(deletePassword);
//             setDeleteSuccess('Account deleted.');
//         } catch (err: any) {
//             setDeleteError('Failed to delete account.');
//         } finally {
//             setDeleteLoading(false);
//         }
//     };
//
//     return (
//         <div className="profile-container">
//             <div className="profile-header">
//                 <h1 className="profile-title">{t('page.settings') || 'Settings'}</h1>
//             </div>
//             {sectionFromPath === 'profile' && (
//                 <form className="profile-card" onSubmit={handleProfileSubmit}>
//                     <div className="profile-details">
//                         <div className="profile-info">
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="firstName">{t('profile.firstName')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="text"
//                                     id="firstName"
//                                     name="firstName"
//                                     value={form.firstName}
//                                     onChange={handleChange}
//                                     autoComplete="given-name"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="lastName">{t('profile.lastName')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="text"
//                                     id="lastName"
//                                     name="lastName"
//                                     value={form.lastName}
//                                     onChange={handleChange}
//                                     autoComplete="family-name"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="email">{t('profile.email')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="email"
//                                     id="email"
//                                     name="email"
//                                     value={form.email}
//                                     onChange={handleChange}
//                                     autoComplete="email"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="phoneNumber">{t('profile.phoneNumber')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="tel"
//                                     id="phoneNumber"
//                                     name="phoneNumber"
//                                     value={form.phoneNumber}
//                                     onChange={handleChange}
//                                     autoComplete="tel"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label">{t('profile.updateType')}</label>
//                                 <label>
//                                     <input
//                                         type="radio"
//                                         name="updateType"
//                                         value="patch"
//                                         checked={updateType === 'patch'}
//                                         onChange={() => setUpdateType('patch')}
//                                     /> PATCH
//                                 </label>
//                                 <label style={{marginLeft: 12}}>
//                                     <input
//                                         type="radio"
//                                         name="updateType"
//                                         value="put"
//                                         checked={updateType === 'put'}
//                                         onChange={() => setUpdateType('put')}
//                                     /> PUT
//                                 </label>
//                             </div>
//                         </div>
//                     </div>
//                     <div style={{marginTop: 24}}>
//                         <button type="submit" className="settings-link" disabled={loading}>
//                             {loading ? t('app.loading') : t('profile.saveChanges')}
//                         </button>
//                     </div>
//                     {success && <div className="settings-success">{success}</div>}
//                     {error && <div className="settings-error">{error}</div>}
//                 </form>
//             )}
//             {sectionFromPath === 'password' && (
//                 <form className="profile-card" onSubmit={handlePasswordSubmit} style={{marginTop: 32}}>
//                     <h2>{t('profile.changePassword')}</h2>
//                     <div className="profile-details">
//                         <div className="profile-info">
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="currentPassword">{t('profile.currentPassword')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="password"
//                                     id="currentPassword"
//                                     name="currentPassword"
//                                     value={passwordForm.currentPassword}
//                                     onChange={handlePasswordChange}
//                                     autoComplete="current-password"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="newPassword">{t('profile.newPassword')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="password"
//                                     id="newPassword"
//                                     name="newPassword"
//                                     value={passwordForm.newPassword}
//                                     onChange={handlePasswordChange}
//                                     autoComplete="new-password"
//                                 />
//                             </div>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="confirmNewPassword">{t('profile.confirmNewPassword')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="password"
//                                     id="confirmNewPassword"
//                                     name="confirmNewPassword"
//                                     value={passwordForm.confirmNewPassword}
//                                     onChange={handlePasswordChange}
//                                     autoComplete="new-password"
//                                 />
//                             </div>
//                         </div>
//                     </div>
//                     <div style={{marginTop: 24}}>
//                         <button type="submit" className="settings-link" disabled={passwordLoading}>
//                             {passwordLoading ? t('app.loading') : t('profile.changePassword')}
//                         </button>
//                     </div>
//                     {passwordSuccess && <div className="settings-success">{passwordSuccess}</div>}
//                     {passwordError && <div className="settings-error">{passwordError}</div>}
//                 </form>
//             )}
//             {sectionFromPath === 'delete' && (
//                 <div className="profile-card" style={{marginTop: 32}}>
//                     <h2 style={{color: '#dc2626'}}>{t('profile.deleteAccount') || 'Delete Account'}</h2>
//                     {!showDeleteConfirm ? (
//                         <button
//                             className="settings-link"
//                             style={{background: '#dc2626', marginTop: 16}}
//                             onClick={() => setShowDeleteConfirm(true)}
//                         >
//                             {t('profile.deleteAccount')}
//                         </button>
//                     ) : (
//                         <form onSubmit={handleDeleteAccount}>
//                             <div className="info-row">
//                                 <label className="info-label" htmlFor="deletePassword">{t('profile.currentPassword')}</label>
//                                 <input
//                                     className="info-value"
//                                     type="password"
//                                     id="deletePassword"
//                                     name="deletePassword"
//                                     value={deletePassword}
//                                     onChange={e => setDeletePassword(e.target.value)}
//                                     autoComplete="current-password"
//                                 />
//                             </div>
//                             <div style={{marginTop: 16}}>
//                                 <button
//                                     type="submit"
//                                     className="settings-link"
//                                     style={{background: '#dc2626'}}
//                                     disabled={deleteLoading}
//                                 >
//                                     {deleteLoading ? t('app.loading') : t('profile.deleteAccount')}
//                                 </button>
//                                 <button
//                                     type="button"
//                                     className="settings-link"
//                                     style={{marginLeft: 12}}
//                                     onClick={() => setShowDeleteConfirm(false)}
//                                 >
//                                     {t('common.cancel') || 'Cancel'}
//                                 </button>
//                             </div>
//                             {deleteSuccess && <div className="settings-success">{deleteSuccess}</div>}
//                             {deleteError && <div className="settings-error">{deleteError}</div>}
//                         </form>
//                     )}
//                 </div>
//             )}
//         </div>
//     );
// };
//
// export default Settings;

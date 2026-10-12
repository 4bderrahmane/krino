import {useRef, useState, type FormEvent} from 'react';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router-dom';
import {
    applyWithBaseCv,
    deleteApplication,
    updateApplicationStatus,
    uploadApplicationResume,
} from '@/features/applications/services/ApplicationService.ts';
import type {Application, ApplicationStatus} from '@/features/applications/types/application.types.ts';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {validateCvFile, type CvFileError} from '@/shared/utils/cvFile.ts';

const DECISIONS: ApplicationStatus[] = ['UNDER_REVIEW', 'ACCEPTED', 'REJECTED'];
const CV_ERROR_KEYS: Record<CvFileError, string> = {
    required: 'offers.apply.cvRequired',
    type: 'offers.apply.cvType',
    size: 'offers.apply.cvSize',
};

const ApplicationActions = ({application}: {application: Application}) => {
    const {t} = useTranslation();
    const {user} = useAuth();
    const {isStaff, isCandidate} = usePermissions();
    const {showSuccessToast} = useSuccessToast();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [decision, setDecision] = useState<ApplicationStatus | ''>('');
    const [file, setFile] = useState<File | null>(null);
    const [confirmingDelete, setConfirmingDelete] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const fileInput = useRef<HTMLInputElement>(null);
    const isOwner = isCandidate && user?.id === application.candidate.id;

    const refreshApplication = (updated: Application) => {
        queryClient.setQueryData(['application', updated.id, user?.id], updated);
        void queryClient.invalidateQueries({queryKey: ['applications']});
    };
    const statusMutation = useMutation({
        mutationFn: (status: ApplicationStatus) => updateApplicationStatus(application.id, status),
        onSuccess: refreshApplication,
    });
    const uploadMutation = useMutation({
        mutationFn: (resume: File) => uploadApplicationResume(application.id, resume),
        onSuccess: refreshApplication,
    });
    const baseMutation = useMutation({
        mutationFn: () => applyWithBaseCv(application.id),
        onSuccess: refreshApplication,
    });
    const deleteMutation = useMutation({
        mutationFn: () => deleteApplication(application.id),
        onSuccess: () => {
            queryClient.removeQueries({queryKey: ['application', application.id]});
            void queryClient.invalidateQueries({queryKey: ['applications']});
            showSuccessToast(t('applications.actions.deleteSuccess'));
            navigate('/applications');
        },
    });

    if (!isStaff && !isOwner) return null;

    const saveDecision = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError(null);
        if (!decision) return;
        try {
            await statusMutation.mutateAsync(decision);
            setDecision('');
            showSuccessToast(t('applications.actions.decisionSuccess'));
        } catch (failure: unknown) {
            setError(resolveServerError(t, failure));
        }
    };

    const uploadResume = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError(null);
        const cvError = validateCvFile(file);
        if (cvError) {
            setError(t(CV_ERROR_KEYS[cvError]));
            return;
        }
        try {
            await uploadMutation.mutateAsync(file as File);
            setFile(null);
            if (fileInput.current) fileInput.current.value = '';
            showSuccessToast(t('applications.actions.resumeSuccess'));
        } catch (failure: unknown) {
            setError(resolveServerError(t, failure));
        }
    };

    const useBaseResume = async () => {
        setError(null);
        try {
            await baseMutation.mutateAsync();
            showSuccessToast(t('applications.actions.resumeSuccess'));
        } catch (failure: unknown) {
            setError(resolveServerError(t, failure));
        }
    };

    const withdraw = async () => {
        setError(null);
        try {
            await deleteMutation.mutateAsync();
        } catch (failure: unknown) {
            setError(resolveServerError(t, failure));
        }
    };

    return (
        <section className="application-detail-section application-actions">
            <h2 className="application-detail-heading">{t('applications.actions.title')}</h2>
            {isStaff && (
                <form className="application-action-row" onSubmit={saveDecision}>
                    <label htmlFor="application-decision">{t('applications.actions.decision')}</label>
                    <select id="application-decision" value={decision}
                            onChange={(event) => setDecision(event.target.value as ApplicationStatus | '')}>
                        <option value="">{t('applications.actions.chooseDecision')}</option>
                        {DECISIONS.map((status) => (
                            <option key={status} value={status}>{t(`applications.status.${status}`)}</option>
                        ))}
                    </select>
                    <button type="submit" disabled={!decision || statusMutation.isPending}>
                        {statusMutation.isPending ? t('app.loading') : t('applications.actions.saveDecision')}
                    </button>
                </form>
            )}
            {isOwner && (
                <>
                    <form className="application-action-row" onSubmit={uploadResume}>
                        <label htmlFor="application-resume">{t('applications.actions.replaceResume')}</label>
                        <input id="application-resume" ref={fileInput} type="file" accept="application/pdf"
                               onChange={(event) => setFile(event.target.files?.[0] ?? null)}/>
                        <button type="submit" disabled={uploadMutation.isPending || baseMutation.isPending}>
                            {uploadMutation.isPending ? t('app.loading') : t('applications.actions.uploadResume')}
                        </button>
                    </form>
                    {user?.resumeFilename && (
                        <button type="button" className="application-action-secondary"
                                disabled={uploadMutation.isPending || baseMutation.isPending}
                                onClick={useBaseResume}>
                            {t('applications.actions.useBaseResume')}
                        </button>
                    )}
                    <div className="application-action-delete">
                        {confirmingDelete ? (
                            <>
                                <p>{t('applications.actions.deleteConfirm')}</p>
                                <button type="button" className="application-action-danger"
                                        disabled={deleteMutation.isPending} onClick={withdraw}>
                                    {deleteMutation.isPending ? t('app.loading') : t('applications.actions.confirmDelete')}
                                </button>
                                <button type="button" className="application-action-secondary"
                                        onClick={() => setConfirmingDelete(false)}>
                                    {t('common.cancel')}
                                </button>
                            </>
                        ) : (
                            <button type="button" className="application-action-danger"
                                    onClick={() => setConfirmingDelete(true)}>
                                {t('applications.actions.deleteApplication')}
                            </button>
                        )}
                    </div>
                </>
            )}
            {error && <p className="application-action-error" role="alert">{error}</p>}
        </section>
    );
};

export default ApplicationActions;

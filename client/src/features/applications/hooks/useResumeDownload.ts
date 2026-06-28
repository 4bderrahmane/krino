import {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {getApplicationResumeBlob} from '@/features/applications/services/ApplicationService.ts';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';

// Opens an application's résumé PDF. The file lives behind an authenticated
// endpoint, so we fetch it as a blob (via the api client, which carries the
// session cookie) and open an object URL rather than linking to a public URL.
// Shared by the list and detail pages so the behaviour stays identical.
export const useResumeDownload = () => {
    const {t} = useTranslation();
    const {showErrorToast} = useSuccessToast();
    const [downloadingId, setDownloadingId] = useState<string | null>(null);

    const openResume = async (applicationId: string) => {
        setDownloadingId(applicationId);
        try {
            const blob = await getApplicationResumeBlob(applicationId);
            const url = URL.createObjectURL(blob);
            window.open(url, '_blank', 'noopener,noreferrer');
            // Give the new tab time to load before releasing the object URL.
            setTimeout(() => URL.revokeObjectURL(url), 60_000);
        } catch (err) {
            console.error('resume download failed:', err);
            showErrorToast(t('applications.resumeError'));
        } finally {
            setDownloadingId(null);
        }
    };

    return {downloadingId, openResume};
};

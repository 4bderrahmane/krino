import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate} from 'react-router-dom';
import {useSuccessToast} from '@/shared/hooks/useSuccessToast';
import {resolveServerError} from '@/shared/services/errors';
import {
    useArchiveOffer,
    useCloseOffer,
    useDeleteOffer,
    usePauseOffer,
    usePublishOffer,
} from '@/features/offers/hooks/useOffers.ts';
import {canArchive, canClose, canEdit, canPause, canPublish} from '@/features/offers/utils/offerTransitions.ts';
import type {CloseStatus} from '@/features/offers/services/OfferService.ts';
import type {Offer} from '@/features/offers/types/offer.types.ts';

interface OfferManagePanelProps {
    offer: Offer;
}

const CLOSE_STATUSES: CloseStatus[] = ['CLOSED', 'FILLED', 'CANCELLED'];

// Staff-only lifecycle controls for a single offer. Only the transitions the
// backend state machine accepts from the current status are rendered.
const OfferManagePanel: React.FC<OfferManagePanelProps> = ({offer}) => {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {showSuccessToast} = useSuccessToast();

    const publish = usePublishOffer();
    const pause = usePauseOffer();
    const close = useCloseOffer();
    const archive = useArchiveOffer();
    const remove = useDeleteOffer();

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [closing, setClosing] = useState(false);
    const [closeStatus, setCloseStatus] = useState<CloseStatus>('CLOSED');
    const [confirmingDelete, setConfirmingDelete] = useState(false);

    const busy =
        publish.isPending ||
        pause.isPending ||
        close.isPending ||
        archive.isPending ||
        remove.isPending;

    // Wraps a transition with shared toast + error handling.
    const run = async (action: () => Promise<unknown>, successKey: string) => {
        setErrorMessage(null);
        try {
            await action();
            showSuccessToast(t(successKey));
        } catch (err: unknown) {
            console.error('offer transition failed:', err);
            setErrorMessage(resolveServerError(t, err));
        }
    };

    const handleClose = async () => {
        await run(() => close.mutateAsync({id: offer.id, status: closeStatus}), 'offers.manage.closeSuccess');
        setClosing(false);
    };

    const handleDelete = async () => {
        setErrorMessage(null);
        try {
            await remove.mutateAsync(offer.id);
            showSuccessToast(t('offers.manage.deleteSuccess'));
            navigate('/offers');
        } catch (err: unknown) {
            console.error('delete offer failed:', err);
            setErrorMessage(resolveServerError(t, err));
            setConfirmingDelete(false);
        }
    };

    return (
        <section className="offer-detail-section offer-manage">
            <h2 className="offer-detail-heading">{t('offers.manage.heading')}</h2>
            <p className="offer-manage-status">
                {t('offers.detail.status')}: <strong>{t(`offers.status.${offer.status}`)}</strong>
            </p>

            <div className="offer-manage-actions">
                {canPublish(offer.status) && (
                    <button
                        type="button"
                        className="offer-manage-button offer-manage-primary"
                        disabled={busy}
                        onClick={() => run(() => publish.mutateAsync(offer.id), 'offers.manage.publishSuccess')}
                    >
                        {/* A paused posting is resumed rather than freshly published. */}
                        {offer.status === 'PAUSED' ? t('offers.manage.resume') : t('offers.manage.publish')}
                    </button>
                )}

                {canPause(offer.status) && (
                    <button
                        type="button"
                        className="offer-manage-button"
                        disabled={busy}
                        onClick={() => run(() => pause.mutateAsync(offer.id), 'offers.manage.pauseSuccess')}
                    >
                        {t('offers.manage.pause')}
                    </button>
                )}

                {canEdit(offer.status) && (
                    <Link to={`/offers/${offer.id}/edit`} className="offer-manage-button">
                        {t('offers.manage.edit')}
                    </Link>
                )}

                {canClose(offer.status) && !closing && (
                    <button
                        type="button"
                        className="offer-manage-button"
                        disabled={busy}
                        onClick={() => setClosing(true)}
                    >
                        {t('offers.manage.close')}
                    </button>
                )}

                {canArchive(offer.status) && (
                    <button
                        type="button"
                        className="offer-manage-button"
                        disabled={busy}
                        onClick={() => run(() => archive.mutateAsync(offer.id), 'offers.manage.archiveSuccess')}
                    >
                        {t('offers.manage.archive')}
                    </button>
                )}

                {!confirmingDelete ? (
                    <button
                        type="button"
                        className="offer-manage-button offer-manage-danger"
                        disabled={busy}
                        onClick={() => setConfirmingDelete(true)}
                    >
                        {t('offers.manage.delete')}
                    </button>
                ) : (
                    <span className="offer-manage-confirm">
                        <span className="offer-manage-confirm-label">{t('offers.manage.deleteConfirm')}</span>
                        <button
                            type="button"
                            className="offer-manage-button offer-manage-danger"
                            disabled={busy}
                            onClick={handleDelete}
                        >
                            {t('common.yes')}
                        </button>
                        <button
                            type="button"
                            className="offer-manage-button"
                            disabled={busy}
                            onClick={() => setConfirmingDelete(false)}
                        >
                            {t('common.no')}
                        </button>
                    </span>
                )}
            </div>

            {closing && (
                <div className="offer-manage-close-form">
                    <label className="offer-field">
                        <span className="offer-field-label">{t('offers.manage.closeReason')}</span>
                        <select
                            className="offer-input"
                            value={closeStatus}
                            onChange={(e) => setCloseStatus(e.target.value as CloseStatus)}
                        >
                            {CLOSE_STATUSES.map((s) => (
                                <option key={s} value={s}>{t(`offers.status.${s}`)}</option>
                            ))}
                        </select>
                    </label>
                    <div className="offer-manage-close-actions">
                        <button
                            type="button"
                            className="offer-manage-button"
                            disabled={busy}
                            onClick={() => setClosing(false)}
                        >
                            {t('common.cancel')}
                        </button>
                        <button
                            type="button"
                            className="offer-manage-button offer-manage-primary"
                            disabled={busy}
                            onClick={handleClose}
                        >
                            {t('offers.manage.confirmClose')}
                        </button>
                    </div>
                </div>
            )}

            {errorMessage && <div className="offer-apply-error">{errorMessage}</div>}
        </section>
    );
};

export default OfferManagePanel;

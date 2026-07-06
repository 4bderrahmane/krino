// Mirrors the backend offer (job) state machine — see Job#publish/pause/close/archive.
// Keep these in sync with the entity guards so the UI only offers transitions the
// server will accept.
import type {OfferStatus} from '@/features/offers/types/offer.types.ts';

// publish: DRAFT | SCHEDULED | PAUSED -> OPEN
export const canPublish = (status: OfferStatus): boolean =>
    status === 'DRAFT' || status === 'SCHEDULED' || status === 'PAUSED';

// pause: OPEN -> PAUSED
export const canPause = (status: OfferStatus): boolean => status === 'OPEN';

// close: any active posting -> CLOSED | FILLED | CANCELLED (never from a terminal
// or archived state).
export const canClose = (status: OfferStatus): boolean =>
    status === 'DRAFT' || status === 'SCHEDULED' || status === 'OPEN' || status === 'PAUSED';

// archive: anything except an OPEN posting (must be closed first) or one already
// archived.
export const canArchive = (status: OfferStatus): boolean =>
    status !== 'OPEN' && status !== 'ARCHIVED';

// edit: blocked only once archived (the entity rejects modifying an archived posting).
export const canEdit = (status: OfferStatus): boolean => status !== 'ARCHIVED';

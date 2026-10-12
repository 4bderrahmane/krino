import {describe, expect, it} from 'vitest';
import {
    canArchive,
    canClose,
    canEdit,
    canPause,
    canPublish,
} from '@/features/offers/utils/offerTransitions.ts';
import type {OfferStatus} from '@/features/offers/types/offer.types.ts';

/**
 * These predicates mirror the backend state machine (Job#publish/pause/close/archive
 * and its guards). Getting them wrong shows up as a button that produces a 409, so
 * the whole matrix is spelled out rather than sampled.
 *
 * Typing the table as a Record over OfferStatus is what keeps it honest: adding a
 * status to the union fails compilation here until this table covers it.
 */
const MATRIX: Record<OfferStatus, {
    publish: boolean;
    pause: boolean;
    close: boolean;
    archive: boolean;
    edit: boolean;
}> = {
    DRAFT: {publish: true, pause: false, close: true, archive: true, edit: true},
    SCHEDULED: {publish: true, pause: false, close: true, archive: true, edit: true},
    OPEN: {publish: false, pause: true, close: true, archive: false, edit: true},
    PAUSED: {publish: true, pause: false, close: true, archive: true, edit: true},
    CLOSED: {publish: false, pause: false, close: false, archive: true, edit: true},
    FILLED: {publish: false, pause: false, close: false, archive: true, edit: true},
    CANCELLED: {publish: false, pause: false, close: false, archive: true, edit: true},
    ARCHIVED: {publish: false, pause: false, close: false, archive: false, edit: false},
};

describe('offer transitions', () => {
    it.each(Object.entries(MATRIX))('allows the right actions from %s', (status, expected) => {
        const from = status as OfferStatus;

        expect(canPublish(from)).toBe(expected.publish);
        expect(canPause(from)).toBe(expected.pause);
        expect(canClose(from)).toBe(expected.close);
        expect(canArchive(from)).toBe(expected.archive);
        expect(canEdit(from)).toBe(expected.edit);
    });

    it('never offers to archive an open posting, which the server requires closing first', () => {
        expect(canArchive('OPEN')).toBe(false);
        expect(canClose('OPEN')).toBe(true);
    });

    it('offers nothing at all on an archived posting', () => {
        const archived: OfferStatus = 'ARCHIVED';

        expect([
            canPublish(archived),
            canPause(archived),
            canClose(archived),
            canArchive(archived),
            canEdit(archived),
        ]).toEqual([false, false, false, false, false]);
    });
});

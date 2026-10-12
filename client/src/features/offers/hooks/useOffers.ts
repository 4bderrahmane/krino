import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
    archiveOffer,
    closeOffer,
    createOffer,
    deleteOffer,
    getManagedOffers,
    getOfferById,
    getPublicOfferById,
    getPublicOffers,
    pauseOffer,
    publishOffer,
    updateOffer,
    type CloseStatus,
} from '@/features/offers/services/OfferService.ts';
import type {EditOfferInput} from '@/features/offers/types/offer.types.ts';
import {useAuth} from '@/shared/hooks/useAuth';
import {usePermissions} from '@/shared/hooks/usePermissions';

/**
 * The catalogue, from whichever endpoint the caller is entitled to.
 *
 * <p>Internal staff read the full list including drafts; everyone else, signed in or
 * not, reads the published one. The audience is part of the query key so a visitor's
 * cached list is never handed to a staff member who signs in on the same tab.
 */
export const useOffers = () => {
    const {isStaff, isInterviewer} = usePermissions();
    const {user} = useAuth();
    const seesDrafts = isStaff || isInterviewer;

    return useQuery({
        queryKey: ['offers', seesDrafts ? 'managed' : 'public', seesDrafts ? user?.id : null],
        queryFn: ({signal}) => seesDrafts ? getManagedOffers(signal) : getPublicOffers(signal),
    });
};

/**
 * A single offer. Signed-in users go through the authenticated endpoint, which also
 * covers a candidate revisiting a posting they applied to after it closed; visitors
 * go through the public one, which only ever resolves published postings.
 */
export const useOffer = (id: string | undefined) => {
    const {user} = useAuth();

    return useQuery({
        queryKey: ['offer', id, user?.id ?? 'public'],
        queryFn: ({signal}) => user
            ? getOfferById(id as string, signal)
            : getPublicOfferById(id as string, signal),
        enabled: !!id,
    });
};

// Creates an offer, then invalidates the list so the new posting appears.
export const useCreateOffer = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createOffer,
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['offers']});
        },
    });
};

// Refresh both the list and the affected offer's own cache entry after a write.
const useInvalidateOffer = () => {
    const queryClient = useQueryClient();
    return (id: string) => {
        queryClient.invalidateQueries({queryKey: ['offers']});
        queryClient.invalidateQueries({queryKey: ['offer', id]});
    };
};

export const useUpdateOffer = () => {
    const invalidate = useInvalidateOffer();
    return useMutation({
        mutationFn: ({id, input}: {id: string; input: EditOfferInput}) => updateOffer(id, input),
        onSuccess: (offer) => invalidate(offer.id),
    });
};

export const usePublishOffer = () => {
    const invalidate = useInvalidateOffer();
    return useMutation({
        mutationFn: publishOffer,
        onSuccess: (offer) => invalidate(offer.id),
    });
};

export const usePauseOffer = () => {
    const invalidate = useInvalidateOffer();
    return useMutation({
        mutationFn: pauseOffer,
        onSuccess: (offer) => invalidate(offer.id),
    });
};

export const useCloseOffer = () => {
    const invalidate = useInvalidateOffer();
    return useMutation({
        mutationFn: ({id, status}: {id: string; status: CloseStatus}) => closeOffer(id, status),
        onSuccess: (offer) => invalidate(offer.id),
    });
};

export const useArchiveOffer = () => {
    const invalidate = useInvalidateOffer();
    return useMutation({
        mutationFn: archiveOffer,
        onSuccess: (offer) => invalidate(offer.id),
    });
};

// Deletes an offer and drops it from the list cache.
export const useDeleteOffer = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteOffer,
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['offers']}),
    });
};

import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {
    archiveOffer,
    closeOffer,
    createOffer,
    deleteOffer,
    getOffers,
    getOfferById,
    pauseOffer,
    publishOffer,
    updateOffer,
    type CloseStatus,
} from '@/features/offers/services/OfferService.ts';
import type {EditOfferInput} from '@/features/offers/types/offer.types.ts';

export const useOffers = () =>
    useQuery({
        queryKey: ['offers'],
        queryFn: getOffers,
    });

export const useOffer = (id: string | undefined) =>
    useQuery({
        queryKey: ['offer', id],
        queryFn: () => getOfferById(id as string),
        enabled: !!id,
    });

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

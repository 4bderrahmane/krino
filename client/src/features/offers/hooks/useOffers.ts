import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {createOffer, getOffers, getOfferById} from '@/features/offers/services/OfferService.ts';

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

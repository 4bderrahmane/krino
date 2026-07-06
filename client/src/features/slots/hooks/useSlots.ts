import {keepPreviousData, useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {createSlot, deleteSlot, getSlots, updateSlot} from '@/features/slots/services/SlotService.ts';
import {getUsersByRole} from '@/features/administration/services/AdminService.ts';
import type {SlotFormValues} from '@/features/slots/types/slot.types.ts';

// `enabled` lets the page skip the (staff-only) request for non-staff users who
// reach the route directly, avoiding a pointless 403.
export const useSlots = (page: number, enabled = true) =>
    useQuery({
        queryKey: ['slots', page],
        queryFn: () => getSlots(page),
        enabled,
        // Keep the current page visible while the next loads (no spinner flash).
        placeholderData: keepPreviousData,
    });

// Interviewers for the slot create form's picker. Cached separately from the
// slots list so opening the form doesn't refetch the whole directory each time.
export const useInterviewers = () =>
    useQuery({
        queryKey: ['users', 'interviewers'],
        queryFn: () => getUsersByRole('INTERVIEWER'),
    });

export const useCreateSlot = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createSlot,
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['slots']}),
    });
};

export const useUpdateSlot = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({id, values}: {id: string; values: SlotFormValues}) => updateSlot(id, values),
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['slots']}),
    });
};

export const useDeleteSlot = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteSlot,
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['slots']}),
    });
};

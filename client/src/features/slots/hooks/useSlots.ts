import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {createSlot, deleteSlot, getMySlots, getSlots, updateSlot} from '@/features/slots/services/SlotService.ts';
import {getUsersByRole} from '@/features/administration/services/AdminService.ts';
import type {SlotFormValues} from '@/features/slots/types/slot.types.ts';
import {useAuth} from '@/shared/hooks/useAuth';

// Staff load all slots; interviewers load only their own. The query stays disabled
// for roles that cannot open the availability page.
export const useSlots = (page: number, scope: 'all' | 'mine', enabled = true) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['slots', scope, page, user?.id],
        queryFn: ({signal}) => scope === 'mine'
            ? getMySlots(page, 20, signal)
            : getSlots(page, 20, signal),
        enabled: enabled && !!user,
        // Keep a previous page only within the same account.
        placeholderData: (previousData, previousQuery) =>
            previousQuery?.queryKey[1] === scope && previousQuery?.queryKey[3] === user?.id
                ? previousData : undefined,
    });
};

// Interviewers for the slot create form's picker. Cached separately from the
// slots list so opening the form doesn't refetch the whole directory each time.
export const useInterviewers = (enabled = true) => {
    const {user} = useAuth();
    return useQuery({
        queryKey: ['users', 'interviewers', user?.id],
        queryFn: ({signal}) => getUsersByRole('INTERVIEWER', signal),
        enabled: enabled && !!user,
    });
};

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

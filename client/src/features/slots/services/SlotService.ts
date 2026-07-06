import api from '@/shared/services/api.ts';
import type {Slot, SlotFormValues, SlotInterviewer, SlotPage} from '@/features/slots/types/slot.types.ts';

// ---------------------------------------------------------------------------
// Raw backend shapes, kept private to this module.
// ---------------------------------------------------------------------------
interface UserDTO {
    id: string;
    firstName?: string | null;
    lastName?: string | null;
    email?: string | null;
}

interface SlotResponseDTO {
    id: string;
    interviewer?: UserDTO | null;
    durationInMinutes?: number | null;
    available?: boolean | null;
    interviewDate?: string | null;
    startTime?: string | null;
    endTime?: string | null;
}

interface PageResponse<T> {
    content: T[];
    page: {
        number: number;
        size: number;
        totalElements: number;
        totalPages: number;
    };
}

// Mirrors SlotRequestDTO (POST) — interviewerId is optional; when omitted the
// backend assigns the slot to the authenticated user.
interface SlotRequestDTO {
    interviewerId?: string;
    interviewDate: string;
    startTime: string;
    endTime: string;
}

// Mirrors SlotUpdateDTO (PUT/PATCH) — window only, no interviewer.
interface SlotUpdateDTO {
    interviewDate?: string;
    startTime?: string;
    endTime?: string;
}

const toInterviewer = (user: UserDTO | null | undefined): SlotInterviewer => ({
    id: user?.id ?? '',
    fullName: [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim() || '—',
    email: user?.email ?? '—',
});

const toSlot = (dto: SlotResponseDTO): Slot => ({
    id: dto.id,
    interviewer: toInterviewer(dto.interviewer),
    date: dto.interviewDate ?? null,
    startTime: dto.startTime ?? null,
    endTime: dto.endTime ?? null,
    durationInMinutes: dto.durationInMinutes ?? null,
    available: dto.available ?? true,
});

// The backend LocalTime accepts ISO_LOCAL_TIME; an <input type="time"> yields
// "HH:mm", so we normalise to "HH:mm:ss" to stay unambiguous on the wire.
const toApiTime = (value: string): string => (value.length === 5 ? `${value}:00` : value);

const SLOTS_ENDPOINT = '/slots';

/** All availability slots — staff only (ADMIN / HR_MANAGER). */
export const getSlots = async (page = 0, size = 20): Promise<SlotPage> => {
    const {data} = await api.get<PageResponse<SlotResponseDTO>>(SLOTS_ENDPOINT, {
        params: {page, size},
    });
    return {slots: data.content.map(toSlot), page: data.page};
};

/** Create an availability slot. Requires CAN_CREATE_SLOT. */
export const createSlot = async (values: SlotFormValues): Promise<Slot> => {
    const payload: SlotRequestDTO = {
        interviewDate: values.date,
        startTime: toApiTime(values.startTime),
        endTime: toApiTime(values.endTime),
    };
    if (values.interviewerId) payload.interviewerId = values.interviewerId;
    const {data} = await api.post<SlotResponseDTO>(SLOTS_ENDPOINT, payload);
    return toSlot(data);
};

/** Replace a slot's window (PUT). Requires CAN_UPDATE_SLOT. */
export const updateSlot = async (id: string, values: SlotFormValues): Promise<Slot> => {
    const payload: SlotUpdateDTO = {
        interviewDate: values.date,
        startTime: toApiTime(values.startTime),
        endTime: toApiTime(values.endTime),
    };
    const {data} = await api.put<SlotResponseDTO>(`${SLOTS_ENDPOINT}/${id}`, payload);
    return toSlot(data);
};

/** Delete a slot. Requires CAN_DELETE_SLOT. */
export const deleteSlot = async (id: string): Promise<void> => {
    await api.delete(`${SLOTS_ENDPOINT}/${id}`);
};

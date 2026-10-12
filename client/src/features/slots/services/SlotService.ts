import api from '@/shared/services/api.ts';
import type {Slot, SlotFormValues, SlotInterviewer, SlotPage} from '@/features/slots/types/slot.types.ts';
import {toInstant, toLocalDate, toLocalTime} from '@/shared/utils/slotWindow.ts';

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
    // Absolute instants in UTC, e.g. 2026-06-25T08:00:00Z.
    startAt?: string | null;
    endAt?: string | null;
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
    startAt: string;
    endAt: string;
}

// Mirrors SlotUpdateDTO (PUT/PATCH) — window only, no interviewer.
interface SlotUpdateDTO {
    startAt?: string;
    endAt?: string;
}

const toInterviewer = (user: UserDTO | null | undefined): SlotInterviewer => ({
    id: user?.id ?? '',
    fullName: [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim() || '—',
    email: user?.email ?? '—',
});

// The window arrives as instants; the table and the form show it on the local clock.
const toSlot = (dto: SlotResponseDTO): Slot => ({
    id: dto.id,
    interviewer: toInterviewer(dto.interviewer),
    date: dto.startAt ? toLocalDate(dto.startAt) : null,
    startTime: dto.startAt ? toLocalTime(dto.startAt) : null,
    endTime: dto.endAt ? toLocalTime(dto.endAt) : null,
    durationInMinutes: dto.durationInMinutes ?? null,
    available: dto.available ?? true,
});

const toWindow = (values: SlotFormValues) => ({
    startAt: toInstant(values.date, values.startTime),
    endAt: toInstant(values.date, values.endTime),
});

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
    const payload: SlotRequestDTO = toWindow(values);
    if (values.interviewerId) payload.interviewerId = values.interviewerId;
    const {data} = await api.post<SlotResponseDTO>(SLOTS_ENDPOINT, payload);
    return toSlot(data);
};

/** Replace a slot's window (PUT). Requires CAN_UPDATE_SLOT. */
export const updateSlot = async (id: string, values: SlotFormValues): Promise<Slot> => {
    const payload: SlotUpdateDTO = toWindow(values);
    const {data} = await api.put<SlotResponseDTO>(`${SLOTS_ENDPOINT}/${id}`, payload);
    return toSlot(data);
};

/** Delete a slot. Requires CAN_DELETE_SLOT. */
export const deleteSlot = async (id: string): Promise<void> => {
    await api.delete(`${SLOTS_ENDPOINT}/${id}`);
};

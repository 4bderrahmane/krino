import api from '@/shared/services/api.ts';
import type {StaffCreateRequest, StaffCreationResponse} from '@/features/administration/types/admin.types.ts';

export const createStaff = async (payload: StaffCreateRequest): Promise<StaffCreationResponse> => {
    const {data} = await api.post<StaffCreationResponse>('/users', payload);
    return data;
};

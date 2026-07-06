import api from '@/shared/services/api.ts';
import type {Role, UserResponseDTO} from '@/features/authentication/types/api.types';
import type {DirectoryUser, StaffCreateRequest} from '@/features/administration/types/admin.types.ts';

interface PageResponse<T> {
    content: T[];
    page: {
        number: number;
        size: number;
        totalElements: number;
        totalPages: number;
    };
}

// The generated initial password is emailed to the new staff member; the API only
// returns the created user.
export const createStaff = async (payload: StaffCreateRequest): Promise<UserResponseDTO> => {
    const {data} = await api.post<UserResponseDTO>('/users', payload);
    return data;
};

const USERS_ENDPOINT = '/users';

const toDirectoryUser = (dto: UserResponseDTO): DirectoryUser => ({
    id: dto.id,
    fullName: [dto.firstName, dto.lastName].filter(Boolean).join(' ').trim() || dto.email,
    email: dto.email,
    roles: dto.roles,
    approved: dto.approved ?? true,
});

// GET /api/users — staff only (ADMIN / HR_MANAGER), server-paginated (size 20).
export const getUsers = async (page = 0): Promise<{users: DirectoryUser[]; totalPages: number}> => {
    const {data} = await api.get<PageResponse<UserResponseDTO>>(USERS_ENDPOINT, {params: {page}});
    return {users: data.content.map(toDirectoryUser), totalPages: data.page.totalPages};
};

// Walks every page so callers needing the complete directory (the approval table,
// the interviewer picker) get all users, not just the first server page.
export const getAllUsers = async (): Promise<DirectoryUser[]> => {
    const first = await getUsers(0);
    const all = [...first.users];
    for (let page = 1; page < first.totalPages; page += 1) {
        const next = await getUsers(page);
        all.push(...next.users);
    }
    return all;
};

// Used by staff pickers (e.g. the interviewer dropdown on the slot form). The
// /users endpoint has no server-side role filter, so we filter client-side.
export const getUsersByRole = async (role: Role): Promise<DirectoryUser[]> =>
    (await getAllUsers()).filter((user) => user.roles.includes(role));

// PATCH /api/users/{publicId}/approval — approve (true) or revoke (false). Requires
// ADMIN / HR_MANAGER.
export const setUserApproval = async (publicId: string, approved: boolean): Promise<void> => {
    await api.patch(`${USERS_ENDPOINT}/${publicId}/approval`, {approved});
};

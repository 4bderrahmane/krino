import type {Role} from '@/features/authentication/types/api.types';

export type StaffRole = Extract<Role, 'HR_MANAGER' | 'INTERVIEWER'>;

export interface StaffCreateRequest {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber?: string;
    role: StaffRole;
}

// A user as shown in staff-facing pickers/directories (e.g. choosing an
// interviewer when creating an availability slot).
export interface DirectoryUser {
    id: string;
    fullName: string;
    email: string;
    roles: Role[];
    approved: boolean;
}

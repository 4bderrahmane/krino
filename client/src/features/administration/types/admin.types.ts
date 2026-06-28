import type {Role, UserResponseDTO} from '@/features/authentication/types/api.types';

export type StaffRole = Extract<Role, 'HR_MANAGER' | 'INTERVIEWER'>;

export interface StaffCreateRequest {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber?: string;
    role: StaffRole;
}

export interface StaffCreationResponse {
    user: UserResponseDTO;
    initialPassword: string; // Generated default password, returned once so the admin can pass it to the new member.
}

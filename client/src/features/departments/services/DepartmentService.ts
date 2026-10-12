import api from '@/shared/services/api.ts';
import type {Department} from '@/features/departments/types/department.types.ts';

// Raw backend shape, kept private to this module.
interface DepartmentResponseDTO {
    id: string;
    name: string;
    description?: string | null;
}

const DEPARTMENTS_ENDPOINT = '/departments';
const PUBLIC_DEPARTMENTS_ENDPOINT = '/public/departments';

const toDepartment = (dto: DepartmentResponseDTO): Department => ({
    id: dto.id,
    name: dto.name,
    description: dto.description ?? null,
});

// Both endpoints answer with a plain array, already ordered by name: a company has
// tens of departments, not thousands. Nothing here pages.
const fetchDepartments = async (endpoint: string, signal?: AbortSignal): Promise<Department[]> => {
    const {data} = await api.get<DepartmentResponseDTO[]>(endpoint, {signal});
    return data.map(toDepartment);
};

// GET /api/public/departments — no session needed, and only departments with a
// published opening. A department whose work is unannounced is deliberately absent:
// listing it would leak exactly what keeping its postings in draft conceals.
export const getPublicDepartments = (signal?: AbortSignal): Promise<Department[]> =>
    fetchDepartments(PUBLIC_DEPARTMENTS_ENDPOINT, signal);

// GET /api/departments — the whole internal directory, hiring or not, so
// ADMIN/HR_MANAGER/INTERVIEWER only. This is the one the offer form's picker needs:
// a new posting can be filed under a department that has nothing open yet.
export const getManagedDepartments = (signal?: AbortSignal): Promise<Department[]> =>
    fetchDepartments(DEPARTMENTS_ENDPOINT, signal);

// Mirrors DepartmentCreateDTO. Description is omitted entirely when empty so the
// backend stores null rather than a blank string.
interface DepartmentCreateDTO {
    name: string;
    description?: string;
}

export interface CreateDepartmentInput {
    name: string;
    description: string | null;
}

// POST /api/departments — requires CAN_CREATE_DEPARTMENT (ADMIN, HR_MANAGER).
export const createDepartment = async (input: CreateDepartmentInput): Promise<Department> => {
    const payload: DepartmentCreateDTO = {name: input.name};
    if (input.description) payload.description = input.description;
    const {data} = await api.post<DepartmentResponseDTO>(DEPARTMENTS_ENDPOINT, payload);
    return toDepartment(data);
};

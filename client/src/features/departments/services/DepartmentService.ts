import api from '@/shared/services/api.ts';
import type {Department, DepartmentPage} from '@/features/departments/types/department.types.ts';

// Raw backend shape, kept private to this module.
interface DepartmentResponseDTO {
    id: string;
    name: string;
    description?: string | null;
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

const DEPARTMENTS_ENDPOINT = '/departments';

const toDepartment = (dto: DepartmentResponseDTO): Department => ({
    id: dto.id,
    name: dto.name,
    description: dto.description ?? null,
});

// The backend paginates departments server-side (GET /api/departments, default
// size 20), so we forward the requested page and surface the page metadata.
export const getDepartments = async (page = 0): Promise<DepartmentPage> => {
    const {data} = await api.get<PageResponse<DepartmentResponseDTO>>(DEPARTMENTS_ENDPOINT, {
        params: {page},
    });
    return {
        departments: data.content.map(toDepartment),
        page: data.page,
    };
};

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

// Walks every page so callers that need the complete list (e.g. a department
// picker) get all of them, not just the first server page.
export const getAllDepartments = async (): Promise<Department[]> => {
    const first = await getDepartments(0);
    const all = [...first.departments];
    for (let page = 1; page < first.page.totalPages; page += 1) {
        const next = await getDepartments(page);
        all.push(...next.departments);
    }
    return all;
};

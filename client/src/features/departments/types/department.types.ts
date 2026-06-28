// Domain types for the Departments feature.

export interface Department {
    id: string;
    name: string;
    description?: string | null;
}

export interface DepartmentPageMeta {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface DepartmentPage {
    departments: Department[];
    page: DepartmentPageMeta;
}

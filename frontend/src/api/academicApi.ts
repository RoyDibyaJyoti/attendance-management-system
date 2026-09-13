import { apiClient } from './client';
import {
  AcademicPeriodResponse,
  CreateAcademicPeriodRequest,
  CreateDepartmentRequest,
  CreateSectionRequest,
  CreateSubjectRequest,
  DepartmentResponse,
  SectionResponse,
  SubjectResponse,
} from '../types/academic';
import { PagedResponse } from '../types/api';

export const academicApi = {
  // Departments
  getDepartments: (includeInactive: boolean = false): Promise<DepartmentResponse[]> => {
    return apiClient<DepartmentResponse[]>('/academic/departments', {
      params: { includeInactive }
    });
  },
  createDepartment: (data: CreateDepartmentRequest): Promise<DepartmentResponse> => {
    return apiClient<DepartmentResponse>('/academic/departments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  deactivateDepartment: (id: string): Promise<DepartmentResponse> => {
    return apiClient<DepartmentResponse>(`/academic/departments/${id}/deactivate`, { method: 'PATCH' });
  },
  reactivateDepartment: (id: string): Promise<DepartmentResponse> => {
    return apiClient<DepartmentResponse>(`/academic/departments/${id}/reactivate`, { method: 'PATCH' });
  },

  // Academic Periods
  getPeriods: (): Promise<AcademicPeriodResponse[]> => {
    return apiClient<AcademicPeriodResponse[]>('/academic/periods');
  },
  createPeriod: (data: CreateAcademicPeriodRequest): Promise<AcademicPeriodResponse> => {
    return apiClient<AcademicPeriodResponse>('/academic/periods', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // Sections — backend REQUIRES periodId query param
  getSectionsByPeriod: (periodId: string, includeInactive: boolean = false): Promise<SectionResponse[]> => {
    return apiClient<SectionResponse[]>('/academic/sections', {
      params: { periodId, includeInactive },
    });
  },

  getSectionById: (sectionId: string): Promise<SectionResponse> => {
    return apiClient<SectionResponse>(`/academic/sections/${sectionId}`);
  },
  deactivateSection: (id: string): Promise<SectionResponse> => {
    return apiClient<SectionResponse>(`/academic/sections/${id}/deactivate`, { method: 'PATCH' });
  },
  reactivateSection: (id: string): Promise<SectionResponse> => {
    return apiClient<SectionResponse>(`/academic/sections/${id}/reactivate`, { method: 'PATCH' });
  },

  // Convenience: get sections across all periods
  getAllSections: async (): Promise<SectionResponse[]> => {
    const periods = await apiClient<AcademicPeriodResponse[]>('/academic/periods');
    if (periods.length === 0) return [];
    const results = await Promise.all(
      periods.map((p) =>
        apiClient<SectionResponse[]>('/academic/sections', { params: { periodId: p.id } })
      )
    );
    // Deduplicate by id
    const seen = new Set<string>();
    const all: SectionResponse[] = [];
    for (const arr of results) {
      for (const s of arr) {
        if (!seen.has(s.id)) {
          seen.add(s.id);
          all.push(s);
        }
      }
    }
    return all;
  },

  createSection: (data: CreateSectionRequest): Promise<SectionResponse> => {
    return apiClient<SectionResponse>('/academic/sections', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // Subjects
  getSubjects: (page: number = 0, size: number = 20, includeInactive: boolean = false): Promise<PagedResponse<SubjectResponse>> => {
    return apiClient<PagedResponse<SubjectResponse>>('/academic/subjects', {
      params: { page, size, includeInactive },
    });
  },
  getAllSubjects: async (): Promise<SubjectResponse[]> => {
    const res = await apiClient<PagedResponse<SubjectResponse>>('/academic/subjects', {
      params: { page: 0, size: 100 },
    });
    return res.content;
  },
  getSubjectById: (subjectId: string): Promise<SubjectResponse> => {
    return apiClient<SubjectResponse>(`/academic/subjects/${subjectId}`);
  },
  createSubject: (data: CreateSubjectRequest): Promise<SubjectResponse> => {
    return apiClient<SubjectResponse>('/academic/subjects', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  deactivateSubject: (id: string): Promise<SubjectResponse> => {
    return apiClient<SubjectResponse>(`/academic/subjects/${id}/deactivate`, { method: 'PATCH' });
  },
  reactivateSubject: (id: string): Promise<SubjectResponse> => {
    return apiClient<SubjectResponse>(`/academic/subjects/${id}/reactivate`, { method: 'PATCH' });
  },
};

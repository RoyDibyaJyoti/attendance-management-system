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
  getDepartments: (): Promise<DepartmentResponse[]> => {
    return apiClient<DepartmentResponse[]>('/academic/departments');
  },
  createDepartment: (data: CreateDepartmentRequest): Promise<DepartmentResponse> => {
    return apiClient<DepartmentResponse>('/academic/departments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
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
  getSectionsByPeriod: (periodId: string): Promise<SectionResponse[]> => {
    return apiClient<SectionResponse[]>('/academic/sections', {
      params: { periodId },
    });
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
  getSubjects: (page: number = 0, size: number = 20): Promise<PagedResponse<SubjectResponse>> => {
    return apiClient<PagedResponse<SubjectResponse>>('/academic/subjects', {
      params: { page, size },
    });
  },
  getAllSubjects: async (): Promise<SubjectResponse[]> => {
    const res = await apiClient<PagedResponse<SubjectResponse>>('/academic/subjects', {
      params: { page: 0, size: 100 },
    });
    return res.content;
  },
  createSubject: (data: CreateSubjectRequest): Promise<SubjectResponse> => {
    return apiClient<SubjectResponse>('/academic/subjects', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

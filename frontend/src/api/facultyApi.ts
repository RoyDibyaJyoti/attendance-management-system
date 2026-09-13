import { apiClient } from './client';
import { CreateFacultyRequest, FacultyResponse } from '../types/faculty';
import { PagedResponse } from '../types/api';

export const facultyApi = {
  getFaculty: (page: number = 0, size: number = 20, includeInactive: boolean = false): Promise<PagedResponse<FacultyResponse>> => {
    return apiClient<PagedResponse<FacultyResponse>>('/faculty', {
      params: { page, size, includeInactive },
    });
  },

  getAllFaculty: async (): Promise<FacultyResponse[]> => {
    const res = await apiClient<PagedResponse<FacultyResponse>>('/faculty', {
      params: { page: 0, size: 100 },
    });
    return res.content;
  },

  getFacultyById: (id: string): Promise<FacultyResponse> => {
    return apiClient<FacultyResponse>(`/faculty/${id}`);
  },

  createFaculty: (data: CreateFacultyRequest): Promise<FacultyResponse> => {
    return apiClient<FacultyResponse>('/faculty', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  deactivateFaculty: (id: string): Promise<FacultyResponse> => {
    return apiClient<FacultyResponse>(`/faculty/${id}/deactivate`, { method: 'PATCH' });
  },

  reactivateFaculty: (id: string): Promise<FacultyResponse> => {
    return apiClient<FacultyResponse>(`/faculty/${id}/reactivate`, { method: 'PATCH' });
  },
};

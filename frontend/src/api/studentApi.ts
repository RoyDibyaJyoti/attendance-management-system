import { apiClient } from './client';
import { CreateStudentRequest, StudentResponse, UpdateStudentRequest } from '../types/student';
import { PagedResponse } from '../types/api';

export const studentApi = {
  getStudents: (page: number = 0, size: number = 20, includeInactive: boolean = false): Promise<PagedResponse<StudentResponse>> => {
    return apiClient<PagedResponse<StudentResponse>>('/students', {
      params: { page, size, includeInactive },
    });
  },

  getAllStudents: async (): Promise<StudentResponse[]> => {
    const res = await apiClient<PagedResponse<StudentResponse>>('/students', {
      params: { page: 0, size: 100 },
    });
    return res.content;
  },

  getStudentById: (id: string): Promise<StudentResponse> => {
    return apiClient<StudentResponse>(`/students/${id}`);
  },

  getStudentByRegNo: (regNo: string): Promise<StudentResponse> => {
    return apiClient<StudentResponse>(`/students/registration/${regNo}`);
  },

  createStudent: (data: CreateStudentRequest): Promise<StudentResponse> => {
    return apiClient<StudentResponse>('/students', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateStudent: (id: string, data: UpdateStudentRequest): Promise<StudentResponse> => {
    return apiClient<StudentResponse>(`/students/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },

  deactivateStudent: (id: string): Promise<StudentResponse> => {
    return apiClient<StudentResponse>(`/students/${id}/deactivate`, { method: 'PATCH' });
  },

  reactivateStudent: (id: string): Promise<StudentResponse> => {
    return apiClient<StudentResponse>(`/students/${id}/reactivate`, { method: 'PATCH' });
  },
};

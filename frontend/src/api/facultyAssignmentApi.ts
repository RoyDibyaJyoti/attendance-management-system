import { apiClient } from './client';
import { PagedResponse } from '../types/api';

export interface FacultyAssignmentResponse {
  id: string;
  facultyId: string;
  subjectId: string;
  sectionId: string;
  academicPeriodId: string;
  assignmentStart: string;
  assignmentEnd: string | null;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface CreateFacultyAssignmentRequest {
  facultyId: string;
  subjectId: string;
  sectionId: string;
  academicPeriodId: string;
  assignmentStart: string;
  assignmentEnd?: string;
}

export const facultyAssignmentApi = {
  getAssignments: (facultyId?: string, sectionId?: string, page: number = 0, size: number = 20): Promise<PagedResponse<FacultyAssignmentResponse>> => {
    return apiClient<PagedResponse<FacultyAssignmentResponse>>('/assignments/faculty', {
      params: { facultyId, sectionId, page, size },
    });
  },

  assignFaculty: (data: CreateFacultyAssignmentRequest): Promise<FacultyAssignmentResponse> => {
    return apiClient<FacultyAssignmentResponse>('/assignments/faculty', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  endAssignment: (id: string, endDate: string): Promise<FacultyAssignmentResponse> => {
    return apiClient<FacultyAssignmentResponse>(`/assignments/faculty/${id}/end`, {
      method: 'PATCH',
      params: { endDate },
    });
  },
};

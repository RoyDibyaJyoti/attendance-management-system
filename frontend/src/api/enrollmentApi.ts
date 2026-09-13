import { apiClient } from './client';
import { EnrollmentResponse, EnrollStudentRequest, TransferStudentRequest } from '../types/enrollment';

export const enrollmentApi = {
  // POST /api/v1/enrollments — { studentId, sectionId, enrollmentStart, enrollmentEnd? }
  enrollStudent: (data: EnrollStudentRequest): Promise<EnrollmentResponse> => {
    return apiClient<EnrollmentResponse>('/enrollments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // POST /api/v1/enrollments/transfer — { studentId, fromSectionId, toSectionId, transferDate }
  transferStudent: (data: TransferStudentRequest): Promise<EnrollmentResponse> => {
    return apiClient<EnrollmentResponse>('/enrollments/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // GET /api/v1/enrollments/students/{studentId}
  getStudentEnrollments: (studentId: string): Promise<EnrollmentResponse[]> => {
    return apiClient<EnrollmentResponse[]>(`/enrollments/students/${studentId}`);
  },
};

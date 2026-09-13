import { apiClient } from './client';
import {
  OverallAttendanceSummaryResponse,
  ShortageProjectionResponse,
  StudentAttendanceOverviewResponse,
  SubjectAttendanceSummaryResponse,
} from '../types/calculation';

export const calculationApi = {
  getStudentOverview: (
    studentId: string,
    sectionId: string,
    policyId: string,
    periodId: string
  ): Promise<StudentAttendanceOverviewResponse> => {
    return apiClient<StudentAttendanceOverviewResponse>(
      `/students/${studentId}/attendance/summary`,
      {
        params: { sectionId, policyId, periodId },
      }
    );
  },

  getSubjectAttendance: (
    studentId: string,
    subjectId: string,
    sectionId: string,
    policyId: string,
    periodId: string
  ): Promise<SubjectAttendanceSummaryResponse> => {
    return apiClient<SubjectAttendanceSummaryResponse>(
      `/students/${studentId}/attendance/subjects/${subjectId}`,
      {
        params: { sectionId, policyId, periodId },
      }
    );
  },

  getOverallAttendance: (
    studentId: string,
    sectionId: string,
    policyId: string,
    periodId: string,
    strategy?: string
  ): Promise<OverallAttendanceSummaryResponse> => {
    return apiClient<OverallAttendanceSummaryResponse>(
      `/students/${studentId}/attendance/overall`,
      {
        params: { sectionId, policyId, periodId, strategy },
      }
    );
  },

  getShortageProjection: (
    studentId: string,
    subjectId: string,
    sectionId: string,
    policyId: string,
    periodId: string,
    projectedRemainingUnits: number = 10
  ): Promise<ShortageProjectionResponse> => {
    return apiClient<ShortageProjectionResponse>(
      `/students/${studentId}/attendance/subjects/${subjectId}/shortage`,
      {
        params: { sectionId, policyId, periodId, projectedRemainingUnits },
      }
    );
  },
};

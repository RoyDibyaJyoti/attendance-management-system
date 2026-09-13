import { apiClient } from './client';
import {
  AttendanceCorrectionResponse,
  CorrectAttendanceRecordRequest,
  RecordAttendanceBatchRequest,
  SessionAttendanceSummaryResponse,
} from '../types/attendance';

export const attendanceApi = {
  getSessionAttendance: (sessionId: string): Promise<SessionAttendanceSummaryResponse> => {
    return apiClient<SessionAttendanceSummaryResponse>(`/sessions/${sessionId}/attendance`);
  },

  recordAttendanceBatch: (
    sessionId: string,
    data: RecordAttendanceBatchRequest
  ): Promise<SessionAttendanceSummaryResponse> => {
    return apiClient<SessionAttendanceSummaryResponse>(`/sessions/${sessionId}/attendance`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  correctAttendanceRecord: (
    recordId: string,
    data: CorrectAttendanceRecordRequest
  ): Promise<AttendanceCorrectionResponse> => {
    return apiClient<AttendanceCorrectionResponse>(`/attendance/records/${recordId}/correction`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

import { SessionStatus, SessionType } from './session';

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'DUTY_LEAVE' | 'MEDICAL_LEAVE' | 'ON_DUTY';

export interface AttendanceRecordItemDto {
  studentId: string;
  status: AttendanceStatus;
}

export interface RecordAttendanceBatchRequest {
  records: AttendanceRecordItemDto[];
}

export interface AttendanceRecordResponse {
  id: string;
  sessionId: string;
  studentId: string;
  status: AttendanceStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface SessionAttendanceSummaryResponse {
  sessionId: string;
  subjectId: string;
  sectionId: string;
  sessionDate: string;
  sessionType: SessionType;
  conductedUnits: number;
  status: SessionStatus;
  totalRecords: number;
  presentCount: number;
  absentCount: number;
  otherCount: number;
  records: AttendanceRecordResponse[];
}

export interface CorrectAttendanceRecordRequest {
  newStatus: AttendanceStatus;
  reason: string;
  approverId: string; // Required by backend: facultyId
}

// Backend AttendanceCorrectionResponse: { recordId, sessionId, studentId, previousStatus, newStatus, reason, approverId, correctedAt }
export interface AttendanceCorrectionResponse {
  recordId: string;
  sessionId: string;
  studentId: string;
  previousStatus: AttendanceStatus;
  newStatus: AttendanceStatus;
  reason: string;
  approverId: string;
  correctedAt: string;
}

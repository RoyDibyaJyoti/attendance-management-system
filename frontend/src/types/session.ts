export type SessionType = 'THEORY' | 'LAB';
export type SessionStatus = 'SCHEDULED' | 'CONDUCTED' | 'CANCELLED' | 'RESCHEDULED';

export interface SessionResponse {
  id: string;
  subjectId: string;
  sectionId: string;
  conductedByFacultyId: string;
  sessionDate: string;
  sessionType: SessionType;
  plannedUnits: number;
  conductedUnits: number;
  status: SessionStatus;
  labGroupId?: string | null;
  replacedBySessionId?: string | null;
  version?: number;
}

export interface CreateSessionRequest {
  subjectId: string;
  sectionId: string;
  conductedByFacultyId: string;
  academicPeriodId: string;
  sessionDate: string;
  sessionType: SessionType;
  plannedUnits: number;
  labGroupId?: string | null;
}

export interface CancelSessionRequest {
  reason: string;
}

export interface RescheduleSessionRequest {
  newSessionDate: string;
  reason: string;
}

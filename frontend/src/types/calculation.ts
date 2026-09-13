export interface SubjectAttendanceSummaryResponse {
  studentId: string;
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  policyName: string;
  policyVersion: number;
  thresholdPercentage: number;
  conductedUnits: number;
  attendedUnits: number;
  attendancePercentage: number;
  classification: string;
  isAdequate: boolean;
  isShortage: boolean;
  shortageUnits: number;
  surplusUnits: number;
  missingRecordCount: number;
  isIncomplete: boolean;
}

export interface StudentAttendanceOverviewResponse {
  studentId: string;
  studentName: string;
  registrationNumber: string;
  academicPeriodId: string;
  subjects: SubjectAttendanceSummaryResponse[];
}

export interface OverallAttendanceSummaryResponse {
  studentId: string;
  policyName: string;
  aggregationStrategy: string;
  thresholdPercentage: number;
  overallPercentage: number;
  classification: string;
  isAdequate: boolean;
  isShortage: boolean;
  evaluatedCourseCount: number;
}

export interface ShortageProjectionResponse {
  studentId: string;
  subjectId: string;
  currentPercentage: number;
  thresholdPercentage: number;
  shortageUnits: number;
  surplusUnits: number;
  minimumAdditionalUnitsRequired: number;
  maximumAllowableAbsenceUnits: number;
  projectedRemainingUnits: number;
  isPossibleToRecover: boolean;
}

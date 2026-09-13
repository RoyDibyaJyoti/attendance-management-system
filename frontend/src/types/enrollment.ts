// EnrollmentResponse: matches backend exactly
// EnrollmentResponse: { id, studentId, sectionId, enrollmentStart, enrollmentEnd, status }
// NO academicPeriodId field — get it from SectionResponse.academicPeriodId

export interface EnrollmentResponse {
  id: string;
  studentId: string;
  sectionId: string;
  enrollmentStart: string;
  enrollmentEnd?: string | null;
  status: 'ACTIVE' | 'ENDED';
}

// Backend EnrollStudentRequest: { studentId, sectionId, enrollmentStart, enrollmentEnd? }
// NO academicPeriodId field
export interface EnrollStudentRequest {
  studentId: string;
  sectionId: string;
  enrollmentStart: string;
  enrollmentEnd?: string | null;
}

// Backend TransferStudentRequest: { studentId, fromSectionId, toSectionId, transferDate }
// NO academicPeriodId field
export interface TransferStudentRequest {
  studentId: string;
  fromSectionId: string;
  toSectionId: string;
  transferDate: string;
}

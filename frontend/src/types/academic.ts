export type CourseType = 'THEORY' | 'LABORATORY' | 'THEORY_INTEGRATED_LABORATORY';

export interface DepartmentResponse {
  id: string;
  code: string;
  name: string;
}

export interface CreateDepartmentRequest {
  code: string;
  name: string;
}

export interface AcademicPeriodResponse {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
}

export interface CreateAcademicPeriodRequest {
  name: string;
  startDate: string;
  endDate: string;
}

export interface SectionResponse {
  id: string;
  name: string;
  departmentId: string;
  academicPeriodId: string;
}

export interface CreateSectionRequest {
  name: string;
  departmentId: string;
  academicPeriodId: string;
}

export interface SubjectResponse {
  id: string;
  code: string;
  name: string;
  courseType: CourseType;
  creditHours: number;
}

export interface CreateSubjectRequest {
  code: string;
  name: string;
  courseType: CourseType;
  creditHours: number;
  departmentId: string;
}

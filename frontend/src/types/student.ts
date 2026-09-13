export interface StudentResponse {
  id: string;
  registrationNumber: string;
  name: string;
  email: string;
  departmentId: string;
  createdAt: string;
  isActive?: boolean;
}

export interface CreateStudentRequest {
  registrationNumber: string;
  name: string;
  email: string;
  departmentId: string;
}

export interface UpdateStudentRequest {
  email?: string;
  name?: string;
}

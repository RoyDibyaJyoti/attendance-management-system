export interface FacultyResponse {
  id: string;
  employeeId: string;
  name: string;
  email: string;
  departmentId: string;
  isActive?: boolean;
}

export interface CreateFacultyRequest {
  employeeId: string;
  name: string;
  email: string;
  departmentId: string;
}

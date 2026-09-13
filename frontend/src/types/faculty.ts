export interface FacultyResponse {
  id: string;
  employeeId: string;
  name: string;
  email: string;
  departmentId: string;
}

export interface CreateFacultyRequest {
  employeeId: string;
  name: string;
  email: string;
  departmentId: string;
}

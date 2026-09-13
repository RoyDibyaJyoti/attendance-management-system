export type UserRole = 'STUDENT' | 'FACULTY' | 'HOD_ADMIN';

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  username: string;
  email: string;
  role: UserRole;
  studentId: string | null;
  facultyId: string | null;
}

export interface UserProfileResponse {
  userId: string;
  username: string;
  email: string;
  role: UserRole;
  studentId: string | null;
  facultyId: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AuthState {
  token: string | null;
  user: UserProfileResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

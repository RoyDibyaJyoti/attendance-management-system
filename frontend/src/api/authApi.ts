import { apiClient } from './client';
import { ChangePasswordRequest, LoginRequest, LoginResponse, UserProfileResponse } from '../types/auth';

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return apiClient<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getMe: (): Promise<UserProfileResponse> => {
    return apiClient<UserProfileResponse>('/auth/me');
  },

  changePassword: (data: ChangePasswordRequest): Promise<void> => {
    return apiClient<void>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

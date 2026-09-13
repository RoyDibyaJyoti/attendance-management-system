import { apiClient } from './client';
import {
  AttendancePolicyResponse,
  CreateAttendancePolicyRequest,
  CreateOverallAttendancePolicyRequest,
  OverallAttendancePolicyResponse,
} from '../types/policy';

export const policyApi = {
  getPolicyById: (id: string): Promise<AttendancePolicyResponse> => {
    return apiClient<AttendancePolicyResponse>(`/policies/${id}`);
  },

  getPolicyVersions: (name: string): Promise<AttendancePolicyResponse[]> => {
    return apiClient<AttendancePolicyResponse[]>(`/policies/${encodeURIComponent(name)}/versions`);
  },

  createPolicy: (data: CreateAttendancePolicyRequest): Promise<AttendancePolicyResponse> => {
    return apiClient<AttendancePolicyResponse>('/policies', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  createOverallPolicy: (data: CreateOverallAttendancePolicyRequest): Promise<OverallAttendancePolicyResponse> => {
    return apiClient<OverallAttendancePolicyResponse>('/policies/overall', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

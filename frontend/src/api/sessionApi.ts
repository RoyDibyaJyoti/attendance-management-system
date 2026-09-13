import { apiClient } from './client';
import {
  CancelSessionRequest,
  CreateSessionRequest,
  RescheduleSessionRequest,
  SessionResponse,
} from '../types/session';
import { PagedResponse } from '../types/api';

export const sessionApi = {
  // Backend returns PagedResponse<SessionResponse>, use large page to get all
  getSessions: async (sectionId: string, page: number = 0, size: number = 50): Promise<SessionResponse[]> => {
    const res = await apiClient<PagedResponse<SessionResponse>>('/sessions', {
      params: { sectionId, page, size },
    });
    return res.content;
  },

  getSessionsPaged: (sectionId: string, page: number = 0, size: number = 20): Promise<PagedResponse<SessionResponse>> => {
    return apiClient<PagedResponse<SessionResponse>>('/sessions', {
      params: { sectionId, page, size },
    });
  },

  getSessionById: (id: string): Promise<SessionResponse> => {
    return apiClient<SessionResponse>(`/sessions/${id}`);
  },

  createSession: (data: CreateSessionRequest): Promise<SessionResponse> => {
    return apiClient<SessionResponse>('/sessions', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  cancelSession: (id: string, data: CancelSessionRequest): Promise<SessionResponse> => {
    return apiClient<SessionResponse>(`/sessions/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  rescheduleSession: (id: string, data: RescheduleSessionRequest): Promise<SessionResponse> => {
    return apiClient<SessionResponse>(`/sessions/${id}/reschedule`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { apiClient, ApiError, setAuthToken, clearAuthToken, getAuthToken } from '../api/client';

describe('API Client Infrastructure', () => {
  beforeEach(() => {
    clearAuthToken();
    vi.restoreAllMocks();
  });

  it('manages JWT token in localStorage correctly', () => {
    expect(getAuthToken()).toBeNull();
    setAuthToken('test-jwt-token');
    expect(getAuthToken()).toBe('test-jwt-token');
    clearAuthToken();
    expect(getAuthToken()).toBeNull();
  });

  it('normalizes HTTP 429 Rate Limiting with Retry-After header', async () => {
    const mockResponse = new Response(JSON.stringify({ message: 'Rate limit exceeded' }), {
      status: 429,
      headers: {
        'Retry-After': '45',
        'Content-Type': 'application/json',
      },
    });

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse);

    await expect(apiClient('/test')).rejects.toThrow(ApiError);

    try {
      await apiClient('/test');
    } catch (err: any) {
      expect(err.status).toBe(429);
      expect(err.code).toBe('RATE_LIMITED');
      expect(err.retryAfterSeconds).toBe(45);
    }
  });

  it('normalizes HTTP 400 Bad Request with field details', async () => {
    const errorBody = {
      timestamp: '2026-08-30T12:00:00Z',
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'Validation failed for request object',
      path: '/api/v1/students',
      details: [{ field: 'email', message: 'Email must be well-formed' }],
    };

    const mockResponse = new Response(JSON.stringify(errorBody), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    });

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse);

    try {
      await apiClient('/test');
      expect.fail('Should have thrown ApiError');
    } catch (err: any) {
      expect(err.status).toBe(400);
      expect(err.code).toBe('VALIDATION_FAILED');
      expect(err.details).toHaveLength(1);
      expect(err.details[0].field).toBe('email');
    }
  });
});

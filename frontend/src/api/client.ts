import { ApiErrorResponse } from '../types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiError extends Error {
  status: number;
  code: string;
  details?: ApiErrorResponse['details'];
  retryAfterSeconds?: number;

  constructor(status: number, message: string, code: string = 'UNKNOWN_ERROR', details?: ApiErrorResponse['details'], retryAfterSeconds?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export function getAuthToken(): string | null {
  return localStorage.getItem('amcs_token');
}

export function setAuthToken(token: string): void {
  localStorage.setItem('amcs_token', token);
}

export function clearAuthToken(): void {
  localStorage.removeItem('amcs_token');
}

interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined | null>;
}

export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers = {}, ...customConfig } = options;

  let url = `${BASE_URL}${endpoint}`;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        searchParams.append(key, String(val));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const token = getAuthToken();
  const defaultHeaders: Record<string, string> = {
    'Accept': 'application/json',
  };

  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  // Only set Content-Type to JSON if body is not FormData
  if (!(customConfig.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const config: RequestInit = {
    ...customConfig,
    headers: {
      ...defaultHeaders,
      ...headers,
    },
  };

  let response: Response;
  try {
    response = await fetch(url, config);
  } catch (err) {
    throw new ApiError(0, 'Network error. Please check your connection to the AMCS server.', 'NETWORK_ERROR');
  }

  if (response.status === 401) {
    clearAuthToken();
    window.dispatchEvent(new CustomEvent('amcs:auth_expired'));
    throw new ApiError(401, 'Your session has expired. Please log in again.', 'UNAUTHORIZED');
  }

  if (response.status === 429) {
    const retryAfter = response.headers.get('Retry-After');
    const retrySeconds = retryAfter ? parseInt(retryAfter, 10) : 60;
    throw new ApiError(
      429,
      `Rate limit exceeded. Please wait ${retrySeconds} seconds before trying again.`,
      'RATE_LIMITED',
      undefined,
      retrySeconds
    );
  }

  if (!response.ok) {
    let errorData: ApiErrorResponse | null = null;
    try {
      errorData = await response.json();
    } catch {
      // Not a JSON error
    }

    const message = errorData?.message || response.statusText || `Request failed with status ${response.status}`;
    const code = errorData?.code || `HTTP_${response.status}`;
    throw new ApiError(response.status, message, code, errorData?.details);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return null as unknown as T;
  }

  return response.json();
}

/**
 * Downloads a binary file (e.g. XLSX report, import template, error sheet)
 * using the browser's streaming download capabilities.
 */
export async function downloadFile(endpoint: string, fallbackFilename: string, params?: Record<string, string | number | boolean | undefined | null>): Promise<void> {
  let url = `${BASE_URL}${endpoint}`;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        searchParams.append(key, String(val));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const token = getAuthToken();
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, { headers });

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthToken();
      window.dispatchEvent(new CustomEvent('amcs:auth_expired'));
      throw new ApiError(401, 'Unauthorized', 'UNAUTHORIZED');
    }
    if (response.status === 403) {
      throw new ApiError(403, 'Access denied. You do not have permission to download this report.', 'FORBIDDEN');
    }
    throw new ApiError(response.status, `Failed to download file (${response.status})`, 'DOWNLOAD_FAILED');
  }

  const disposition = response.headers.get('Content-Disposition');
  let filename = fallbackFilename;
  if (disposition && disposition.includes('filename=')) {
    const match = disposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) {
      filename = match[1];
    }
  }

  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = downloadUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(downloadUrl);
  a.remove();
}

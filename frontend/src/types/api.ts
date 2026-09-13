export interface ValidationErrorDetail {
  field: string;
  rejectedValue?: unknown;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  details?: ValidationErrorDetail[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

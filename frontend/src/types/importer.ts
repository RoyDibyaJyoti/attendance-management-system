export type ImportType = 'STUDENTS' | 'SESSIONS' | 'ATTENDANCE_RECORDS';
export type ImportMode = 'FAIL_FAST' | 'PARTIAL_COMMIT';
export type ImportStatus = 'VALIDATING' | 'FAILED_VALIDATION' | 'STAGED' | 'COMMITTED' | 'DISCARDED';

export interface RowValidationErrorResponse {
  rowIndex: number;
  columnName: string;
  rejectedValue: string;
  errorCode: string;
  errorMessage: string;
}

export interface ImportJobResponse {
  jobId: string;
  importType: ImportType;
  importMode: ImportMode;
  status: ImportStatus;
  originalFilename: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: RowValidationErrorResponse[];
  createdByUserId: string;
  createdAt: string;
  committedAt?: string | null;
  discardedAt?: string | null;
  isCommittable: boolean;
}

export interface ImportCommitResponse {
  jobId: string;
  status: ImportStatus;
  committedRows: number;
  committedAt: string;
}

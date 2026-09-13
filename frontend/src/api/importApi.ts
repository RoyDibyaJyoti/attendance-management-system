import { apiClient, downloadFile } from './client';
import {
  ImportCommitResponse,
  ImportJobResponse,
  ImportMode,
  ImportType,
} from '../types/importer';

export const importApi = {
  // Download standard XLSX template
  downloadTemplate: (type: ImportType): Promise<void> => {
    return downloadFile(`/imports/templates/${type}`, `template-${type.toLowerCase()}.xlsx`);
  },

  // Upload and stage file
  uploadImport: (
    type: ImportType,
    file: File,
    mode: ImportMode = 'FAIL_FAST'
  ): Promise<ImportJobResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    return apiClient<ImportJobResponse>(`/imports/${type}`, {
      method: 'POST',
      body: formData,
      params: { mode },
    });
  },

  // Query staged job status & errors
  getJob: (jobId: string): Promise<ImportJobResponse> => {
    return apiClient<ImportJobResponse>(`/imports/jobs/${jobId}`);
  },

  // Download error report XLSX
  downloadErrorReport: (jobId: string): Promise<void> => {
    return downloadFile(`/imports/jobs/${jobId}/errors/download`, `errors-${jobId}.xlsx`);
  },

  // Two-Phase Commit
  commitJob: (jobId: string): Promise<ImportCommitResponse> => {
    return apiClient<ImportCommitResponse>(`/imports/jobs/${jobId}/commit`, {
      method: 'POST',
    });
  },

  // Two-Phase Discard
  discardJob: (jobId: string): Promise<void> => {
    return apiClient<void>(`/imports/jobs/${jobId}/discard`, {
      method: 'POST',
    });
  },
};

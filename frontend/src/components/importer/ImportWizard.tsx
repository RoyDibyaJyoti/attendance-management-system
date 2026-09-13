import React, { useState } from 'react';
import { importApi } from '../../api/importApi';
import { useToast } from '../../context/ToastContext';
import {
  ImportCommitResponse,
  ImportJobResponse,
  ImportMode,
  ImportType,
} from '../../types/importer';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Badge } from '../common/Badge';
import { Select } from '../common/Select';
import { LoadingSpinner } from '../common/LoadingSpinner';
import {
  FileSpreadsheet,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  Download,
  Trash2,
  Check,
  FileCheck,
  XCircle,
} from 'lucide-react';
import { ApiError } from '../../api/client';

interface ImportWizardProps {
  allowedTypes?: ImportType[];
}

export const ImportWizard: React.FC<ImportWizardProps> = ({
  allowedTypes = ['STUDENTS', 'SESSIONS', 'ATTENDANCE_RECORDS'],
}) => {
  const { showToast } = useToast();

  const [selectedType, setSelectedType] = useState<ImportType>(allowedTypes[0]);
  const [selectedMode, setSelectedMode] = useState<ImportMode>('FAIL_FAST');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // States
  const [isDownloadingTemplate, setIsDownloadingTemplate] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [stagedJob, setStagedJob] = useState<ImportJobResponse | null>(null);
  const [isCommitting, setIsCommitting] = useState(false);
  const [isDiscarding, setIsDiscarding] = useState(false);
  const [commitResult, setCommitResult] = useState<ImportCommitResponse | null>(null);

  const handleDownloadTemplate = async () => {
    setIsDownloadingTemplate(true);
    try {
      await importApi.downloadTemplate(selectedType);
      showToast(`Downloaded template for ${selectedType}.`, 'success');
    } catch (err) {
      showToast('Failed to download template.', 'error');
    } finally {
      setIsDownloadingTemplate(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setStagedJob(null);
      setCommitResult(null);
    }
  };

  const handleUploadAndStage = async () => {
    if (!selectedFile) {
      showToast('Please select a spreadsheet file (.xlsx) to upload.', 'warning');
      return;
    }

    setIsUploading(true);
    setStagedJob(null);
    setCommitResult(null);

    try {
      const job = await importApi.uploadImport(selectedType, selectedFile, selectedMode);
      setStagedJob(job);
      if (job.status === 'STAGED') {
        showToast(`Spreadsheet parsed: ${job.validRows} valid rows staged for commit.`, 'success');
      } else {
        showToast(`Validation found ${job.invalidRows} errors.`, 'warning');
      }
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to upload spreadsheet. Ensure file is a valid XLSX.', 'error');
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleCommit = async () => {
    if (!stagedJob) return;
    setIsCommitting(true);
    try {
      const res = await importApi.commitJob(stagedJob.jobId);
      setCommitResult(res);
      setStagedJob(null);
      setSelectedFile(null);
      showToast(`Successfully committed ${res.committedRows} records!`, 'success');
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to commit staged rows.', 'error');
      }
    } finally {
      setIsCommitting(false);
    }
  };

  const handleDiscard = async () => {
    if (!stagedJob) return;
    setIsDiscarding(true);
    try {
      await importApi.discardJob(stagedJob.jobId);
      showToast('Staged import job discarded.', 'info');
      setStagedJob(null);
      setSelectedFile(null);
    } catch (err: unknown) {
      showToast('Failed to discard job.', 'error');
    } finally {
      setIsDiscarding(false);
    }
  };

  const handleDownloadErrors = async () => {
    if (!stagedJob) return;
    try {
      await importApi.downloadErrorReport(stagedJob.jobId);
      showToast('Downloaded error report workbook.', 'success');
    } catch (err) {
      showToast('Failed to download error report.', 'error');
    }
  };

  return (
    <div className="space-y-6">
      {/* Step 1 & 2: Category & Template */}
      <Card title="Step 1: Select Import Category & Template" subtitle="Two-Phase atomic staging pipeline">
        <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between gap-4">
          <div className="w-full sm:max-w-xs">
            <Select
              label="Import Category"
              id="import-type"
              options={allowedTypes.map((t) => ({ value: t, label: t }))}
              value={selectedType}
              onChange={(e) => {
                setSelectedType(e.target.value as ImportType);
                setSelectedFile(null);
                setStagedJob(null);
              }}
            />
          </div>

          <Button
            variant="outline"
            onClick={handleDownloadTemplate}
            isLoading={isDownloadingTemplate}
            leftIcon={<Download className="w-4 h-4 text-indigo-600" />}
          >
            Download Official Template (.xlsx)
          </Button>
        </div>
      </Card>

      {/* Step 3: Upload & Mode */}
      <Card title="Step 2: Upload Spreadsheet" subtitle="Inspects security, validates constraints, and stages rows">
        <div className="space-y-4">
          <div className="max-w-xs">
            <Select
              label="Ingestion Mode"
              id="import-mode"
              options={[
                { value: 'FAIL_FAST', label: 'FAIL_FAST (Abort if any error occurs)' },
                { value: 'PARTIAL_COMMIT', label: 'PARTIAL_COMMIT (Stage valid rows, isolate errors)' },
              ]}
              value={selectedMode}
              onChange={(e) => setSelectedMode(e.target.value as ImportMode)}
            />
          </div>

          {/* File Input Box */}
          <div className="border-2 border-dashed border-slate-200 hover:border-indigo-400 rounded-2xl p-8 text-center transition-colors bg-slate-50/50">
            <input
              type="file"
              id="file-upload"
              accept=".xlsx"
              onChange={handleFileChange}
              className="hidden"
            />
            <label htmlFor="file-upload" className="cursor-pointer flex flex-col items-center">
              <UploadCloud className="w-12 h-12 text-indigo-500 mb-2" />
              <span className="text-sm font-semibold text-slate-800">
                {selectedFile ? selectedFile.name : 'Click to select XLSX spreadsheet'}
              </span>
              <span className="text-xs text-slate-400 mt-1">
                Supported: Microsoft Excel OpenXML (.xlsx). Max size: 25MB.
              </span>
            </label>
          </div>

          <div className="flex justify-end">
            <Button
              disabled={!selectedFile}
              isLoading={isUploading}
              onClick={handleUploadAndStage}
              leftIcon={<FileCheck className="w-4 h-4" />}
            >
              Upload & Validate Spreadsheet
            </Button>
          </div>
        </div>
      </Card>

      {/* Step 4: Staging Review */}
      {stagedJob && (
        <Card
          title="Step 3: Staging Verification & Two-Phase Decision"
          subtitle={`Job ID: ${stagedJob.jobId}`}
          className="border-indigo-200"
        >
          <div className="space-y-6">
            {/* Status & Metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Status</span>
                <Badge
                  variant={
                    stagedJob.status === 'STAGED'
                      ? 'success'
                      : stagedJob.status === 'FAILED_VALIDATION'
                      ? 'danger'
                      : 'warning'
                  }
                  size="sm"
                  className="mt-1"
                >
                  {stagedJob.status}
                </Badge>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Total Rows</span>
                <span className="text-lg font-bold text-slate-900 mt-0.5 block">{stagedJob.totalRows}</span>
              </div>

              <div className="p-3.5 bg-emerald-50 rounded-xl border border-emerald-100 text-center">
                <span className="text-[10px] uppercase font-bold text-emerald-600 block">Valid Rows</span>
                <span className="text-lg font-bold text-emerald-700 mt-0.5 block">{stagedJob.validRows}</span>
              </div>

              <div className="p-3.5 bg-rose-50 rounded-xl border border-rose-100 text-center">
                <span className="text-[10px] uppercase font-bold text-rose-600 block">Invalid Rows</span>
                <span className="text-lg font-bold text-rose-700 mt-0.5 block">{stagedJob.invalidRows}</span>
              </div>
            </div>

            {/* Error Diagnostics Table */}
            {stagedJob.errors && stagedJob.errors.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-rose-700 flex items-center gap-1.5">
                    <AlertTriangle className="w-4 h-4" />
                    Validation Diagnostics ({stagedJob.errors.length})
                  </h4>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleDownloadErrors}
                    leftIcon={<Download className="w-3.5 h-3.5 text-rose-600" />}
                  >
                    Download Error Workbook (.xlsx)
                  </Button>
                </div>

                <div className="border border-rose-100 rounded-xl overflow-x-auto max-h-56">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-rose-50/80 text-rose-900 font-semibold sticky top-0">
                      <tr>
                        <th className="px-3 py-2">Row</th>
                        <th className="px-3 py-2">Column</th>
                        <th className="px-3 py-2">Rejected Value</th>
                        <th className="px-3 py-2">Error Code</th>
                        <th className="px-3 py-2">Diagnostic Message</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-rose-50 text-slate-700">
                      {stagedJob.errors.map((err, i) => (
                        <tr key={i} className="hover:bg-rose-50/30">
                          <td className="px-3 py-1.5 font-mono font-bold text-rose-600">{err.rowIndex}</td>
                          <td className="px-3 py-1.5 font-medium">{err.columnName}</td>
                          <td className="px-3 py-1.5 font-mono text-slate-500">{err.rejectedValue || '—'}</td>
                          <td className="px-3 py-1.5 font-mono text-slate-600">{err.errorCode}</td>
                          <td className="px-3 py-1.5 text-rose-700">{err.errorMessage}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Two-Phase Decision Buttons */}
            <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-slate-100">
              <Button
                variant="ghost"
                className="text-slate-500 hover:text-rose-600"
                onClick={handleDiscard}
                isLoading={isDiscarding}
                leftIcon={<Trash2 className="w-4 h-4" />}
              >
                Discard Staged Job
              </Button>

              <div className="flex items-center gap-3">
                <Button
                  disabled={!stagedJob.isCommittable}
                  isLoading={isCommitting}
                  onClick={handleCommit}
                  leftIcon={<Check className="w-4 h-4" />}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  Commit {stagedJob.validRows} Records to Primary DB
                </Button>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Success Notification */}
      {commitResult && (
        <div className="p-5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-950 flex items-start gap-4 animate-in fade-in">
          <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0 mt-0.5" />
          <div className="text-sm">
            <h4 className="font-bold">Two-Phase Commit Succeeded</h4>
            <p className="text-xs text-emerald-800 mt-1">
              Successfully inserted and validated <strong>{commitResult.committedRows}</strong> rows into the primary institutional database.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

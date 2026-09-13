import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { attendanceApi } from '../../api/attendanceApi';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { AttendanceStatus } from '../../types/attendance';
import { History, ShieldCheck, CheckCircle2, AlertCircle } from 'lucide-react';
import { ApiError } from '../../api/client';

export const FacultyCorrectionsPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [recordId, setRecordId] = useState('');
  const [newStatus, setNewStatus] = useState<AttendanceStatus>('PRESENT');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastCorrection, setLastCorrection] = useState<{
    id: string;
    previous: string;
    newStatus: string;
    reason: string;
  } | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recordId.trim() || !reason.trim() || !user) return;

    if (reason.trim().length < 5) {
      showToast('Justification reason must be at least 5 characters.', 'warning');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await attendanceApi.correctAttendanceRecord(recordId.trim(), {
        newStatus,
        reason: reason.trim(),
        approverId: user.facultyId || user.userId,
      });

      showToast('Correction successfully recorded in audit log.', 'success');
      setLastCorrection({
        id: res.recordId,
        previous: res.previousStatus,
        newStatus: res.newStatus,
        reason: res.reason,
      });
      setRecordId('');
      setReason('');
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to apply attendance correction. Check Record ID.', 'error');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Attendance Audit Corrections</h1>
        <p className="text-sm text-slate-500 mt-1">
          Submit formal corrections to locked attendance records with mandatory audit justification
        </p>
      </div>

      <div className="p-4 rounded-xl bg-indigo-50 border border-indigo-200 text-indigo-900 text-sm flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
        <div>
          <h4 className="font-semibold text-indigo-950">Immutable Audit Trail Enforced</h4>
          <p className="text-xs text-indigo-700 mt-0.5">
            Every correction permanently archives previous status, new status, timestamp, faculty ID, and justification reason in compliance with university audit standards.
          </p>
        </div>
      </div>

      <Card title="Record Correction Form" subtitle="Requires valid attendance record UUID">
        <form onSubmit={handleSubmit} className="space-y-4 max-w-xl">
          <Input
            label="Attendance Record UUID"
            id="record-id"
            type="text"
            placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
            value={recordId}
            onChange={(e) => setRecordId(e.target.value)}
            helperText="Obtain the record UUID from the session roll-call sheet or register"
            required
          />

          <Select
            label="Adjusted Attendance Status"
            id="correction-status"
            options={[
              { value: 'PRESENT', label: 'PRESENT' },
              { value: 'ABSENT', label: 'ABSENT' },
              { value: 'DUTY_LEAVE', label: 'DUTY LEAVE (Official Duty)' },
              { value: 'MEDICAL_LEAVE', label: 'MEDICAL LEAVE (Documented)' },
              { value: 'ON_DUTY', label: 'ON DUTY (Faculty Sanctioned)' },
            ]}
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value as AttendanceStatus)}
            required
          />

          <div>
            <label htmlFor="correction-reason" className="block text-xs font-semibold uppercase tracking-wider text-slate-700 mb-1.5">
              Audited Justification Reason <span className="text-rose-500">*</span>
            </label>
            <textarea
              id="correction-reason"
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. Medical certificate verified by department head on Aug 30"
              className="block w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              required
            />
            <p className="text-xs text-slate-400 mt-1">Minimum 5 characters. Retained permanently.</p>
          </div>

          <div className="pt-2">
            <Button type="submit" isLoading={isSubmitting}>
              Apply Audited Correction
            </Button>
          </div>
        </form>
      </Card>

      {lastCorrection && (
        <div className="p-5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-950 flex items-start gap-4">
          <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0 mt-0.5" />
          <div className="text-sm">
            <h4 className="font-bold">Correction Successfully Logged</h4>
            <p className="text-xs text-emerald-800 mt-1">
              Audit ID: <span className="font-mono">{lastCorrection.id}</span>
            </p>
            <p className="text-xs text-emerald-800 mt-0.5">
              Status changed from <strong>{lastCorrection.previous}</strong> to <strong>{lastCorrection.newStatus}</strong>.
            </p>
            <p className="text-xs text-emerald-800 mt-0.5 italic">
              "{lastCorrection.reason}"
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiFetch, SessionRosterResponse, RosterStudent } from '../api';
import {
  ArrowLeft,
  CheckCircle,
  XCircle,
  Search,
  Check,
  X,
  Lock,
  AlertCircle,
  Send,
  Users,
  Calendar,
  BookOpen,
  FlaskConical,
  RotateCcw,
} from 'lucide-react';

export const TakeAttendanceView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [rosterData, setRosterData] = useState<SessionRosterResponse | null>(null);
  const [attendance, setAttendance] = useState<Record<number, 'PRESENT' | 'ABSENT'>>({});
  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState<'ALL' | 'PRESENT' | 'ABSENT'>('ALL');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    loadRoster();
  }, [id]);

  const loadRoster = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<SessionRosterResponse>(`/sessions/${id}/roster`);
      setRosterData(data);

      // Initialize attendance map from loaded students
      const initialMap: Record<number, 'PRESENT' | 'ABSENT'> = {};
      data.students.forEach((s) => {
        initialMap[s.studentId] = s.status === 'ABSENT' ? 'ABSENT' : 'PRESENT';
      });
      setAttendance(initialMap);
    } catch (err: any) {
      setError(err.message || 'Failed to load session roster');
    } finally {
      setLoading(false);
    }
  };

  const isLocked = rosterData?.status === 'SUBMITTED';

  // Toggle single student
  const toggleStudent = (studentId: number) => {
    if (isLocked) return;
    setAttendance((prev) => ({
      ...prev,
      [studentId]: prev[studentId] === 'PRESENT' ? 'ABSENT' : 'PRESENT',
    }));
  };

  // Mark all present
  const handleMarkAllPresent = () => {
    if (isLocked || !rosterData) return;
    const updated: Record<number, 'PRESENT' | 'ABSENT'> = {};
    rosterData.students.forEach((s) => {
      updated[s.studentId] = 'PRESENT';
    });
    setAttendance(updated);
  };

  // Mark all absent
  const handleMarkAllAbsent = () => {
    if (isLocked || !rosterData) return;
    const updated: Record<number, 'PRESENT' | 'ABSENT'> = {};
    rosterData.students.forEach((s) => {
      updated[s.studentId] = 'ABSENT';
    });
    setAttendance(updated);
  };

  // Tally counts
  const totalStudents = rosterData?.students.length || 0;
  const presentCount = Object.values(attendance).filter((s) => s === 'PRESENT').length;
  const absentCount = Object.values(attendance).filter((s) => s === 'ABSENT').length;
  const attendanceRate = totalStudents > 0 ? ((presentCount / totalStudents) * 100).toFixed(1) : '0';

  // Filtered students list with guaranteed uniqueness
  const filteredStudents = useMemo(() => {
    if (!rosterData) return [];
    return rosterData.students.filter((s) => {
      const matchesSearch =
        s.registrationNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.name.toLowerCase().includes(searchTerm.toLowerCase());

      const currentStatus = attendance[s.studentId] || 'PRESENT';
      const matchesFilter =
        filterMode === 'ALL' ||
        (filterMode === 'PRESENT' && currentStatus === 'PRESENT') ||
        (filterMode === 'ABSENT' && currentStatus === 'ABSENT');

      return matchesSearch && matchesFilter;
    });
  }, [rosterData, searchTerm, filterMode, attendance]);

  // Submit attendance to backend
  const handleSubmit = async () => {
    if (!rosterData || isLocked) return;
    setSubmitting(true);
    setError(null);

    // Build payload ensuring each student ID appears exactly once
    const uniqueIds = Array.from(new Set(rosterData.students.map((s) => s.studentId)));
    const records = uniqueIds.map((studentId) => ({
      studentId,
      status: attendance[studentId] || 'PRESENT',
    }));

    try {
      await apiFetch(`/attendance/sessions/${id}/submit`, {
        method: 'POST',
        body: JSON.stringify({ records }),
      });
      setShowConfirmModal(false);
      setSuccessMessage('Attendance has been submitted successfully and permanently recorded.');
      // Reload roster to reflect SUBMITTED state
      await loadRoster();
    } catch (err: any) {
      setError(err.message || 'Submission failed');
      setShowConfirmModal(false);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 text-center text-slate-500">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="font-semibold text-sm">Loading session roster...</p>
      </div>
    );
  }

  if (!rosterData) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12 text-center">
        <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-slate-900">Session Not Found</h2>
        <p className="text-sm text-slate-500 mt-1 mb-4">
          The requested class session could not be found or you do not have permission to view it.
        </p>
        <button
          onClick={() => navigate('/')}
          className="px-4 py-2 bg-slate-900 text-white rounded-xl text-xs font-semibold"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const isLab = rosterData.subjectType === 'LAB';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6 pb-24">
      {/* Back button and Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 rounded-xl border border-slate-200 bg-white text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                {rosterData.subjectCode} — {rosterData.subjectName}
              </h1>
              <span
                className={`text-xs font-bold px-2 py-0.5 rounded-full inline-flex items-center gap-1 ${
                  isLab ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                }`}
              >
                {isLab ? <FlaskConical className="w-3 h-3" /> : <BookOpen className="w-3 h-3" />}
                {isLab ? `LAB (${rosterData.labBatchName})` : 'THEORY'}
              </span>
            </div>
            <div className="flex items-center gap-4 text-xs text-slate-500 mt-1">
              <span className="flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-slate-400" />
                {rosterData.sessionDate}
              </span>
              <span className="flex items-center gap-1">
                <Users className="w-3.5 h-3.5 text-slate-400" />
                Section {rosterData.sectionName}
              </span>
              <span>Faculty: {rosterData.facultyName}</span>
            </div>
          </div>
        </div>

        {/* Status Badge */}
        <div>
          {isLocked ? (
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold">
              <Lock className="w-3.5 h-3.5" />
              SUBMITTED (Locked)
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-50 text-amber-700 border border-amber-200 text-xs font-bold animate-pulse">
              ● OPEN (In Progress)
            </span>
          )}
        </div>
      </div>

      {/* Messages */}
      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-xs font-semibold text-emerald-700 underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Lock Notice */}
      {isLocked && (
        <div className="p-4 rounded-xl bg-slate-100 border border-slate-200 text-slate-700 text-xs flex items-center gap-2">
          <Lock className="w-4 h-4 text-slate-500" />
          <span>
            Attendance has been submitted and locked. Records cannot be edited.
          </span>
        </div>
      )}

      {/* Real-time Counter Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="p-3 bg-slate-50 rounded-xl text-center">
          <span className="text-xs text-slate-500 font-medium uppercase tracking-wider block">
            Roster Size
          </span>
          <span className="text-2xl font-black text-slate-900">{totalStudents}</span>
        </div>

        <div className="p-3 bg-emerald-50/70 border border-emerald-100 rounded-xl text-center">
          <span className="text-xs text-emerald-700 font-semibold uppercase tracking-wider block">
            Present
          </span>
          <span className="text-2xl font-black text-emerald-600">{presentCount}</span>
        </div>

        <div className="p-3 bg-rose-50/70 border border-rose-100 rounded-xl text-center">
          <span className="text-xs text-rose-700 font-semibold uppercase tracking-wider block">
            Absent
          </span>
          <span className="text-2xl font-black text-rose-600">{absentCount}</span>
        </div>

        <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-xl text-center">
          <span className="text-xs text-indigo-700 font-semibold uppercase tracking-wider block">
            Turnout Rate
          </span>
          <span className="text-2xl font-black text-indigo-600">{attendanceRate}%</span>
        </div>
      </div>

      {/* Action Controls & Fast Workflow Buttons */}
      {!isLocked && (
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          {/* Quick Mark Buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={handleMarkAllPresent}
              className="flex-1 sm:flex-initial px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-all shadow-sm shadow-emerald-200"
            >
              <Check className="w-4 h-4" />
              MARK ALL PRESENT
            </button>

            <button
              onClick={handleMarkAllAbsent}
              className="flex-1 sm:flex-initial px-4 py-2 rounded-xl bg-rose-100 hover:bg-rose-200 text-rose-800 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <X className="w-4 h-4" />
              MARK ALL ABSENT
            </button>
          </div>

          <div className="text-xs text-slate-500 font-medium sm:text-right">
            <span>Fast Roll-Call: Click any student's button to toggle Present / Absent.</span>
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by registration number or student name..."
            className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
          />
        </div>

        {/* Filter Pills */}
        <div className="inline-flex p-1 bg-slate-200/70 rounded-xl text-xs font-semibold text-slate-700">
          <button
            onClick={() => setFilterMode('ALL')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterMode === 'ALL' ? 'bg-white text-slate-900 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            All ({totalStudents})
          </button>
          <button
            onClick={() => setFilterMode('PRESENT')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterMode === 'PRESENT'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'hover:text-emerald-700'
            }`}
          >
            Present ({presentCount})
          </button>
          <button
            onClick={() => setFilterMode('ABSENT')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              filterMode === 'ABSENT' ? 'bg-rose-600 text-white shadow-sm' : 'hover:text-rose-700'
            }`}
          >
            Absent ({absentCount})
          </button>
        </div>
      </div>

      {/* Student Roster Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-700 uppercase font-bold tracking-wider border-b border-slate-200">
              <tr>
                <th className="py-3.5 px-4 w-14 text-center">S.No</th>
                <th className="py-3.5 px-4 w-44">Registration No</th>
                <th className="py-3.5 px-4">Student Name</th>
                {isLab && <th className="py-3.5 px-4 w-32">Lab Batch</th>}
                <th className="py-3.5 px-4 w-44 text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={isLab ? 5 : 4} className="py-8 text-center text-slate-400">
                    No students found matching current search/filter.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((s, idx) => {
                  const currentStatus = attendance[s.studentId] || 'PRESENT';
                  const isPresent = currentStatus === 'PRESENT';

                  return (
                    <tr
                      key={s.studentId}
                      className={`transition-colors ${
                        !isLocked ? 'hover:bg-slate-50/80 cursor-pointer' : ''
                      } ${!isPresent ? 'bg-rose-50/30' : ''}`}
                      onClick={() => !isLocked && toggleStudent(s.studentId)}
                    >
                      <td className="py-3 px-4 text-center text-slate-400 font-mono">
                        {idx + 1}
                      </td>
                      <td className="py-3 px-4 font-mono font-semibold text-slate-900">
                        {s.registrationNumber}
                      </td>
                      <td className="py-3 px-4 font-medium text-slate-800">
                        {s.name}
                      </td>
                      {isLab && (
                        <td className="py-3 px-4 text-slate-600">
                          <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-medium text-[11px]">
                            {s.labBatchName || 'Batch 1'}
                          </span>
                        </td>
                      )}
                      <td className="py-3 px-4 text-center" onClick={(e) => e.stopPropagation()}>
                        {isLocked ? (
                          <span
                            className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold ${
                              isPresent
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-rose-100 text-rose-800'
                            }`}
                          >
                            {isPresent ? <Check className="w-3.5 h-3.5" /> : <X className="w-3.5 h-3.5" />}
                            {currentStatus}
                          </span>
                        ) : (
                          <div className="inline-flex rounded-xl p-0.5 bg-slate-100 border border-slate-200">
                            <button
                              type="button"
                              onClick={() =>
                                setAttendance((prev) => ({ ...prev, [s.studentId]: 'PRESENT' }))
                              }
                              className={`px-3 py-1 rounded-lg text-xs font-bold flex items-center gap-1 transition-all ${
                                isPresent
                                  ? 'bg-emerald-600 text-white shadow-sm'
                                  : 'text-slate-500 hover:text-slate-800'
                              }`}
                            >
                              <Check className="w-3 h-3" />
                              P
                            </button>
                            <button
                              type="button"
                              onClick={() =>
                                setAttendance((prev) => ({ ...prev, [s.studentId]: 'ABSENT' }))
                              }
                              className={`px-3 py-1 rounded-lg text-xs font-bold flex items-center gap-1 transition-all ${
                                !isPresent
                                  ? 'bg-rose-600 text-white shadow-sm'
                                  : 'text-slate-500 hover:text-slate-800'
                              }`}
                            >
                              <X className="w-3 h-3" />
                              A
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Sticky Bottom Bar for Submission */}
      {!isLocked && (
        <div className="fixed bottom-0 inset-x-0 bg-white/95 backdrop-blur-md border-t border-slate-200 shadow-2xl py-3.5 px-4 sm:px-8 z-30">
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="flex items-center gap-4 text-xs">
              <span className="font-semibold text-slate-800">
                Ready to submit:
              </span>
              <span className="text-emerald-700 font-bold">
                {presentCount} Present
              </span>
              <span className="text-slate-300">•</span>
              <span className="text-rose-700 font-bold">
                {absentCount} Absent
              </span>
              <span className="text-slate-300">•</span>
              <span className="text-slate-600">
                Total: {totalStudents} Students
              </span>
            </div>

            <div className="flex items-center gap-3 w-full sm:w-auto">
              <button
                type="button"
                onClick={() => setShowConfirmModal(true)}
                disabled={submitting || totalStudents === 0}
                className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md shadow-indigo-100 disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                Submit Attendance
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-2">
              <Send className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="text-lg font-bold text-slate-900">
                Submit Class Attendance?
              </h3>
              <p className="text-xs text-slate-500 mt-1">
                Once submitted, this attendance session will be locked and saved permanently into the institutional database.
              </p>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-xl space-y-1.5 text-xs text-slate-700">
              <div className="flex justify-between">
                <span>Subject:</span>
                <strong className="text-slate-900">{rosterData.subjectCode} ({rosterData.subjectType})</strong>
              </div>
              <div className="flex justify-between">
                <span>Date:</span>
                <strong className="text-slate-900">{rosterData.sessionDate}</strong>
              </div>
              <div className="flex justify-between">
                <span>Present Students:</span>
                <strong className="text-emerald-700">{presentCount}</strong>
              </div>
              <div className="flex justify-between">
                <span>Absent Students:</span>
                <strong className="text-rose-700">{absentCount}</strong>
              </div>
              <div className="flex justify-between border-t border-slate-200 pt-1 font-semibold">
                <span>Total Enrolled:</span>
                <span>{totalStudents}</span>
              </div>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                disabled={submitting}
                className="flex-1 py-2.5 px-4 rounded-xl border border-slate-300 text-slate-700 hover:bg-slate-50 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={submitting}
                className="flex-1 py-2.5 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-md shadow-indigo-100 disabled:opacity-50"
              >
                {submitting ? 'Submitting...' : 'Confirm & Submit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

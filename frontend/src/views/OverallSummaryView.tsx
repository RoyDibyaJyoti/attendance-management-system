import React, { useState, useEffect, useMemo } from 'react';
import { apiFetch, OverallSummaryResponse, Section } from '../api';
import {
  ShieldCheck,
  Search,
  Users,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  TrendingUp,
} from 'lucide-react';

export const OverallSummaryView: React.FC = () => {
  const [sections, setSections] = useState<Section[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<number | ''>('');
  const [summaryData, setSummaryData] = useState<OverallSummaryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'GOOD' | 'SHORTAGE'>('ALL');

  useEffect(() => {
    loadSections();
  }, []);

  const loadSections = async () => {
    try {
      const data = await apiFetch<Section[]>('/sections');
      setSections(data);
      if (data.length > 0) setSelectedSectionId(data[0].id);
    } catch (err: any) {
      setError('Failed to load sections: ' + err.message);
    }
  };

  useEffect(() => {
    if (selectedSectionId) {
      loadOverallSummary();
    }
  }, [selectedSectionId]);

  const loadOverallSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<OverallSummaryResponse>(
        `/attendance/summary/overall?sectionId=${selectedSectionId}`
      );
      setSummaryData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load overall summary');
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = useMemo(() => {
    if (!summaryData) return [];
    return summaryData.students.filter((s) => {
      const matchesSearch =
        s.registrationNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.name.toLowerCase().includes(searchTerm.toLowerCase());

      const isShortage = s.percentage < 75;
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'GOOD' && !isShortage) ||
        (statusFilter === 'SHORTAGE' && isShortage);

      return matchesSearch && matchesStatus;
    });
  }, [summaryData, searchTerm, statusFilter]);

  const shortageCount = (summaryData?.warningCount || 0) + (summaryData?.criticalCount || 0);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            Institutional Compliance & Eligibility
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Overall Attendance Summary
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total attendance aggregation per student. University 75% minimum threshold monitoring.
          </p>
        </div>

        {/* Section selector */}
        <div>
          <select
            value={selectedSectionId}
            onChange={(e) => setSelectedSectionId(Number(e.target.value))}
            className="py-2 px-3 bg-white border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 shadow-sm"
          >
            {sections.map((s) => (
              <option key={s.id} value={s.id}>
                Section: {s.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Enrolled Students
            </span>
            <span className="text-xl font-black text-slate-900">
              {summaryData?.totalStudents || 0}
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Cumulative Turnout
            </span>
            <span className="text-xl font-black text-blue-600">
              {summaryData?.averageAttendancePercentage || 0}%
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Eligible (≥75%)
            </span>
            <span className="text-xl font-black text-emerald-600">
              {summaryData?.goodCount || 0}
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Shortage (&lt;75%)
            </span>
            <span className="text-xl font-black text-rose-600">{shortageCount}</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative max-w-md flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by roll number or student name..."
            className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
          />
        </div>

        <div className="inline-flex p-1 bg-slate-200/70 rounded-xl text-xs font-semibold text-slate-700">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              statusFilter === 'ALL' ? 'bg-white text-slate-900 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            All Students ({summaryData?.totalStudents || 0})
          </button>
          <button
            onClick={() => setStatusFilter('GOOD')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              statusFilter === 'GOOD'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'hover:text-emerald-700'
            }`}
          >
            Eligible ≥75% ({summaryData?.goodCount || 0})
          </button>
          <button
            onClick={() => setStatusFilter('SHORTAGE')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              statusFilter === 'SHORTAGE' ? 'bg-rose-600 text-white shadow-sm' : 'hover:text-rose-700'
            }`}
          >
            Shortage &lt;75% ({shortageCount})
          </button>
        </div>
      </div>

      {/* Overall Table */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Aggregating attendance records...</p>
        </div>
      ) : !summaryData || summaryData.students.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500">
          <ShieldCheck className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-bold text-slate-800 text-sm">No Attendance Recorded</h3>
          <p className="text-xs text-slate-500 mt-1">
            Overall summary will appear once sessions are held and attendance is submitted.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto max-h-[700px]">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-slate-50 text-slate-700 sticky top-0 z-20 shadow-xs border-b border-slate-200">
                <tr>
                  <th className="py-3 px-3 w-12 text-center border-r border-slate-200">S.No</th>
                  <th className="py-3 px-3 w-36 border-r border-slate-200">Registration No</th>
                  <th className="py-3 px-3 min-w-[200px] border-r border-slate-200">Student Name</th>
                  <th className="py-3 px-3 w-24 border-r border-slate-200">Lab Batch</th>
                  <th className="py-3 px-3 text-center w-24 border-r border-slate-200 text-emerald-700">
                    Attended
                  </th>
                  <th className="py-3 px-3 text-center w-20 border-r border-slate-200 text-rose-700">
                    Absent
                  </th>
                  <th className="py-3 px-3 text-center w-24 border-r border-slate-200">
                    Total Classes
                  </th>
                  <th className="py-3 px-3 min-w-[150px] border-r border-slate-200">
                    Progress
                  </th>
                  <th className="py-3 px-3 text-center w-28 font-bold">Standing</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredStudents.map((st, idx) => {
                  const pct = st.percentage;
                  const isGood = st.status === 'GOOD';
                  const isWarning = st.status === 'WARNING';

                  return (
                    <tr key={st.studentId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-2.5 px-3 text-center text-slate-400 border-r border-slate-200">
                        {idx + 1}
                      </td>
                      <td className="py-2.5 px-3 font-semibold text-slate-900 border-r border-slate-200">
                        {st.registrationNumber}
                      </td>
                      <td className="py-2.5 px-3 font-sans font-medium text-slate-800 border-r border-slate-200">
                        {st.name}
                      </td>
                      <td className="py-2.5 px-3 font-sans text-slate-500 text-[10px] border-r border-slate-200">
                        {st.labBatchName || '—'}
                      </td>
                      <td className="py-2.5 px-3 text-center font-bold text-emerald-700 border-r border-slate-200">
                        {st.attendedClasses}
                      </td>
                      <td className="py-2.5 px-3 text-center font-bold text-rose-700 border-r border-slate-200">
                        {st.absentClasses}
                      </td>
                      <td className="py-2.5 px-3 text-center text-slate-700 border-r border-slate-200">
                        {st.totalClasses}
                      </td>

                      {/* Percentage Bar */}
                      <td className="py-2.5 px-3 border-r border-slate-200 font-sans">
                        <div className="flex items-center gap-2">
                          <div className="flex-1 h-2 rounded-full bg-slate-100 overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all ${
                                isGood
                                  ? 'bg-emerald-500'
                                  : isWarning
                                  ? 'bg-amber-500'
                                  : 'bg-rose-500'
                              }`}
                              style={{ width: `${Math.min(100, pct)}%` }}
                            />
                          </div>
                          <span className="font-bold text-[11px] w-12 text-right">
                            {pct}%
                          </span>
                        </div>
                      </td>

                      {/* Standing Status Badge */}
                      <td className="py-2.5 px-3 text-center font-sans">
                        {isGood ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                            Eligible
                          </span>
                        ) : isWarning ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                            Warning
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                            Critical
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

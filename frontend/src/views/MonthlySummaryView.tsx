import React, { useState, useEffect, useMemo } from 'react';
import { apiFetch, MonthlySummaryResponse, Section } from '../api';
import {
  FileSpreadsheet,
  Search,
  Users,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  AlertCircle,
} from 'lucide-react';

export const MonthlySummaryView: React.FC = () => {
  const [sections, setSections] = useState<Section[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<number | ''>('');
  const [selectedMonth, setSelectedMonth] = useState<number>(9); // September by default
  const [selectedYear, setSelectedYear] = useState<number>(2026);
  const [summaryData, setSummaryData] = useState<MonthlySummaryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

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
      loadMonthlySummary();
    }
  }, [selectedSectionId, selectedMonth, selectedYear]);

  const loadMonthlySummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<MonthlySummaryResponse>(
        `/attendance/summary/monthly?sectionId=${selectedSectionId}&month=${selectedMonth}&year=${selectedYear}`
      );
      setSummaryData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load monthly summary');
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = useMemo(() => {
    if (!summaryData) return [];
    if (!searchTerm.trim()) return summaryData.students;
    const term = searchTerm.toLowerCase();
    return summaryData.students.filter(
      (s) =>
        s.registrationNumber.toLowerCase().includes(term) ||
        s.name.toLowerCase().includes(term)
    );
  }, [summaryData, searchTerm]);

  const lowAttendanceCount = useMemo(() => {
    if (!summaryData) return 0;
    return summaryData.students.filter((s) => s.overallPercentage < 75).length;
  }, [summaryData]);

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
            <FileSpreadsheet className="w-3.5 h-3.5" />
            Monthly Attendance Aggregation
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Monthly Summary Report
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Subject-wise attendance breakdown and overall turnout for {monthNames[selectedMonth - 1]} {selectedYear}.
          </p>
        </div>

        {/* Filter selectors */}
        <div className="flex flex-wrap items-center gap-2.5">
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

          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(Number(e.target.value))}
            className="py-2 px-3 bg-white border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 shadow-sm"
          >
            {[7, 8, 9, 10, 11].map((m) => (
              <option key={m} value={m}>
                {monthNames[m - 1]} {selectedYear}
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
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Sessions Held
            </span>
            <span className="text-xl font-black text-slate-900">
              {summaryData?.totalSubmittedSessions || 0}
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Section Turnout
            </span>
            <span className="text-xl font-black text-emerald-600">
              {summaryData?.sectionAveragePercentage || 0}%
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Attendance Shortage (&lt;75%)
            </span>
            <span className="text-xl font-black text-rose-600">{lowAttendanceCount} Students</span>
          </div>
        </div>
      </div>

      {/* Search Filter */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter students by roll or name..."
          className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
        />
      </div>

      {/* Monthly Summary Table */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Computing monthly statistics...</p>
        </div>
      ) : !summaryData || summaryData.totalSubmittedSessions === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500">
          <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-bold text-slate-800 text-sm">No Submitted Classes in this Month</h3>
          <p className="text-xs text-slate-500 mt-1">
            Classes must be marked and submitted to populate monthly aggregates.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto max-h-[700px]">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-slate-50 text-slate-700 sticky top-0 z-20 shadow-xs">
                <tr>
                  <th className="py-3 px-3 w-12 text-center border-r border-slate-200">S.No</th>
                  <th className="py-3 px-3 w-36 border-r border-slate-200">Registration No</th>
                  <th className="py-3 px-3 min-w-[180px] border-r border-slate-200">Student Name</th>
                  <th className="py-3 px-3 w-20 border-r border-slate-200">Batch</th>

                  {/* Subject headers */}
                  {summaryData.subjectCodes.map((code) => (
                    <th key={code} className="py-3 px-3 text-center border-r border-slate-200 min-w-[90px]">
                      <div className="font-bold text-slate-900">{code}</div>
                      <div className="text-[10px] text-slate-400 font-mono">P / Total (%)</div>
                    </th>
                  ))}

                  <th className="py-3 px-3 text-center bg-indigo-50 text-indigo-900 w-28 font-bold">
                    Monthly Total
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredStudents.map((st, idx) => {
                  const isLow = st.overallPercentage < 75;

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

                      {/* Subject Stats */}
                      {summaryData.subjectCodes.map((code) => {
                        const stat = st.subjects[code];
                        if (!stat || stat.total === 0) {
                          return (
                            <td key={code} className="py-2.5 px-3 text-center text-slate-300 border-r border-slate-100">
                              —
                            </td>
                          );
                        }
                        const isSubLow = stat.percentage < 75;
                        return (
                          <td key={code} className="py-2.5 px-3 text-center border-r border-slate-100">
                            <span className="font-semibold text-slate-800">
                              {stat.present}/{stat.total}
                            </span>
                            <span
                              className={`ml-1 text-[10px] px-1 rounded ${
                                isSubLow ? 'text-rose-700 bg-rose-50' : 'text-emerald-700 bg-emerald-50'
                              }`}
                            >
                              {stat.percentage}%
                            </span>
                          </td>
                        );
                      })}

                      {/* Total */}
                      <td
                        className={`py-2.5 px-3 text-center font-sans font-bold ${
                          isLow ? 'bg-rose-50 text-rose-800' : 'bg-emerald-50 text-emerald-800'
                        }`}
                      >
                        {st.totalPresent}/{st.totalEligible} ({st.overallPercentage}%)
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

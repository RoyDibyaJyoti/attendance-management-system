import React, { useState, useEffect, useMemo } from 'react';
import { apiFetch, RegisterResponse, Section } from '../api';
import {
  Calendar,
  ClipboardList,
  Search,
  Users,
  Filter,
  Download,
  AlertCircle,
  FileSpreadsheet,
} from 'lucide-react';

export const RegisterView: React.FC = () => {
  const [sections, setSections] = useState<Section[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<number | ''>('');
  const [selectedMonth, setSelectedMonth] = useState<number | ''>('');
  const [selectedYear, setSelectedYear] = useState<number>(2026);
  const [registerData, setRegisterData] = useState<RegisterResponse | null>(null);
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
      if (data.length > 0) {
        setSelectedSectionId(data[0].id);
      }
    } catch (err: any) {
      setError('Failed to load sections: ' + err.message);
    }
  };

  useEffect(() => {
    if (selectedSectionId) {
      loadRegister();
    }
  }, [selectedSectionId, selectedMonth, selectedYear]);

  const loadRegister = async () => {
    setLoading(true);
    setError(null);
    try {
      let url = `/attendance/register?sectionId=${selectedSectionId}`;
      if (selectedMonth !== '') {
        url += `&month=${selectedMonth}&year=${selectedYear}`;
      }
      const data = await apiFetch<RegisterResponse>(url);
      setRegisterData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load register');
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = useMemo(() => {
    if (!registerData) return [];
    if (!searchTerm.trim()) return registerData.students;
    const term = searchTerm.toLowerCase();
    return registerData.students.filter(
      (s) =>
        s.registrationNumber.toLowerCase().includes(term) ||
        s.name.toLowerCase().includes(term)
    );
  }, [registerData, searchTerm]);

  // Section metrics
  const totalStudents = registerData?.students.length || 0;
  const totalSessions = registerData?.sessions.length || 0;
  const sectionAverage = useMemo(() => {
    if (!registerData || registerData.students.length === 0) return 0;
    const sum = registerData.students.reduce((acc, s) => acc + s.percentage, 0);
    return (sum / registerData.students.length).toFixed(1);
  }, [registerData]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
            <ClipboardList className="w-3.5 h-3.5" />
            Detailed Attendance Matrix
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Class Attendance Register
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Student roll rows vs actual class sessions. Non-applicable lab batches marked with — and excluded from calculation.
          </p>
        </div>

        {/* Filter selectors */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div>
            <select
              value={selectedSectionId}
              onChange={(e) => setSelectedSectionId(Number(e.target.value))}
              className="py-2 px-3 bg-white border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {sections.map((s) => (
                <option key={s.id} value={s.id}>
                  Section: {s.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value === '' ? '' : Number(e.target.value))}
              className="py-2 px-3 bg-white border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All Months</option>
              <option value="7">July 2026</option>
              <option value="8">August 2026</option>
              <option value="9">September 2026</option>
              <option value="10">October 2026</option>
              <option value="11">November 2026</option>
            </select>
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Stats Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Enrolled Students
            </span>
            <span className="text-xl font-black text-slate-900">{totalStudents}</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Submitted Sessions Held
            </span>
            <span className="text-xl font-black text-slate-900">{totalSessions}</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <FileSpreadsheet className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider block">
              Section Average
            </span>
            <span className="text-xl font-black text-emerald-600">{sectionAverage}%</span>
          </div>
        </div>
      </div>

      {/* Search box */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter students by roll number or name..."
          className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
        />
      </div>

      {/* The Register Matrix Table */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Generating attendance grid...</p>
        </div>
      ) : !registerData || registerData.sessions.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500">
          <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-bold text-slate-800 text-sm">No Submitted Sessions Found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
            Once faculty takes and submits attendance for scheduled classes, sessions will appear here column-by-column as a scalable attendance grid.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto max-h-[700px]">
            <table className="w-full text-left text-xs border-collapse">
              {/* Header */}
              <thead className="bg-slate-50 text-slate-700 sticky top-0 z-20 shadow-xs">
                {/* Row 1: Session Dates & Subjects */}
                <tr className="border-b border-slate-200">
                  <th className="py-2.5 px-3 sticky left-0 z-30 bg-slate-100 w-12 text-center border-r border-slate-200">
                    S.No
                  </th>
                  <th className="py-2.5 px-3 sticky left-12 z-30 bg-slate-100 w-36 border-r border-slate-200">
                    Roll No
                  </th>
                  <th className="py-2.5 px-3 sticky left-48 z-30 bg-slate-100 min-w-[180px] border-r border-slate-200">
                    Student Name
                  </th>
                  <th className="py-2.5 px-3 sticky left-[372px] z-30 bg-slate-100 w-24 border-r border-slate-200">
                    Batch
                  </th>

                  {/* Session columns */}
                  {registerData.sessions.map((s) => (
                    <th
                      key={s.id}
                      className={`py-2 px-2.5 text-center min-w-[80px] border-r border-slate-200 ${
                        s.subjectType === 'LAB' ? 'bg-purple-50/60' : 'bg-slate-50'
                      }`}
                    >
                      <div className="text-[10px] text-slate-400 font-mono">
                        {s.date.substring(5)}
                      </div>
                      <div className="font-bold text-slate-900 text-[11px] leading-tight mt-0.5">
                        {s.subjectCode}
                      </div>
                      <div className="text-[9px] text-slate-500 font-medium">
                        {s.subjectType === 'LAB' ? s.labBatchName : 'Theory'}
                      </div>
                    </th>
                  ))}

                  {/* Summary Columns */}
                  <th className="py-2.5 px-3 bg-emerald-50 text-emerald-800 text-center w-14 font-bold border-r border-slate-200">
                    P
                  </th>
                  <th className="py-2.5 px-3 bg-rose-50 text-rose-800 text-center w-14 font-bold border-r border-slate-200">
                    A
                  </th>
                  <th className="py-2.5 px-3 bg-slate-100 text-slate-800 text-center w-16 font-bold border-r border-slate-200">
                    Total
                  </th>
                  <th className="py-2.5 px-3 bg-indigo-50 text-indigo-900 text-center w-16 font-bold">
                    %
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredStudents.map((st, idx) => {
                  const pct = st.percentage;
                  const isLow = pct < 75;

                  return (
                    <tr key={st.studentId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-2 px-3 sticky left-0 bg-white text-center text-slate-400 border-r border-slate-200">
                        {idx + 1}
                      </td>
                      <td className="py-2 px-3 sticky left-12 bg-white font-semibold text-slate-900 border-r border-slate-200">
                        {st.registrationNumber}
                      </td>
                      <td className="py-2 px-3 sticky left-48 bg-white font-sans font-medium text-slate-800 border-r border-slate-200 truncate max-w-[200px]">
                        {st.name}
                      </td>
                      <td className="py-2 px-3 sticky left-[372px] bg-white text-slate-500 font-sans text-[10px] border-r border-slate-200">
                        {st.labBatchName || '—'}
                      </td>

                      {/* Session cells */}
                      {registerData.sessions.map((sess) => {
                        const val = st.attendance[sess.id] || '';
                        let cellBg = '';
                        let textCol = 'text-slate-400';

                        if (val === 'P') {
                          cellBg = 'bg-emerald-50/60 font-bold';
                          textCol = 'text-emerald-700';
                        } else if (val === 'A') {
                          cellBg = 'bg-rose-50/70 font-bold';
                          textCol = 'text-rose-700';
                        } else if (val === '—') {
                          cellBg = 'bg-slate-50/50';
                          textCol = 'text-slate-300';
                        }

                        return (
                          <td
                            key={sess.id}
                            className={`py-2 px-2 text-center border-r border-slate-100 ${cellBg} ${textCol}`}
                          >
                            {val}
                          </td>
                        );
                      })}

                      {/* Totals */}
                      <td className="py-2 px-2 text-center font-bold text-emerald-700 bg-emerald-50/30 border-r border-slate-200">
                        {st.presentCount}
                      </td>
                      <td className="py-2 px-2 text-center font-bold text-rose-700 bg-rose-50/30 border-r border-slate-200">
                        {st.absentCount}
                      </td>
                      <td className="py-2 px-2 text-center font-medium text-slate-700 bg-slate-50 border-r border-slate-200">
                        {st.totalEligible}
                      </td>
                      <td
                        className={`py-2 px-2 text-center font-bold font-sans ${
                          isLow ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {pct}%
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

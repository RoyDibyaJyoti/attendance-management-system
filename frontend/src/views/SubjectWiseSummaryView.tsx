import React, { useState, useEffect, useMemo } from 'react';
import { apiFetch, SubjectWiseSummaryResponse, Section } from '../api';
import {
  BarChart3,
  Search,
  BookOpen,
  FlaskConical,
  AlertCircle,
  TrendingUp,
} from 'lucide-react';

export const SubjectWiseSummaryView: React.FC = () => {
  const [sections, setSections] = useState<Section[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<number | ''>('');
  const [summaryData, setSummaryData] = useState<SubjectWiseSummaryResponse | null>(null);
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
      loadSubjectWiseSummary();
    }
  }, [selectedSectionId]);

  const loadSubjectWiseSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<SubjectWiseSummaryResponse>(
        `/attendance/summary/subject-wise?sectionId=${selectedSectionId}`
      );
      setSummaryData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load subject-wise summary');
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
            <BarChart3 className="w-3.5 h-3.5" />
            Curricular Analysis
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Subject-wise Attendance Summary
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Theory & Practical Lab course breakdown across the entire academic term.
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

      {/* Metrics Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs text-slate-500 font-medium">Curricular Courses Tracked:</span>
            <div className="flex items-center gap-2 mt-0.5">
              {(summaryData?.baseSubjectCodes || []).map((code) => (
                <span
                  key={code}
                  className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-800 text-xs font-bold"
                >
                  {code}
                </span>
              ))}
            </div>
          </div>
        </div>

        <div className="text-right border-t sm:border-t-0 sm:border-l border-slate-100 pt-3 sm:pt-0 sm:pl-6">
          <span className="text-[11px] text-slate-500 font-semibold uppercase block">
            Overall Institutional Average
          </span>
          <span className="text-2xl font-black text-indigo-600">
            {summaryData?.sectionAveragePercentage || 0}%
          </span>
        </div>
      </div>

      {/* Search box */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search by roll number or student name..."
          className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
        />
      </div>

      {/* Subject-Wise Table */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Loading subject-wise analytics...</p>
        </div>
      ) : !summaryData || summaryData.baseSubjectCodes.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500">
          <BookOpen className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-bold text-slate-800 text-sm">No Attendance Data Available</h3>
          <p className="text-xs text-slate-500 mt-1">
            Submit attendance for theory or lab classes to view subject-wise breakdowns.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto max-h-[700px]">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-slate-50 text-slate-700 sticky top-0 z-20 shadow-xs">
                <tr>
                  <th className="py-3 px-3 w-12 text-center border-r border-slate-200" rowSpan={2}>
                    S.No
                  </th>
                  <th className="py-3 px-3 w-36 border-r border-slate-200" rowSpan={2}>
                    Roll No
                  </th>
                  <th className="py-3 px-3 min-w-[180px] border-r border-slate-200" rowSpan={2}>
                    Student Name
                  </th>
                  <th className="py-3 px-3 w-20 border-r border-slate-200" rowSpan={2}>
                    Batch
                  </th>

                  {/* Top Subject Headers */}
                  {summaryData.baseSubjectCodes.map((code) => (
                    <th
                      key={code}
                      colSpan={3}
                      className="py-2 px-3 text-center border-r border-slate-200 bg-slate-100/70 font-black text-slate-900"
                    >
                      {code}
                    </th>
                  ))}

                  <th className="py-3 px-3 text-center bg-indigo-50 text-indigo-900 w-28 font-bold" rowSpan={2}>
                    Overall
                  </th>
                </tr>

                {/* Sub headers: Theory, Lab, Combined */}
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] text-slate-600 font-semibold">
                  {summaryData.baseSubjectCodes.map((code) => (
                    <React.Fragment key={code}>
                      <th className="py-1 px-2 text-center border-r border-slate-100">Theory</th>
                      <th className="py-1 px-2 text-center border-r border-slate-100">Lab</th>
                      <th className="py-1 px-2 text-center border-r border-slate-200 bg-slate-100/50">Total</th>
                    </React.Fragment>
                  ))}
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredStudents.map((st, idx) => {
                  const isOverallLow = st.overallPercentage < 75;

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

                      {/* Subject Breakdowns */}
                      {summaryData.baseSubjectCodes.map((code) => {
                        const detail = st.subjects[code];
                        if (!detail) {
                          return (
                            <React.Fragment key={code}>
                              <td className="py-2 px-2 text-center text-slate-300 border-r border-slate-100">—</td>
                              <td className="py-2 px-2 text-center text-slate-300 border-r border-slate-100">—</td>
                              <td className="py-2 px-2 text-center text-slate-300 border-r border-slate-200">—</td>
                            </React.Fragment>
                          );
                        }

                        const th = detail.theory;
                        const lb = detail.lab;
                        const cb = detail.combined;

                        return (
                          <React.Fragment key={code}>
                            {/* Theory */}
                            <td className="py-2 px-2 text-center border-r border-slate-100">
                              {th.total > 0 ? (
                                <span className={th.percentage < 75 ? 'text-rose-600 font-bold' : 'text-slate-700'}>
                                  {th.percentage}%
                                </span>
                              ) : (
                                <span className="text-slate-300">—</span>
                              )}
                            </td>

                            {/* Lab */}
                            <td className="py-2 px-2 text-center border-r border-slate-100">
                              {lb.total > 0 ? (
                                <span className={lb.percentage < 75 ? 'text-rose-600 font-bold' : 'text-slate-700'}>
                                  {lb.percentage}%
                                </span>
                              ) : (
                                <span className="text-slate-300">—</span>
                              )}
                            </td>

                            {/* Combined */}
                            <td className="py-2 px-2 text-center border-r border-slate-200 bg-slate-50/50 font-bold">
                              {cb.total > 0 ? (
                                <span
                                  className={`px-1.5 py-0.5 rounded text-[10px] ${
                                    cb.percentage < 75
                                      ? 'bg-rose-100 text-rose-800'
                                      : 'bg-emerald-100 text-emerald-800'
                                  }`}
                                >
                                  {cb.percentage}%
                                </span>
                              ) : (
                                <span className="text-slate-300">—</span>
                              )}
                            </td>
                          </React.Fragment>
                        );
                      })}

                      {/* Overall Column */}
                      <td
                        className={`py-2.5 px-3 text-center font-sans font-bold ${
                          isOverallLow
                            ? 'bg-rose-50 text-rose-800'
                            : 'bg-emerald-50 text-emerald-800'
                        }`}
                      >
                        {st.overallPercentage}%
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

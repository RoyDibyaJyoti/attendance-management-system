import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  apiFetch,
  TimetableEntry,
  Section,
  Subject,
  Faculty,
  AcademicTerm,
  LabBatch,
  ClassSession,
  Holiday,
} from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Calendar,
  Clock,
  MapPin,
  BookOpen,
  FlaskConical,
  PlusCircle,
  AlertCircle,
  Pencil,
  Trash2,
  Plus,
  X,
  UserCheck,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { getCalendarDayStatus } from '../utils/calendarRules';
import { ScheduleImportModal } from '../components/ScheduleImportModal';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export const TimetableView: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [facultyList, setFacultyList] = useState<Faculty[]>([]);
  const [terms, setTerms] = useState<AcademicTerm[]>([]);
  const [labBatches, setLabBatches] = useState<LabBatch[]>([]);
  const [holidays, setHolidays] = useState<Holiday[]>([]);

  const [selectedSectionId, setSelectedSectionId] = useState<number | ''>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<TimetableEntry | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [sessionModalEntry, setSessionModalEntry] = useState<TimetableEntry | null>(null);
  const [sessionDateInput, setSessionDateInput] = useState<string>('');
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    termId: 0,
    sectionId: 0,
    subjectId: 0,
    facultyId: 0,
    labBatchId: 0,
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '10:40',
    room: '',
  });

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(null), 3500);
  };

  useEffect(() => {
    loadPrerequisites();
  }, []);

  const loadPrerequisites = async () => {
    try {
      const [secRes, subRes, facRes, termRes, holRes] = await Promise.all([
        apiFetch<Section[]>('/sections'),
        apiFetch<Subject[]>('/subjects'),
        apiFetch<Faculty[]>('/faculty'),
        apiFetch<AcademicTerm[]>('/terms'),
        apiFetch<Holiday[]>('/holidays'),
      ]);

      setSections(secRes);
      setSubjects(subRes);
      setFacultyList(facRes);
      setTerms(termRes);
      setHolidays(holRes);

      if (secRes.length > 0) {
        setSelectedSectionId(secRes[0].id);
      }
    } catch (err: any) {
      setError('Failed to load schedule prerequisites: ' + err.message);
    }
  };

  useEffect(() => {
    if (selectedSectionId) {
      loadTimetable();
      loadBatchesForSection(Number(selectedSectionId));
    }
  }, [selectedSectionId]);

  const loadBatchesForSection = async (secId: number) => {
    try {
      const batches = await apiFetch<LabBatch[]>(`/lab-batches?sectionId=${secId}`);
      setLabBatches(batches);
    } catch (err: any) {
      console.error('Failed to load lab batches', err);
    }
  };

  const loadTimetable = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<TimetableEntry[]>(
        `/timetable?sectionId=${selectedSectionId}`
      );
      setEntries(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load timetable');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAddModal = () => {
    const activeTerm = terms.find((t) => t.active) || terms[0];
    const currentSecId = Number(selectedSectionId) || sections[0]?.id || 0;
    const defaultSub = subjects[0];

    setFormData({
      termId: activeTerm?.id || 0,
      sectionId: currentSecId,
      subjectId: defaultSub?.id || 0,
      facultyId: facultyList[0]?.id || 0,
      labBatchId: 0,
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '10:40',
      room: 'Room 401',
    });
    setEditingEntry(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = async (entry: TimetableEntry) => {
    setEditingEntry(entry);
    setFormData({
      termId: entry.term.id,
      sectionId: entry.section.id,
      subjectId: entry.subject.id,
      facultyId: entry.faculty.id,
      labBatchId: entry.labBatch?.id || 0,
      dayOfWeek: entry.dayOfWeek,
      startTime: entry.startTime.substring(0, 5),
      endTime: entry.endTime.substring(0, 5),
      room: entry.room || '',
    });
    await loadBatchesForSection(entry.section.id);
    setIsModalOpen(true);
  };

  const handleFormSectionChange = async (newSecId: number) => {
    setFormData((prev) => ({ ...prev, sectionId: newSecId, labBatchId: 0 }));
    await loadBatchesForSection(newSecId);
  };

  const handleSaveEntry = async (e: React.FormEvent) => {
    e.preventDefault();

    const selectedSub = subjects.find((s) => s.id === formData.subjectId);
    if (!selectedSub) {
      alert('Please select a valid subject');
      return;
    }

    if (formData.startTime >= formData.endTime) {
      alert('End time must be strictly after start time.');
      return;
    }

    if (selectedSub.type === 'LAB' && (!formData.labBatchId || formData.labBatchId === 0)) {
      alert('Please select a Lab Batch for this LAB class.');
      return;
    }

    const payload = {
      termId: formData.termId,
      sectionId: formData.sectionId,
      subjectId: formData.subjectId,
      facultyId: formData.facultyId,
      dayOfWeek: formData.dayOfWeek,
      startTime: formData.startTime + ':00',
      endTime: formData.endTime + ':00',
      room: formData.room,
      labBatchId: selectedSub.type === 'LAB' ? formData.labBatchId : null,
    };

    try {
      if (editingEntry) {
        await apiFetch(`/timetable/${editingEntry.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        });
        showSuccess('Timetable entry updated successfully.');
      } else {
        await apiFetch('/timetable', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        showSuccess('Timetable entry created successfully.');
      }
      setIsModalOpen(false);
      await loadTimetable();
    } catch (err: any) {
      alert('Error saving timetable entry: ' + err.message);
    }
  };

  const handleDeleteEntry = async () => {
    if (!deleteConfirmId) return;
    try {
      await apiFetch(`/timetable/${deleteConfirmId}`, { method: 'DELETE' });
      showSuccess('Timetable entry deleted/deactivated.');
      setDeleteConfirmId(null);
      await loadTimetable();
    } catch (err: any) {
      alert('Delete failed: ' + err.message);
    }
  };

  const getTodayStr = (): string => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  const getDayOfWeekFromDate = (dateStr: string): string => {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('-').map(Number);
    const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    return dayNames[new Date(y, m - 1, d).getDay()];
  };

  const getMostRecentValidDate = (targetDay: string): string => {
    const d = new Date();
    const dayMap: Record<string, number> = {
      'SUNDAY': 0, 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6
    };
    const targetIdx = dayMap[targetDay];
    if (targetIdx === undefined) return getTodayStr();
    const currentIdx = d.getDay();
    let diff = currentIdx - targetIdx;
    if (diff < 0) {
      diff += 7;
    }
    d.setDate(d.getDate() - diff);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  const handleInitiateSession = (te: TimetableEntry) => {
    setSessionModalEntry(te);
    setSessionDateInput(getMostRecentValidDate(te.dayOfWeek));
  };

  const handleConfirmOpenSession = async () => {
    if (!sessionModalEntry || !sessionDateInput) return;
    try {
      const session = await apiFetch<ClassSession>('/sessions', {
        method: 'POST',
        body: JSON.stringify({
          timetableEntryId: sessionModalEntry.id,
          sessionDate: sessionDateInput,
        }),
      });
      setSessionModalEntry(null);
      navigate(`/sessions/${session.id}/attendance`);
    } catch (err: any) {
      alert('Error opening session: ' + err.message);
    }
  };

  const currentSelectedSubject = subjects.find((s) => s.id === formData.subjectId);
  const isSelectedSubjectLab = currentSelectedSubject?.type === 'LAB';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold mb-2">
            <Calendar className="w-3.5 h-3.5" />
            Official Academic Routine & Faculty Assignments
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Weekly Timetable Schedule
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage section schedules, faculty assignments, and lab batch slots.
          </p>
        </div>

        {/* Section Selector & Add Button */}
        <div className="flex items-center gap-3">
          <select
            value={selectedSectionId}
            onChange={(e) => setSelectedSectionId(Number(e.target.value))}
            className="py-2 px-3.5 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-800 shadow-sm"
          >
            {sections.map((s) => (
              <option key={s.id} value={s.id}>
                Section: {s.name} ({s.department?.code})
              </option>
            ))}
          </select>

          {user?.role === 'ADMIN' && (
            <>
              <button
                onClick={() => setIsImportModalOpen(true)}
                className="py-2 px-3.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-all whitespace-nowrap"
              >
                <Sparkles className="w-4 h-4" />
                Import Schedule
              </button>
              <button
                onClick={handleOpenAddModal}
                className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-all whitespace-nowrap"
              >
                <Plus className="w-4 h-4" />
                Add Class Entry
              </button>
            </>
          )}
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
          <span className="font-semibold">{successMsg}</span>
        </div>
      )}

      {/* Timetable Grid by Day */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Loading routine...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {DAYS.map((day) => {
            const dayEntries = entries
              .filter((e) => e.dayOfWeek === day)
              .sort((a, b) => a.startTime.localeCompare(b.startTime));

            return (
              <div
                key={day}
                className="bg-white rounded-2xl border border-slate-200 shadow-sm flex flex-col overflow-hidden"
              >
                {/* Day Header */}
                <div className="bg-slate-50 p-3 border-b border-slate-200 text-center flex items-center justify-between px-4">
                  <span className="font-extrabold text-xs text-slate-800 tracking-wide">
                    {day}
                  </span>
                  <span className="text-[10px] font-bold text-slate-400">
                    {dayEntries.length} {dayEntries.length === 1 ? 'class' : 'classes'}
                  </span>
                </div>

                {/* Day Slots */}
                <div className="p-3 space-y-3 flex-1">
                  {dayEntries.length === 0 ? (
                    <div className="h-32 flex items-center justify-center text-slate-400 text-xs italic">
                      No classes
                    </div>
                  ) : (
                    dayEntries.map((te) => {
                      const isLab = !!te.labBatch;
                      const isMyClass =
                        user?.role === 'FACULTY' &&
                        te.faculty?.user?.username === user?.username;

                      return (
                        <div
                          key={te.id}
                          className={`rounded-xl p-3 border text-xs flex flex-col justify-between transition-all ${
                            isLab
                              ? 'bg-purple-50/50 border-purple-200 hover:border-purple-300'
                              : 'bg-blue-50/50 border-blue-200 hover:border-blue-300'
                          } ${isMyClass ? 'ring-2 ring-indigo-500 shadow-sm' : ''}`}
                        >
                          <div>
                            {/* Time & Type Badge */}
                            <div className="flex items-center justify-between gap-1 mb-1.5">
                              <span className="font-mono text-[10px] text-slate-500 flex items-center gap-1">
                                <Clock className="w-3 h-3" />
                                {te.startTime.substring(0, 5)} - {te.endTime.substring(0, 5)}
                              </span>
                              <span
                                className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                                  isLab
                                    ? 'bg-purple-100 text-purple-800'
                                    : 'bg-blue-100 text-blue-800'
                                }`}
                              >
                                {isLab ? te.labBatch?.name : 'THEORY'}
                              </span>
                            </div>

                            {/* Subject */}
                            <div className="font-black text-slate-900 text-sm">
                              {te.subject.code}
                            </div>
                            <div className="text-[11px] text-slate-600 line-clamp-1 font-medium">
                              {te.subject.name}
                            </div>

                            {/* Details */}
                            <div className="mt-2 pt-2 border-t border-slate-200/60 space-y-1 text-[11px] text-slate-500">
                              <div className="flex items-center gap-1.5">
                                <MapPin className="w-3 h-3 text-slate-400 flex-shrink-0" />
                                <span className="font-medium text-slate-700 truncate">{te.room || 'Room TBA'}</span>
                              </div>
                              <div className="flex items-center gap-1.5">
                                <UserCheck className="w-3 h-3 text-indigo-500 flex-shrink-0" />
                                <span className="text-[10px] text-slate-800 font-bold truncate">
                                  {te.faculty?.user?.fullName || 'Faculty'}
                                </span>
                              </div>
                            </div>
                          </div>

                          {/* Admin & Faculty Actions */}
                          <div className="mt-3 pt-2 border-t border-slate-200/60 space-y-1.5">
                            {user?.role === 'ADMIN' && (
                              <div className="flex items-center justify-end gap-1 mb-1">
                                <button
                                  onClick={() => handleOpenEditModal(te)}
                                  className="p-1 text-slate-500 hover:text-indigo-600 hover:bg-white rounded"
                                  title="Edit Timetable Entry"
                                >
                                  <Pencil className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  onClick={() => setDeleteConfirmId(te.id)}
                                  className="p-1 text-slate-500 hover:text-rose-600 hover:bg-white rounded"
                                  title="Delete Timetable Entry"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            )}

                            {(isMyClass || user?.role === 'ADMIN') && (
                              <button
                                onClick={() => handleInitiateSession(te)}
                                className="w-full py-1.5 px-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-[10px] font-bold flex items-center justify-center gap-1 shadow-xs transition-colors"
                              >
                                <PlusCircle className="w-3 h-3" />
                                Take Attendance
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ==========================================
          ADD / EDIT TIMETABLE MODAL (ADMIN)
      ========================================== */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-bold text-slate-900 text-sm">
                  {editingEntry ? 'Edit Timetable Entry' : 'Create Timetable Entry'}
                </h3>
                <p className="text-[11px] text-slate-500">
                  Assign faculty, course type, and batch to schedule
                </p>
              </div>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveEntry} className="space-y-3.5 text-xs">
              {/* Term & Section */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Academic Term</label>
                  <select
                    value={formData.termId}
                    onChange={(e) => setFormData({ ...formData, termId: Number(e.target.value) })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                  >
                    {terms.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} {t.active ? '(Active)' : ''}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Section</label>
                  <select
                    value={formData.sectionId}
                    onChange={(e) => handleFormSectionChange(Number(e.target.value))}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                  >
                    {sections.map((s) => (
                      <option key={s.id} value={s.id}>
                        Section {s.name} ({s.department?.code})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Subject & Type Indicator */}
              <div>
                <label className="block font-bold text-slate-700 mb-1">
                  Subject / Course
                </label>
                <select
                  value={formData.subjectId}
                  onChange={(e) => setFormData({ ...formData, subjectId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-bold"
                >
                  {subjects.map((sub) => (
                    <option key={sub.id} value={sub.id}>
                      [{sub.type}] {sub.code} — {sub.name}
                    </option>
                  ))}
                </select>
                {isSelectedSubjectLab && (
                  <p className="mt-1 text-[11px] text-purple-700 font-semibold flex items-center gap-1">
                    <FlaskConical className="w-3 h-3" />
                    This is a LAB subject: selecting a Lab Batch is required below.
                  </p>
                )}
              </div>

              {/* Lab Batch Selection (Conditional on LAB) */}
              {isSelectedSubjectLab && (
                <div className="p-3 bg-purple-50/70 border border-purple-200 rounded-xl space-y-1">
                  <label className="block font-bold text-purple-900 mb-1">
                    Lab Batch <span className="text-rose-600">*</span>
                  </label>
                  <select
                    required
                    value={formData.labBatchId}
                    onChange={(e) => setFormData({ ...formData, labBatchId: Number(e.target.value) })}
                    className="w-full p-2 bg-white border border-purple-300 rounded-lg text-xs text-purple-950 font-bold"
                  >
                    <option value={0}>— Select Lab Batch —</option>
                    {labBatches.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Faculty Assignment */}
              <div>
                <label className="block font-bold text-slate-700 mb-1">
                  Assign Faculty Member <span className="text-rose-600">*</span>
                </label>
                <select
                  required
                  value={formData.facultyId}
                  onChange={(e) => setFormData({ ...formData, facultyId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-bold"
                >
                  {facultyList.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.user?.fullName} ({f.employeeId}) • Dept: {f.department?.code}
                    </option>
                  ))}
                </select>
              </div>

              {/* Day & Room */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Day of Week</label>
                  <select
                    value={formData.dayOfWeek}
                    onChange={(e) => setFormData({ ...formData, dayOfWeek: e.target.value })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                  >
                    {DAYS.map((d) => (
                      <option key={d} value={d}>
                        {d}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">Room / Lab Venue</label>
                  <input
                    type="text"
                    required
                    value={formData.room}
                    onChange={(e) => setFormData({ ...formData, room: e.target.value })}
                    placeholder="e.g. Room 402 or OS Lab"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-medium"
                  />
                </div>
              </div>

              {/* Timing */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Start Time</label>
                  <input
                    type="time"
                    required
                    value={formData.startTime}
                    onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-mono font-bold"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">End Time</label>
                  <input
                    type="time"
                    required
                    value={formData.endTime}
                    onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-mono font-bold"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  {editingEntry ? 'Update Timetable Entry' : 'Assign to Timetable'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE CONFIRMATION MODAL */}
      {deleteConfirmId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-sm w-full p-6 space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="font-black text-slate-900 text-base">Remove Timetable Entry?</h3>
              <p className="text-xs text-slate-500 mt-1">
                Are you sure you want to remove this class slot from the timetable?
              </p>
            </div>

            <div className="flex justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeleteConfirmId(null)}
                className="py-2 px-4 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteEntry}
                className="py-2 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-sm"
              >
                Yes, Remove
              </button>
            </div>
          </div>
        </div>
      )}
      {/* OPEN ATTENDANCE SESSION MODAL */}
      {sessionModalEntry && (() => {
        const todayStr = getTodayStr();
        const selectedDayOfWeek = getDayOfWeekFromDate(sessionDateInput);
        const isFuture = sessionDateInput > todayStr;
        const isDayMismatch = selectedDayOfWeek !== sessionModalEntry.dayOfWeek;
        const calStatus = getCalendarDayStatus(sessionDateInput, holidays);

        let validationError: string | null = null;
        if (isFuture) {
          validationError = 'Future dates are not allowed. Attendance cannot be recorded for future dates.';
        } else if (calStatus.isNonWorkingDay) {
          validationError = `Cannot open session: ${calStatus.reason} (${sessionDateInput}).`;
        } else if (isDayMismatch) {
          validationError = `Selected date (${sessionDateInput}) is a ${selectedDayOfWeek}, but this class is scheduled on ${sessionModalEntry.dayOfWeek}.`;
        }

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
            <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Open Attendance Session
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    {sessionModalEntry.subject.code} — {sessionModalEntry.subject.name}
                  </p>
                </div>
                <button
                  onClick={() => setSessionModalEntry(null)}
                  className="text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-3.5 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                  <div className="flex justify-between text-slate-600">
                    <span>Scheduled Day:</span>
                    <strong className="text-slate-900">{sessionModalEntry.dayOfWeek}</strong>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Slot Time:</span>
                    <strong className="text-slate-900">
                      {sessionModalEntry.startTime.substring(0, 5)} - {sessionModalEntry.endTime.substring(0, 5)}
                    </strong>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Section / Batch:</span>
                    <strong className="text-slate-900">
                      {sessionModalEntry.section.name} {sessionModalEntry.labBatch ? `(${sessionModalEntry.labBatch.name})` : '(Theory)'}
                    </strong>
                  </div>
                </div>

                <div>
                  <label className="block font-bold text-slate-700 mb-1">
                    Select Class Date (Scheduled {sessionModalEntry.dayOfWeek}s only)
                  </label>
                  <input
                    type="date"
                    max={todayStr}
                    value={sessionDateInput}
                    onChange={(e) => setSessionDateInput(e.target.value)}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-bold text-xs text-slate-900"
                  />
                </div>

                {validationError && (
                  <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                    <span>{validationError}</span>
                  </div>
                )}

                <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setSessionModalEntry(null)}
                    className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    disabled={!!validationError || !sessionDateInput}
                    onClick={handleConfirmOpenSession}
                    className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 disabled:hover:bg-indigo-600 text-white font-bold shadow-sm flex items-center gap-1.5"
                  >
                    <PlusCircle className="w-3.5 h-3.5" />
                    Open Session & Roster
                  </button>
                </div>
              </div>
            </div>
          </div>
        );
      })()}

      {/* Schedule Import Modal */}
      <ScheduleImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onSuccess={(result) => {
          setIsImportModalOpen(false);
          showSuccess(
            `Schedule imported successfully! ${result.timetableEntriesCreated} timetable entries created.`
          );
          loadPrerequisites();
          loadTimetable();
        }}
      />
    </div>
  );
};

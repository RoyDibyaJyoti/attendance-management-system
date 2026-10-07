import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, TodayClassDto, TimetableEntry, ClassSession, Holiday } from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Calendar,
  Clock,
  MapPin,
  Users,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  BookOpen,
  FlaskConical,
  PlusCircle,
} from 'lucide-react';
import { getCalendarDayStatus } from '../utils/calendarRules';

export const FacultyDashboardView: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [todayClasses, setTodayClasses] = useState<TodayClassDto[]>([]);
  const [allMyClasses, setAllMyClasses] = useState<TimetableEntry[]>([]);
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  // Manual open session modal / state
  const [selectedEntryId, setSelectedEntryId] = useState<number | ''>('');
  const [sessionDate, setSessionDate] = useState<string>(getTodayStr());
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [todayData, myData, holidaysData] = await Promise.all([
        apiFetch<TodayClassDto[]>('/sessions/today'),
        apiFetch<TimetableEntry[]>('/timetable/my'),
        apiFetch<Holiday[]>('/holidays'),
      ]);
      setTodayClasses(todayData);
      setAllMyClasses(myData);
      setHolidays(holidaysData);
      if (myData.length > 0 && selectedEntryId === '') {
        setSelectedEntryId(myData[0].id);
        setSessionDate(getMostRecentValidDate(myData[0].dayOfWeek));
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenSession = async (timetableEntryId: number, targetDate: string) => {
    setCreating(true);
    try {
      const session = await apiFetch<ClassSession>('/sessions', {
        method: 'POST',
        body: JSON.stringify({
          timetableEntryId,
          sessionDate: targetDate,
        }),
      });
      navigate(`/sessions/${session.id}/attendance`);
    } catch (err: any) {
      alert('Error opening session: ' + err.message);
    } finally {
      setCreating(false);
    }
  };

  const todayStr = new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date());

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Greeting */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-gradient-to-r from-indigo-700 via-indigo-600 to-indigo-800 rounded-2xl p-6 sm:p-8 text-white shadow-xl shadow-indigo-100">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/15 text-indigo-100 text-xs font-medium mb-3 backdrop-blur-sm">
            <Calendar className="w-3.5 h-3.5" />
            {todayStr}
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Welcome back, {user?.fullName}
          </h1>
          <p className="text-indigo-100/90 text-sm mt-1 max-w-xl">
            Institutional Attendance Management System • Faculty Portal
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-white/10 rounded-xl p-3 backdrop-blur-sm border border-white/15 text-center min-w-[110px]">
            <span className="block text-2xl font-black">{todayClasses.length}</span>
            <span className="text-[11px] text-indigo-200 uppercase font-semibold">Today's Slots</span>
          </div>
          <div className="bg-white/10 rounded-xl p-3 backdrop-blur-sm border border-white/15 text-center min-w-[110px]">
            <span className="block text-2xl font-black">
              {todayClasses.filter((c) => c.session?.status === 'SUBMITTED').length}
            </span>
            <span className="text-[11px] text-indigo-200 uppercase font-semibold">Completed</span>
          </div>
        </div>
      </div>

      {/* Holiday Banner if Today is Sunday, Second Saturday, or Declared Holiday */}
      {(() => {
        const todayIso = getTodayStr();
        const calStatus = getCalendarDayStatus(todayIso, holidays);
        if (!calStatus.isNonWorkingDay) return null;
        return (
          <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center gap-3 shadow-xs">
            <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold flex-shrink-0">
              <Calendar className="w-4 h-4" />
            </div>
            <div>
              <div className="font-extrabold text-amber-900 text-sm">
                {calStatus.reason} ({todayIso})
              </div>
              <div className="text-amber-700 mt-0.5">
                Institutional classes are officially suspended. Attendance recording is disabled for today.
              </div>
            </div>
          </div>
        );
      })()}

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-500" />
          <span>{error}</span>
        </div>
      )}

      {/* Today's Classes List */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900 tracking-tight">
              Today's Scheduled Routine
            </h2>
            <p className="text-xs text-slate-500">Classes scheduled according to timetable</p>
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-44 rounded-2xl bg-white border border-slate-200 animate-pulse" />
            ))}
          </div>
        ) : todayClasses.length === 0 ? (() => {
          const todayIso = getTodayStr();
          const calStatus = getCalendarDayStatus(todayIso, holidays);
          if (calStatus.isNonWorkingDay) {
            return (
              <div className="bg-amber-50/50 rounded-2xl border border-amber-200/70 p-10 text-center text-slate-600">
                <Calendar className="w-12 h-12 text-amber-500 mx-auto mb-3" />
                <h3 className="font-extrabold text-slate-900 text-base">{calStatus.reason}</h3>
                <p className="text-xs text-amber-800 font-medium mt-1">
                  Today is an official non-working day. Institutional classes are suspended and attendance recording is disabled.
                </p>
              </div>
            );
          }
          return (
            <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-slate-500">
              <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-3" />
              <p className="font-semibold text-slate-700">No classes scheduled for today.</p>
              <p className="text-xs text-slate-400 mt-1">
                You can manually open an attendance session for any day using the quick session opener below.
              </p>
            </div>
          );
        })() : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {todayClasses.map(({ timetableEntry, session }) => {
              const isLab = !!timetableEntry.labBatch;
              const isSubmitted = session?.status === 'SUBMITTED';
              const isOpen = session?.status === 'OPEN';

              return (
                <div
                  key={timetableEntry.id}
                  className="bg-white rounded-2xl border border-slate-200/90 shadow-sm hover:shadow-md transition-all p-5 flex flex-col justify-between"
                >
                  <div>
                    {/* Header: Subject & Badges */}
                    <div className="flex items-start justify-between gap-2 mb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-black text-slate-900 text-lg">
                            {timetableEntry.subject.code}
                          </span>
                          <span
                            className={`text-[11px] font-bold px-2 py-0.5 rounded-full inline-flex items-center gap-1 ${
                              isLab
                                ? 'bg-purple-100 text-purple-700'
                                : 'bg-blue-100 text-blue-700'
                            }`}
                          >
                            {isLab ? <FlaskConical className="w-3 h-3" /> : <BookOpen className="w-3 h-3" />}
                            {isLab ? 'LAB' : 'THEORY'}
                          </span>
                        </div>
                        <h3 className="text-xs font-medium text-slate-600 line-clamp-1 mt-0.5">
                          {timetableEntry.subject.name}
                        </h3>
                      </div>

                      {/* Status indicator badge */}
                      {isSubmitted ? (
                        <span className="text-[11px] font-semibold px-2 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          Done
                        </span>
                      ) : isOpen ? (
                        <span className="text-[11px] font-semibold px-2 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200">
                          In Progress
                        </span>
                      ) : (
                        <span className="text-[11px] font-semibold px-2 py-1 rounded-md bg-slate-100 text-slate-600">
                          Scheduled
                        </span>
                      )}
                    </div>

                    {/* Metadata details */}
                    <div className="space-y-1.5 text-xs text-slate-600 mt-4 pt-3 border-t border-slate-100">
                      <div className="flex items-center gap-2">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        <span>
                          {timetableEntry.startTime.substring(0, 5)} - {timetableEntry.endTime.substring(0, 5)}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <MapPin className="w-3.5 h-3.5 text-slate-400" />
                        <span>{timetableEntry.room || 'Room TBA'}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <Users className="w-3.5 h-3.5 text-slate-400" />
                        <span>
                          {timetableEntry.section.name}{' '}
                          {isLab && (
                            <strong className="text-purple-700 font-semibold">
                              ({timetableEntry.labBatch?.name} • Lab Batch)
                            </strong>
                          )}
                          {!isLab && <span className="text-slate-500">(All Section Students)</span>}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Action button */}
                  <div className="mt-5 pt-3">
                    {session ? (
                      <button
                        onClick={() => navigate(`/sessions/${session.id}/attendance`)}
                        className={`w-full py-2.5 px-4 rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                          isSubmitted
                            ? 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                            : 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm shadow-indigo-100'
                        }`}
                      >
                        {isSubmitted ? 'View Attendance' : 'Resume Attendance'}
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    ) : (() => {
                      const todayIso = getTodayStr();
                      const calStatus = getCalendarDayStatus(todayIso, holidays);
                      if (calStatus.isNonWorkingDay) {
                        return (
                          <button
                            disabled
                            className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold bg-slate-100 text-slate-400 cursor-not-allowed flex items-center justify-center gap-2 border border-slate-200"
                            title={calStatus.reason || 'Non-working day'}
                          >
                            <AlertCircle className="w-3.5 h-3.5 text-amber-500" />
                            {calStatus.reason}
                          </button>
                        );
                      }
                      return (
                        <button
                          disabled={creating}
                          onClick={() => handleOpenSession(timetableEntry.id, todayIso)}
                          className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700 active:bg-indigo-800 transition-all shadow-sm shadow-indigo-100 flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                          <PlusCircle className="w-3.5 h-3.5" />
                          Take Attendance Now
                        </button>
                      );
                    })()}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Manual / Past Session Opener Card */}
      {(() => {
        const todayIso = getTodayStr();
        const selectedEntry = allMyClasses.find((c) => c.id === selectedEntryId);
        const selectedDateDay = getDayOfWeekFromDate(sessionDate);
        const isFuture = sessionDate > todayIso;
        const isDayMismatch = selectedEntry ? selectedDateDay !== selectedEntry.dayOfWeek : false;
        const calStatus = getCalendarDayStatus(sessionDate, holidays);

        let validationError: string | null = null;
        if (isFuture) {
          validationError = 'Future dates are not allowed. Attendance cannot be recorded for future dates.';
        } else if (calStatus.isNonWorkingDay) {
          validationError = `Cannot open attendance: ${calStatus.reason} (${sessionDate}).`;
        } else if (isDayMismatch && selectedEntry) {
          validationError = `Selected date (${sessionDate}) is a ${selectedDateDay}, but this class is scheduled on ${selectedEntry.dayOfWeek}s.`;
        }

        return (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2.5 mb-2">
              <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                <PlusCircle className="w-4 h-4" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900 text-sm">
                  Open Session for Any Date or Class
                </h3>
                <p className="text-xs text-slate-500">
                  Need to record attendance for a previous date or another scheduled slot? Select class and date.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 uppercase mb-1">
                  Select Class
                </label>
                <select
                  value={selectedEntryId}
                  onChange={(e) => {
                    const id = Number(e.target.value);
                    setSelectedEntryId(id);
                    const ent = allMyClasses.find((c) => c.id === id);
                    if (ent) {
                      setSessionDate(getMostRecentValidDate(ent.dayOfWeek));
                    }
                  }}
                  className="w-full py-2 px-3 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  {allMyClasses.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.subject.code} {c.labBatch ? `(${c.labBatch.name})` : '(Theory)'} — {c.dayOfWeek} {c.startTime.substring(0, 5)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-600 uppercase mb-1">
                  Class Date {selectedEntry ? `(Scheduled ${selectedEntry.dayOfWeek}s)` : ''}
                </label>
                <input
                  type="date"
                  max={todayIso}
                  value={sessionDate}
                  onChange={(e) => setSessionDate(e.target.value)}
                  className="w-full py-2 px-3 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-bold"
                />
              </div>

              <div className="flex items-end">
                <button
                  disabled={creating || selectedEntryId === '' || !!validationError}
                  onClick={() => handleOpenSession(Number(selectedEntryId), sessionDate)}
                  className="w-full py-2.5 px-4 bg-slate-900 hover:bg-slate-800 disabled:opacity-40 disabled:hover:bg-slate-900 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
                >
                  <ArrowRight className="w-3.5 h-3.5" />
                  Open Session & Roster
                </button>
              </div>
            </div>

            {validationError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2">
                <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                <span>{validationError}</span>
              </div>
            )}
          </div>
        );
      })()}
    </div>
  );
};

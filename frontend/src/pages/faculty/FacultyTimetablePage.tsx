import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { academicApi } from '../../api/academicApi';
import { sessionApi } from '../../api/sessionApi';
import { facultyAssignmentApi } from '../../api/facultyAssignmentApi';
import { useToast } from '../../context/ToastContext';
import { SessionResponse, CreateSessionRequest } from '../../types/session';
import { SectionResponse, SubjectResponse, AcademicPeriodResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { EmptyState } from '../../components/common/EmptyState';
import {
  Calendar,
  Plus,
  PlayCircle,
  XCircle,
  RotateCcw,
  Clock,
  BookOpen,
  Filter,
} from 'lucide-react';
import { ApiError } from '../../api/client';

export const FacultyTimetablePage: React.FC = () => {
  const { user } = useAuth();
  const facultyId = user?.facultyId;
  const { showToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<string>('');
  const [sessions, setSessions] = useState<SessionResponse[]>([]);

  // Schedule Modal
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [newSubjectId, setNewSubjectId] = useState('');
  const [newSessionDate, setNewSessionDate] = useState(new Date().toISOString().split('T')[0]);
  const [newSessionType, setNewSessionType] = useState<'THEORY' | 'LAB'>('THEORY');
  const [newPlannedUnits, setNewPlannedUnits] = useState(1);
  const [isScheduling, setIsScheduling] = useState(false);

  // Cancel Modal
  const [cancelSessionId, setCancelSessionId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [isCancelling, setIsCancelling] = useState(false);

  // Reschedule Modal
  const [rescheduleSessionId, setRescheduleSessionId] = useState<string | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleReason, setRescheduleReason] = useState('');
  const [isRescheduling, setIsRescheduling] = useState(false);

  const loadSessions = useCallback(async (sectionId: string) => {
    if (!sectionId) return;
    try {
      const data = await sessionApi.getSessions(sectionId);
      setSessions(data);
    } catch (err) {
      console.error('Failed to load sessions:', err);
      showToast('Failed to load sessions for section.', 'error');
    }
  }, [showToast]);

  useEffect(() => {
    async function init() {
      setIsLoading(true);
      try {
        if (!facultyId) return;

        const [assnRes, pers] = await Promise.all([
          facultyAssignmentApi.getAssignments(facultyId, undefined, 0, 100),
          academicApi.getPeriods(),
        ]);
        
        const activeAssignments = assnRes.content.filter(a => a.status === 'ACTIVE');
        setPeriods(pers);

        // Extract unique section and subject IDs
        const uniqueSectionIds = Array.from(new Set(activeAssignments.map(a => a.sectionId)));
        const uniqueSubjectIds = Array.from(new Set(activeAssignments.map(a => a.subjectId)));

        // Fetch details for these specific entities
        const [secs, subs] = await Promise.all([
          Promise.all(uniqueSectionIds.map(id => academicApi.getSectionById(id))),
          Promise.all(uniqueSubjectIds.map(id => academicApi.getSubjectById(id))),
        ]);

        setSections(secs);
        setSubjects(subs);

        if (secs.length > 0) {
          setSelectedSectionId(secs[0].id);
          const data = await sessionApi.getSessions(secs[0].id);
          setSessions(data);
        }
      } catch (err) {
        console.error('Failed to initialize timetable:', err);
      } finally {
        setIsLoading(false);
      }
    }
    init();
  }, [facultyId]);

  const handleSectionChange = (newSecId: string) => {
    setSelectedSectionId(newSecId);
    loadSessions(newSecId);
  };

  const handleScheduleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!facultyId || !selectedSectionId || !newSubjectId || !periods[0]) return;

    setIsScheduling(true);
    try {
      const req: CreateSessionRequest = {
        subjectId: newSubjectId,
        sectionId: selectedSectionId,
        conductedByFacultyId: facultyId,
        academicPeriodId: periods[0].id,
        sessionDate: newSessionDate,
        sessionType: newSessionType,
        plannedUnits: newPlannedUnits,
      };
      await sessionApi.createSession(req);
      showToast('Session scheduled successfully.', 'success');
      setIsScheduleModalOpen(false);
      loadSessions(selectedSectionId);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to schedule session.', 'error');
      }
    } finally {
      setIsScheduling(false);
    }
  };

  const handleCancelSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelSessionId || !cancelReason.trim()) return;

    setIsCancelling(true);
    try {
      await sessionApi.cancelSession(cancelSessionId, { reason: cancelReason.trim() });
      showToast('Session cancelled.', 'info');
      setCancelSessionId(null);
      setCancelReason('');
      loadSessions(selectedSectionId);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to cancel session.', 'error');
      }
    } finally {
      setIsCancelling(false);
    }
  };

  const handleRescheduleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rescheduleSessionId || !rescheduleDate || !rescheduleReason.trim()) return;

    setIsRescheduling(true);
    try {
      await sessionApi.rescheduleSession(rescheduleSessionId, {
        newSessionDate: rescheduleDate,
        reason: rescheduleReason.trim(),
      });
      showToast('Session rescheduled successfully.', 'success');
      setRescheduleSessionId(null);
      setRescheduleDate('');
      setRescheduleReason('');
      loadSessions(selectedSectionId);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to reschedule session.', 'error');
      }
    } finally {
      setIsRescheduling(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading timetable and section schedules..." />;
  }

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Faculty Timetable & Sessions</h1>
          <p className="text-sm text-slate-500 mt-0.5">Manage scheduled lectures, labs, and attendance sessions</p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            onClick={() => setIsScheduleModalOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Schedule New Session
          </Button>
        </div>
      </div>

      {/* Section Filter Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-4">
        <Filter className="w-4 h-4 text-slate-400 shrink-0" />
        <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider shrink-0">
          Cohort Section:
        </span>
        <div className="max-w-xs w-full">
          <select
            value={selectedSectionId}
            onChange={(e) => handleSectionChange(e.target.value)}
            className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          >
            {sections.map((sec) => (
              <option key={sec.id} value={sec.id}>
                {sec.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Timetable Session List */}
      {sessions.length === 0 ? (
        <EmptyState
          title="No Sessions Scheduled"
          description="There are currently no sessions scheduled for this section. Click above to schedule your first class."
          action={
            <Button
              size="sm"
              onClick={() => setIsScheduleModalOpen(true)}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Schedule Session
            </Button>
          }
        />
      ) : (
        <div className="space-y-3">
          {sessions.map((session) => {
            const sub = subjectMap.get(session.subjectId);
            const isConducted = session.status === 'CONDUCTED';
            const isScheduled = session.status === 'SCHEDULED';
            const isCancelled = session.status === 'CANCELLED';

            return (
              <div
                key={session.id}
                className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 hover:border-slate-300 transition-colors"
              >
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-700 flex flex-col items-center justify-center shrink-0 border border-indigo-100/80">
                    <span className="text-[10px] font-bold uppercase leading-none">
                      {new Date(session.sessionDate).toLocaleString('default', { month: 'short' })}
                    </span>
                    <span className="text-base font-extrabold leading-tight">
                      {new Date(session.sessionDate).getDate()}
                    </span>
                  </div>

                  <div>
                    <div className="flex items-center gap-2.5">
                      <span className="font-mono text-xs font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
                        {sub?.code || 'COURSE'}
                      </span>
                      <Badge
                        variant={
                          isConducted ? 'success' : isScheduled ? 'warning' : isCancelled ? 'danger' : 'neutral'
                        }
                        size="sm"
                        dot
                      >
                        {session.status}
                      </Badge>
                    </div>

                    <h3 className="text-base font-bold text-slate-900 mt-1">
                      {sub?.name || 'Class Occurrence'}
                    </h3>

                    <div className="flex items-center gap-4 text-xs text-slate-500 mt-1">
                      <span>Type: <strong className="text-slate-700">{session.sessionType}</strong></span>
                      <span>Planned: <strong className="text-slate-700">{session.plannedUnits} units</strong></span>
                      {isConducted && (
                        <span>Conducted: <strong className="text-emerald-700">{session.conductedUnits} units</strong></span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-2 w-full sm:w-auto justify-end pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                  {isScheduled && (
                    <>
                      <Link to={`/faculty/sessions/${session.id}/attendance`}>
                        <Button
                          size="sm"
                          className="bg-indigo-600 hover:bg-indigo-700 text-white"
                          leftIcon={<PlayCircle className="w-4 h-4" />}
                        >
                          Roll Call
                        </Button>
                      </Link>

                      <Button
                        size="sm"
                        variant="outline"
                        title="Reschedule session to another date"
                        onClick={() => {
                          setRescheduleSessionId(session.id);
                          setRescheduleDate(session.sessionDate);
                        }}
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                      </Button>

                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-rose-600 hover:bg-rose-50"
                        title="Cancel this session"
                        onClick={() => setCancelSessionId(session.id)}
                      >
                        <XCircle className="w-3.5 h-3.5" />
                      </Button>
                    </>
                  )}

                  {isConducted && (
                    <Link to={`/faculty/sessions/${session.id}/attendance`}>
                      <Button size="sm" variant="secondary">
                        View Roll-Call Sheet
                      </Button>
                    </Link>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Schedule Modal */}
      <Modal
        isOpen={isScheduleModalOpen}
        onClose={() => setIsScheduleModalOpen(false)}
        title="Schedule Academic Session"
        subtitle="Add a lecture or laboratory session occurrence to the timetable"
      >
        <form onSubmit={handleScheduleSubmit} className="space-y-4">
          <Select
            label="Subject"
            id="session-subject"
            options={subjects.map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))}
            value={newSubjectId}
            onChange={(e) => setNewSubjectId(e.target.value)}
            placeholder="Select curriculum course"
            required
          />

          <Input
            label="Session Date"
            id="session-date"
            type="date"
            value={newSessionDate}
            onChange={(e) => setNewSessionDate(e.target.value)}
            required
          />

          <Select
            label="Session Stream"
            id="session-type"
            options={[
              { value: 'THEORY', label: 'Theory Session' },
              { value: 'LAB', label: 'Laboratory Session' },
            ]}
            value={newSessionType}
            onChange={(e) => setNewSessionType(e.target.value as 'THEORY' | 'LAB')}
            required
          />

          <Input
            label="Planned Attendance Units"
            id="session-units"
            type="number"
            min={1}
            max={5}
            value={newPlannedUnits}
            onChange={(e) => setNewPlannedUnits(parseInt(e.target.value, 10))}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsScheduleModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isScheduling}>
              Confirm Schedule
            </Button>
          </div>
        </form>
      </Modal>

      {/* Cancel Modal */}
      <Modal
        isOpen={!!cancelSessionId}
        onClose={() => setCancelSessionId(null)}
        title="Cancel Session Occurrence"
        subtitle="Cancelled sessions contribute 0 conducted units to student attendance"
      >
        <form onSubmit={handleCancelSubmit} className="space-y-4">
          <Input
            label="Reason for Cancellation"
            id="cancel-reason"
            type="text"
            placeholder="e.g. Official University Holiday, Faculty Sabbatical"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setCancelSessionId(null)}>
              Dismiss
            </Button>
            <Button variant="danger" type="submit" isLoading={isCancelling}>
              Confirm Cancellation
            </Button>
          </div>
        </form>
      </Modal>

      {/* Reschedule Modal */}
      <Modal
        isOpen={!!rescheduleSessionId}
        onClose={() => setRescheduleSessionId(null)}
        title="Reschedule Session Occurrence"
        subtitle="Moves planned session to a new replacement date"
      >
        <form onSubmit={handleRescheduleSubmit} className="space-y-4">
          <Input
            label="New Session Date"
            id="reschedule-date"
            type="date"
            value={rescheduleDate}
            onChange={(e) => setRescheduleDate(e.target.value)}
            required
          />

          <Input
            label="Reason for Rescheduling"
            id="reschedule-reason"
            type="text"
            placeholder="e.g. Lab equipment maintenance, time conflict"
            value={rescheduleReason}
            onChange={(e) => setRescheduleReason(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setRescheduleSessionId(null)}>
              Dismiss
            </Button>
            <Button type="submit" isLoading={isRescheduling}>
              Confirm Reschedule
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

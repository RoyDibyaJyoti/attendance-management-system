import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { academicApi } from '../../api/academicApi';
import { facultyApi } from '../../api/facultyApi';
import { sessionApi } from '../../api/sessionApi';
import { useToast } from '../../context/ToastContext';
import { SessionResponse, CreateSessionRequest } from '../../types/session';
import { SectionResponse, SubjectResponse, AcademicPeriodResponse } from '../../types/academic';
import { FacultyResponse } from '../../types/faculty';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { EmptyState } from '../../components/common/EmptyState';
import { Plus, Calendar, RotateCcw, XCircle, PlayCircle, Filter } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminSessionsPage: React.FC = () => {
  const { showToast } = useToast();
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [facultyList, setFacultyList] = useState<FacultyResponse[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<string>('');
  const [sessions, setSessions] = useState<SessionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Schedule Modal
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [newSubjectId, setNewSubjectId] = useState('');
  const [newFacultyId, setNewFacultyId] = useState('');
  const [newSessionDate, setNewSessionDate] = useState(new Date().toISOString().split('T')[0]);
  const [newSessionType, setNewSessionType] = useState<'THEORY' | 'LAB'>('THEORY');
  const [newPlannedUnits, setNewPlannedUnits] = useState(1);
  const [isScheduling, setIsScheduling] = useState(false);

  const loadSessions = useCallback(async (secId: string) => {
    if (!secId) return;
    try {
      const data = await sessionApi.getSessions(secId);
      setSessions(data);
    } catch (err) {
      console.error('Failed to load sessions:', err);
    }
  }, []);

  useEffect(() => {
    async function init() {
      try {
        const [secs, subs, pers, facs] = await Promise.all([
          academicApi.getAllSections(),
          academicApi.getAllSubjects(),
          academicApi.getPeriods(),
          facultyApi.getAllFaculty(),
        ]);
        setSections(secs);
        setSubjects(subs);
        setPeriods(pers);
        setFacultyList(facs);

        if (secs.length > 0) {
          setSelectedSectionId(secs[0].id);
          const data = await sessionApi.getSessions(secs[0].id);
          setSessions(data);
        }
        if (subs.length > 0) setNewSubjectId(subs[0].id);
        if (facs.length > 0) setNewFacultyId(facs[0].id);
      } catch (err) {
        console.error('Failed to initialize sessions page:', err);
      } finally {
        setIsLoading(false);
      }
    }
    init();
  }, []);

  const handleSectionChange = (secId: string) => {
    setSelectedSectionId(secId);
    loadSessions(secId);
  };

  const handleScheduleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSectionId || !newSubjectId || !newFacultyId || !periods[0]) return;

    setIsScheduling(true);
    try {
      const req: CreateSessionRequest = {
        subjectId: newSubjectId,
        sectionId: selectedSectionId,
        conductedByFacultyId: newFacultyId,
        academicPeriodId: periods[0].id,
        sessionDate: newSessionDate,
        sessionType: newSessionType,
        plannedUnits: newPlannedUnits,
      };
      await sessionApi.createSession(req);
      showToast('Master session scheduled successfully.', 'success');
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

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading master timetables..." />;
  }

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));
  const facultyMap = new Map(facultyList.map((f) => [f.id, f]));

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Master Timetables & Sessions</h1>
          <p className="text-sm text-slate-500 mt-0.5">Institution-wide session timetabling, assignments, and cancellations</p>
        </div>

        <Button onClick={() => setIsScheduleModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Schedule Class Session
        </Button>
      </div>

      {/* Cohort Section Selector */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-4">
        <Filter className="w-4 h-4 text-slate-400 shrink-0" />
        <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider shrink-0">
          Target Section:
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
          title="No Sessions in Section"
          description="There are currently no sessions scheduled in this section timetable."
          action={
            <Button size="sm" onClick={() => setIsScheduleModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
              Schedule First Session
            </Button>
          }
        />
      ) : (
        <div className="space-y-3">
          {sessions.map((session) => {
            const sub = subjectMap.get(session.subjectId);
            const fac = facultyMap.get(session.conductedByFacultyId);
            const isConducted = session.status === 'CONDUCTED';
            const isScheduled = session.status === 'SCHEDULED';
            const isCancelled = session.status === 'CANCELLED';

            return (
              <div
                key={session.id}
                className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
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

                    <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 mt-1">
                      <span>Faculty: <strong className="text-slate-700">{fac?.name || 'Assigned Instructor'}</strong></span>
                      <span>Type: <strong className="text-slate-700">{session.sessionType}</strong></span>
                      <span>Planned: <strong className="text-slate-700">{session.plannedUnits} units</strong></span>
                      {isConducted && (
                        <span>Conducted: <strong className="text-emerald-700">{session.conductedUnits} units</strong></span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  <Link to={`/faculty/sessions/${session.id}/attendance`}>
                    <Button size="sm" variant={isScheduled ? 'primary' : 'outline'}>
                      {isScheduled ? 'Open Roll Call' : 'View Sheet'}
                    </Button>
                  </Link>
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
        title="Schedule Master Session"
        subtitle="Schedule class session and assign teaching instructor"
      >
        <form onSubmit={handleScheduleSubmit} className="space-y-4">
          <Select
            label="Curriculum Subject"
            id="admin-sub"
            options={subjects.map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))}
            value={newSubjectId}
            onChange={(e) => setNewSubjectId(e.target.value)}
            required
          />

          <Select
            label="Conducting Faculty Member"
            id="admin-fac"
            options={facultyList.map((f) => ({ value: f.id, label: `${f.name} (${f.employeeId})` }))}
            value={newFacultyId}
            onChange={(e) => setNewFacultyId(e.target.value)}
            required
          />

          <Input
            label="Session Date"
            id="admin-date"
            type="date"
            value={newSessionDate}
            onChange={(e) => setNewSessionDate(e.target.value)}
            required
          />

          <Select
            label="Session Type"
            id="admin-stream"
            options={[
              { value: 'THEORY', label: 'THEORY' },
              { value: 'LAB', label: 'LABORATORY' },
            ]}
            value={newSessionType}
            onChange={(e) => setNewSessionType(e.target.value as 'THEORY' | 'LAB')}
            required
          />

          <Input
            label="Planned Units"
            id="admin-units"
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
    </div>
  );
};

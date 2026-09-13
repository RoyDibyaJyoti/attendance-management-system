import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { academicApi } from '../../api/academicApi';
import { sessionApi } from '../../api/sessionApi';
import { SessionResponse } from '../../types/session';
import { SectionResponse, SubjectResponse } from '../../types/academic';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { EmptyState } from '../../components/common/EmptyState';
import {
  Calendar,
  CheckCircle2,
  Clock,
  PlayCircle,
  Users,
  BookOpen,
  ArrowRight,
} from 'lucide-react';

export const FacultyDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const facultyId = user?.facultyId;

  const [isLoading, setIsLoading] = useState(true);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [sessions, setSessions] = useState<SessionResponse[]>([]);

  useEffect(() => {
    async function loadFacultyData() {
      setIsLoading(true);
      try {
        const [secs, subs] = await Promise.all([
          academicApi.getAllSections(),
          academicApi.getAllSubjects(),
        ]);
        setSections(secs);
        setSubjects(subs);

        // Fetch sessions for all sections
        if (secs.length > 0) {
          const sessionPromises = secs.map((s) => sessionApi.getSessions(s.id));
          const allSessionArrays = await Promise.all(sessionPromises);
          const flat = allSessionArrays.flat();
          // Filter to sessions conducted by or relevant to this faculty
          const facultySessions = flat.filter(
            (s) => !facultyId || s.conductedByFacultyId === facultyId
          );
          setSessions(facultySessions);
        }
      } catch (err) {
        console.error('Failed to load faculty dashboard:', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadFacultyData();
  }, [facultyId]);

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading faculty schedule and rosters..." />;
  }

  const todayStr = new Date().toISOString().split('T')[0];
  const todaySessions = sessions.filter((s) => s.sessionDate === todayStr);
  const pendingSessions = sessions.filter((s) => s.status === 'SCHEDULED');
  const conductedSessions = sessions.filter((s) => s.status === 'CONDUCTED');

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));
  const sectionMap = new Map(sections.map((s) => [s.id, s]));

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Top Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
        <div>
          <Badge variant="purple" className="bg-indigo-600/40 text-indigo-200 border-indigo-500/30 mb-2.5">
            Faculty Teaching Console
          </Badge>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
            Welcome, {user?.username || 'Faculty'}!
          </h1>
          <p className="text-slate-300 text-sm mt-1">
            Faculty Portal | Employee ID: {user?.facultyId ? user.facultyId.slice(0,8).toUpperCase() : 'N/A'}
          </p>
        </div>

        <Link to="/faculty/timetable">
          <Button
            size="md"
            className="bg-indigo-500 hover:bg-indigo-600 text-white"
            leftIcon={<Calendar className="w-4 h-4" />}
          >
            View Full Timetable
          </Button>
        </Link>
      </div>

      {/* KPI Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Today's Sessions"
          value={todaySessions.length}
          subtitle={todaySessions.length > 0 ? 'Ready for roll-call marking' : 'No classes scheduled today'}
          color="indigo"
          icon={<Clock className="w-6 h-6" />}
        />

        <StatCard
          title="Conducted Sessions"
          value={conductedSessions.length}
          subtitle="Attendance locked and computed"
          color="emerald"
          icon={<CheckCircle2 className="w-6 h-6" />}
        />

        <StatCard
          title="Pending Roll-Calls"
          value={pendingSessions.length}
          subtitle={pendingSessions.length > 0 ? 'Requires attendance submission' : 'All scheduled sessions marked'}
          color={pendingSessions.length > 0 ? 'amber' : 'slate'}
          icon={<PlayCircle className="w-6 h-6" />}
        />

        <StatCard
          title="Teaching Sections"
          value={sections.length}
          subtitle="Active cohorts in Fall 2026"
          color="slate"
          icon={<Users className="w-6 h-6" />}
        />
      </div>

      {/* Pending Roll-Calls Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Immediate Classroom Action</h2>
            <p className="text-xs text-slate-500">Scheduled sessions waiting for attendance recording</p>
          </div>
          <Link to="/faculty/timetable">
            <span className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
              View All Timetables <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </Link>
        </div>

        {pendingSessions.length === 0 ? (
          <div className="bg-white border border-slate-200/80 rounded-xl p-8 text-center">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
            <h3 className="text-base font-semibold text-slate-800">All Scheduled Sessions Up to Date</h3>
            <p className="text-xs text-slate-500 mt-1">
              No unconducted sessions pending attendance recording.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {pendingSessions.map((session) => {
              const sub = subjectMap.get(session.subjectId);
              const sec = sectionMap.get(session.sectionId);

              return (
                <div
                  key={session.id}
                  className="bg-white rounded-xl border border-indigo-100 shadow-xs hover:shadow-md transition-shadow p-5 flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                        {sub?.code || 'COURSE'}
                      </span>
                      <Badge variant="warning" size="sm">
                        SCHEDULED
                      </Badge>
                    </div>

                    <h3 className="text-base font-bold text-slate-900 leading-snug">
                      {sub?.name || 'Class Session'}
                    </h3>

                    <div className="mt-3 space-y-1.5 text-xs text-slate-500">
                      <div className="flex items-center gap-2">
                        <Users className="w-3.5 h-3.5 text-slate-400" />
                        <span>Section: {sec?.name || 'Section'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>Date: {session.sessionDate}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <BookOpen className="w-3.5 h-3.5 text-slate-400" />
                        <span>Type: {session.sessionType} ({session.plannedUnits} units)</span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-5 pt-4 border-t border-slate-100">
                    <Link to={`/faculty/sessions/${session.id}/attendance`}>
                      <Button
                        size="sm"
                        className="w-full bg-indigo-600 hover:bg-indigo-700 text-white"
                        leftIcon={<PlayCircle className="w-4 h-4" />}
                      >
                        Start Roll Call
                      </Button>
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { sessionApi } from '../../api/sessionApi';
import { attendanceApi } from '../../api/attendanceApi';
import { studentApi } from '../../api/studentApi';
import { enrollmentApi } from '../../api/enrollmentApi';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { SessionResponse } from '../../types/session';
import { AttendanceStatus, SessionAttendanceSummaryResponse } from '../../types/attendance';
import { StudentResponse } from '../../types/student';
import { SubjectResponse } from '../../types/academic';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  CheckCircle,
  XCircle,
  ArrowLeft,
  Users,
  Calendar,
  Save,
  CheckCheck,
  Ban,
  Clock,
  Award,
} from 'lucide-react';
import { ApiError } from '../../api/client';

interface StudentRosterItem {
  studentId: string;
  name: string;
  registrationNumber: string;
  status: AttendanceStatus;
}

export const FacultyRollCallPage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [subject, setSubject] = useState<SubjectResponse | null>(null);
  const [isConducted, setIsConducted] = useState(false);
  const [summary, setSummary] = useState<SessionAttendanceSummaryResponse | null>(null);

  // Roster state
  const [roster, setRoster] = useState<StudentRosterItem[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    async function loadRoster() {
      if (!sessionId) return;
      setIsLoading(true);
      try {
        const sess = await sessionApi.getSessionById(sessionId);
        setSession(sess);

        const sub = await academicApi.getAllSubjects().then((list) =>
          list.find((s) => s.id === sess.subjectId)
        );
        if (sub) setSubject(sub);

        if (sess.status === 'CONDUCTED') {
          setIsConducted(true);
          const att = await attendanceApi.getSessionAttendance(sessionId);
          setSummary(att);

          // Map students
          const allStudents = await studentApi.getAllStudents();
          const studentMap = new Map(allStudents.map((s) => [s.id, s]));

          const conductedRoster: StudentRosterItem[] = att.records.map((r) => {
            const s = studentMap.get(r.studentId);
            return {
              studentId: r.studentId,
              name: s?.name || 'Unknown Student',
              registrationNumber: s?.registrationNumber || 'N/A',
              status: r.status,
            };
          });
          setRoster(conductedRoster);
        } else {
          // Unconducted session: fetch enrolled students for this section directly
          const [sectionEnrollments, allStudents] = await Promise.all([
            enrollmentApi.getSectionEnrollments(sess.sectionId),
            studentApi.getAllStudents(),
          ]);

          const studentMap = new Map(allStudents.map((s) => [s.id, s]));
          const activeEnrollments = sectionEnrollments.filter((e) => e.status === 'ACTIVE');

          const enrolledStudents = activeEnrollments
            .map((e) => studentMap.get(e.studentId))
            .filter((s): s is StudentResponse => !!s);

          // If no section-specific enrollments found, fallback to all students
          const studentList = enrolledStudents.length > 0 ? enrolledStudents : allStudents;

          const initialRoster: StudentRosterItem[] = studentList.map((st) => ({
            studentId: st.id,
            name: st.name,
            registrationNumber: st.registrationNumber,
            status: 'PRESENT',
          }));
          setRoster(initialRoster);
        }
      } catch (err) {
        console.error('Failed to load session roll call:', err);
        showToast('Failed to load classroom roster.', 'error');
      } finally {
        setIsLoading(false);
      }
    }
    loadRoster();
  }, [sessionId, showToast]);

  const handleStatusChange = (studentId: string, status: AttendanceStatus) => {
    if (isConducted) return;
    setRoster((prev) =>
      prev.map((item) => (item.studentId === studentId ? { ...item, status } : item))
    );
  };

  const handleMarkAll = (status: AttendanceStatus) => {
    if (isConducted) return;
    setRoster((prev) => prev.map((item) => ({ ...item, status })));
    showToast(`Marked all students as ${status}.`, 'info');
  };

  const handleSubmit = async () => {
    if (!sessionId || isConducted || roster.length === 0) return;

    setIsSubmitting(true);
    try {
      const records = roster.map((r) => ({
        studentId: r.studentId,
        status: r.status,
      }));

      await attendanceApi.recordAttendanceBatch(sessionId, { records });
      showToast('Attendance recorded and committed successfully!', 'success');
      navigate('/faculty/timetable');
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to record attendance batch.', 'error');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading student roster for roll-call..." />;
  }

  if (!session) {
    return (
      <div className="text-center py-12">
        <h2 className="text-lg font-bold text-slate-800">Session Not Found</h2>
        <Link to="/faculty/timetable" className="mt-4 inline-block text-indigo-600 font-medium">
          Return to Timetable
        </Link>
      </div>
    );
  }

  const presentCount = roster.filter((r) => r.status === 'PRESENT').length;
  const absentCount = roster.filter((r) => r.status === 'ABSENT').length;
  const otherCount = roster.length - presentCount - absentCount;

  return (
    <div className="space-y-6 max-w-5xl mx-auto animate-in fade-in duration-150">
      {/* Navigation and Header */}
      <div className="flex items-center gap-3">
        <Link to="/faculty/timetable">
          <Button variant="outline" size="sm" leftIcon={<ArrowLeft className="w-4 h-4" />}>
            Back to Timetable
          </Button>
        </Link>
      </div>

      {/* Session Banner */}
      <div className="bg-white rounded-2xl border border-slate-200/90 p-6 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
              {subject?.code || 'COURSE'}
            </span>
            <Badge variant={isConducted ? 'success' : 'warning'} dot>
              {session.status}
            </Badge>
          </div>

          <h1 className="text-2xl font-bold text-slate-900 leading-tight">
            {subject?.name || 'Class Occurrence'}
          </h1>

          <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 mt-2">
            <span className="flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-slate-400" />
              {session.sessionDate}
            </span>
            <span className="flex items-center gap-1.5">
              <Users className="w-4 h-4 text-slate-400" />
              Stream: {session.sessionType} ({session.plannedUnits} units)
            </span>
          </div>
        </div>

        {/* Live Counter Badges */}
        <div className="flex items-center gap-2 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          <div className="px-3 py-1.5 rounded-lg bg-white border border-slate-200/80 text-center">
            <span className="text-[10px] uppercase font-bold text-slate-400 block">Total</span>
            <span className="text-base font-bold text-slate-900">{roster.length}</span>
          </div>
          <div className="px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200 text-center">
            <span className="text-[10px] uppercase font-bold text-emerald-600 block">Present</span>
            <span className="text-base font-bold text-emerald-700">{presentCount}</span>
          </div>
          <div className="px-3 py-1.5 rounded-lg bg-rose-50 border border-rose-200 text-center">
            <span className="text-[10px] uppercase font-bold text-rose-600 block">Absent</span>
            <span className="text-base font-bold text-rose-700">{absentCount}</span>
          </div>
          {otherCount > 0 && (
            <div className="px-3 py-1.5 rounded-lg bg-amber-50 border border-amber-200 text-center">
              <span className="text-[10px] uppercase font-bold text-amber-600 block">Leave</span>
              <span className="text-base font-bold text-amber-700">{otherCount}</span>
            </div>
          )}
        </div>
      </div>

      {/* Conducted Notice or Batch Actions Toolbar */}
      {isConducted ? (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm font-medium flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>Attendance has already been recorded and locked for this session.</span>
          </div>
          <Link to="/faculty/corrections">
            <Button size="sm" variant="outline" className="bg-white text-emerald-800 border-emerald-300">
              Need a Correction?
            </Button>
          </Link>
        </div>
      ) : (
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Quick Actions:
            </span>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleMarkAll('PRESENT')}
              leftIcon={<CheckCheck className="w-4 h-4 text-emerald-600" />}
            >
              Mark All Present
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleMarkAll('ABSENT')}
              leftIcon={<Ban className="w-4 h-4 text-rose-600" />}
            >
              Mark All Absent
            </Button>
          </div>

          <Button
            size="md"
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold"
            isLoading={isSubmitting}
            onClick={handleSubmit}
            leftIcon={<Save className="w-4 h-4" />}
          >
            Submit Roll-Call ({roster.length} Records)
          </Button>
        </div>
      )}

      {/* Student Roster Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">
            Enrolled Student Roll-Call
          </h3>
          <span className="text-xs text-slate-400 font-medium">{roster.length} enrolled learners</span>
        </div>

        <div className="divide-y divide-slate-100">
          {roster.map((student, idx) => {
            const isPresent = student.status === 'PRESENT';
            const isAbsent = student.status === 'ABSENT';
            const isDutyLeave = student.status === 'DUTY_LEAVE';
            const isMedicalLeave = student.status === 'MEDICAL_LEAVE';
            const isOnDuty = student.status === 'ON_DUTY';

            return (
              <div
                key={student.studentId}
                className="px-6 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/70 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono text-slate-400 w-6 text-right">
                    {idx + 1}.
                  </span>
                  <div className="w-9 h-9 rounded-full bg-slate-100 text-slate-600 font-bold text-xs flex items-center justify-center shrink-0">
                    {student.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-slate-900">{student.name}</h4>
                    <span className="text-xs font-mono text-indigo-600 font-medium">
                      {student.registrationNumber}
                    </span>
                  </div>
                </div>

                {/* Status Toggle Buttons */}
                <div className="flex items-center gap-1.5 self-end sm:self-center">
                  <button
                    type="button"
                    disabled={isConducted}
                    onClick={() => handleStatusChange(student.studentId, 'PRESENT')}
                    className={`px-3 py-1 rounded-lg text-xs font-semibold transition-colors ${
                      isPresent
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    PRESENT
                  </button>

                  <button
                    type="button"
                    disabled={isConducted}
                    onClick={() => handleStatusChange(student.studentId, 'ABSENT')}
                    className={`px-3 py-1 rounded-lg text-xs font-semibold transition-colors ${
                      isAbsent
                        ? 'bg-rose-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    ABSENT
                  </button>

                  <button
                    type="button"
                    disabled={isConducted}
                    onClick={() => handleStatusChange(student.studentId, 'DUTY_LEAVE')}
                    className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
                      isDutyLeave
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                    title="Official Institutional Duty Leave"
                  >
                    DUTY
                  </button>

                  <button
                    type="button"
                    disabled={isConducted}
                    onClick={() => handleStatusChange(student.studentId, 'MEDICAL_LEAVE')}
                    className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
                      isMedicalLeave
                        ? 'bg-amber-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                    title="Documented Medical Leave"
                  >
                    MED
                  </button>

                  <button
                    type="button"
                    disabled={isConducted}
                    onClick={() => handleStatusChange(student.studentId, 'ON_DUTY')}
                    className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
                      isOnDuty
                        ? 'bg-purple-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                    title="Faculty Sanctioned On-Duty"
                  >
                    OD
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

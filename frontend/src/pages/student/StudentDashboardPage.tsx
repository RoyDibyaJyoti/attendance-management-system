import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { enrollmentApi } from '../../api/enrollmentApi';
import { academicApi } from '../../api/academicApi';
import { policyApi } from '../../api/policyApi';
import { calculationApi } from '../../api/calculationApi';
import {
  OverallAttendanceSummaryResponse,
  ShortageProjectionResponse,
  StudentAttendanceOverviewResponse,
  SubjectAttendanceSummaryResponse,
} from '../../types/calculation';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { EmptyState } from '../../components/common/EmptyState';
import {
  AlertTriangle,
  Award,
  BookOpen,
  CheckCircle,
  Percent,
  TrendingUp,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const StudentDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const studentId = user?.studentId;

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [sectionId, setSectionId] = useState<string | null>(null);
  const [periodId, setPeriodId] = useState<string | null>(null);
  const [policyId, setPolicyId] = useState<string | null>(null);

  const [overallSummary, setOverallSummary] = useState<OverallAttendanceSummaryResponse | null>(null);
  const [overview, setOverview] = useState<StudentAttendanceOverviewResponse | null>(null);

  // Shortage Projection Modal state
  const [selectedSubject, setSelectedSubject] = useState<SubjectAttendanceSummaryResponse | null>(null);
  const [projection, setProjection] = useState<ShortageProjectionResponse | null>(null);
  const [projectedUnits, setProjectedUnits] = useState<number>(10);
  const [isProjectionLoading, setIsProjectionLoading] = useState(false);

  const loadData = useCallback(async () => {
    if (!studentId) return;
    setIsLoading(true);
    setError(null);

    try {
      // 1. Fetch student enrollments
      const enrollments = await enrollmentApi.getStudentEnrollments(studentId);
      if (enrollments.length === 0) {
        setIsLoading(false);
        return;
      }

      // Find the active enrollment (status === 'ACTIVE' or most recent)
      const activeEnrollment = enrollments.find((e) => e.status === 'ACTIVE') || enrollments[0];
      const secId = activeEnrollment.sectionId;
      setSectionId(secId);

      // 2. Get section directly to find its academicPeriodId
      const section = await academicApi.getSectionById(secId);
      const perId = section.academicPeriodId;

      if (!perId) {
        throw new Error('No academic period found for enrollment.');
      }

      setPeriodId(perId);

      // 3. Fetch default policy
      const policies = await policyApi.getPolicyVersions('Standard University Policy');
      const activePolicy = policies.find((p) => p.active) || policies[0];
      if (!activePolicy) {
        throw new Error('No attendance policy found. Please contact your administrator.');
      }
      const polId = activePolicy.id;
      setPolicyId(polId);

      // 4. Fetch calculation overview and overall summary in parallel
      const [overall, studentOverview] = await Promise.all([
        calculationApi.getOverallAttendance(studentId, secId, polId, perId),
        calculationApi.getStudentOverview(studentId, secId, polId, perId),
      ]);

      setOverallSummary(overall);
      setOverview(studentOverview);
    } catch (err: unknown) {
      console.error('Failed to load student dashboard:', err);
      setError('Unable to load attendance records. Please try again later.');
    } finally {
      setIsLoading(false);
    }
  }, [studentId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Load projection for selected subject
  const openProjectionModal = async (subject: SubjectAttendanceSummaryResponse) => {
    if (!studentId || !sectionId || !policyId || !periodId) return;
    setSelectedSubject(subject);
    setIsProjectionLoading(true);
    setProjection(null);
    try {
      const res = await calculationApi.getShortageProjection(
        studentId,
        subject.subjectId,
        sectionId,
        policyId,
        periodId,
        projectedUnits
      );
      setProjection(res);
    } catch (err) {
      console.error('Failed to load projection:', err);
    } finally {
      setIsProjectionLoading(false);
    }
  };

  const handleRecalculateProjection = async (units: number) => {
    if (!studentId || !sectionId || !policyId || !periodId || !selectedSubject) return;
    setProjectedUnits(units);
    setIsProjectionLoading(true);
    try {
      const res = await calculationApi.getShortageProjection(
        studentId,
        selectedSubject.subjectId,
        sectionId,
        policyId,
        periodId,
        units
      );
      setProjection(res);
    } catch (err) {
      console.error('Failed to recalculate projection:', err);
    } finally {
      setIsProjectionLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Calculating your attendance overview..." />;
  }

  if (error) {
    return (
      <div className="p-6 bg-rose-50 border border-rose-200 rounded-2xl text-rose-800">
        <h3 className="font-semibold text-base">Error Loading Attendance</h3>
        <p className="text-sm mt-1 mb-4">{error}</p>
        <Button variant="danger" size="sm" onClick={loadData}>
          Try Again
        </Button>
      </div>
    );
  }

  if (!sectionId || !overview || overview.subjects.length === 0) {
    return (
      <EmptyState
        title="No Enrollments Found"
        description="You are not currently enrolled in any active class section. Please contact your department administrator."
      />
    );
  }

  const overallPercentage = Number(overallSummary?.overallPercentage ?? 0);
  const isOverallShortage = overallSummary?.isShortage ?? false;
  const subjectsAtRisk = overview.subjects.filter((s) => s.isShortage).length;

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-indigo-950 rounded-2xl p-6 sm:p-8 text-white shadow-lg relative overflow-hidden">
        <div className="relative z-10 max-w-2xl">
          <Badge variant="purple" className="bg-indigo-700/50 text-indigo-100 border-indigo-500/30 mb-3">
            Academic Semester Dashboard
          </Badge>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
            Welcome back, {overview.studentName || user?.username}!
          </h1>
          <p className="text-indigo-200 text-sm mt-1">
            Registration: <span className="font-mono text-white">{overview.registrationNumber}</span>
            {' '}| Institutional threshold: {Number(overview.subjects[0]?.thresholdPercentage ?? 75).toFixed(0)}%
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <StatCard
          title="Overall Attendance"
          value={`${overallPercentage.toFixed(2)}%`}
          subtitle={isOverallShortage ? 'Below minimum threshold' : 'Compliant with institutional policy'}
          color={isOverallShortage ? 'rose' : 'emerald'}
          icon={<Percent className="w-6 h-6" />}
        />

        <StatCard
          title="Attendance Status"
          value={
            <Badge
              variant={isOverallShortage ? 'danger' : 'success'}
              size="md"
              dot
            >
              {overallSummary?.classification || (isOverallShortage ? 'SHORTAGE' : 'ADEQUATE')}
            </Badge>
          }
          subtitle="Governed by Standard Policy"
          color={isOverallShortage ? 'rose' : 'emerald'}
          icon={isOverallShortage ? <AlertTriangle className="w-6 h-6" /> : <CheckCircle className="w-6 h-6" />}
        />

        <StatCard
          title="Enrolled Subjects"
          value={overview.subjects.length}
          subtitle="Active theory and lab courses"
          color="indigo"
          icon={<BookOpen className="w-6 h-6" />}
        />

        <StatCard
          title="Subjects At Risk"
          value={subjectsAtRisk}
          subtitle={subjectsAtRisk > 0 ? 'Requires immediate attention' : 'All subjects currently adequate'}
          color={subjectsAtRisk > 0 ? 'amber' : 'slate'}
          icon={<Award className="w-6 h-6" />}
        />
      </div>

      {/* Subject Attendance Cards */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Enrolled Courses</h2>
            <p className="text-xs text-slate-500">Live attendance percentage and shortage forecasting</p>
          </div>
          <Link to="/student/reports">
            <Button variant="outline" size="sm">
              Download RPT-001 Excel
            </Button>
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {overview.subjects.map((sub) => {
            const pct = Number(sub.attendancePercentage ?? 0);
            const isDanger = sub.isShortage;

            return (
              <div
                key={sub.subjectId}
                className={`bg-white rounded-2xl border p-6 shadow-xs hover:shadow-md transition-shadow ${
                  isDanger ? 'border-rose-200/90' : 'border-slate-200/80'
                }`}
              >
                <div className="flex items-start justify-between gap-3 mb-4">
                  <div>
                    <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                      {sub.subjectCode}
                    </span>
                    <h3 className="text-base font-bold text-slate-900 mt-1.5 leading-snug">
                      {sub.subjectName}
                    </h3>
                  </div>
                  <Badge variant={isDanger ? 'danger' : 'success'} dot>
                    {sub.classification || (isDanger ? 'SHORTAGE' : 'ADEQUATE')}
                  </Badge>
                </div>

                {/* Progress bar */}
                <div className="mb-4">
                  <div className="flex justify-between text-xs font-semibold mb-1">
                    <span className="text-slate-600">Attendance</span>
                    <span className={isDanger ? 'text-rose-600 font-bold' : 'text-emerald-700 font-bold'}>
                      {pct.toFixed(2)}%
                    </span>
                  </div>
                  <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        isDanger ? 'bg-rose-500' : 'bg-emerald-500'
                      }`}
                      style={{ width: `${Math.min(pct, 100)}%` }}
                    />
                  </div>
                  {/* Threshold marker */}
                  <div className="relative w-full mt-1">
                    <div
                      className="absolute w-0.5 h-2 bg-amber-500 -top-3"
                      style={{ left: `${Math.min(Number(sub.thresholdPercentage ?? 75), 100)}%` }}
                      title={`Threshold: ${sub.thresholdPercentage}%`}
                    />
                  </div>
                </div>

                {/* Metrics Breakdown */}
                <div className="grid grid-cols-3 gap-2 p-3 bg-slate-50 rounded-xl text-center mb-5">
                  <div>
                    <span className="text-[11px] text-slate-500 block">Attended</span>
                    <span className="text-sm font-bold text-slate-800">{Number(sub.attendedUnits).toFixed(0)}</span>
                  </div>
                  <div>
                    <span className="text-[11px] text-slate-500 block">Conducted</span>
                    <span className="text-sm font-bold text-slate-800">{Number(sub.conductedUnits).toFixed(0)}</span>
                  </div>
                  <div>
                    <span className="text-[11px] text-slate-500 block">Threshold</span>
                    <span className="text-sm font-bold text-slate-800">{Number(sub.thresholdPercentage).toFixed(0)}%</span>
                  </div>
                </div>

                {isDanger && sub.shortageUnits && Number(sub.shortageUnits) > 0 && (
                  <div className="mb-4 px-3 py-2 bg-rose-50 border border-rose-100 rounded-lg text-xs text-rose-700">
                    <strong>⚠ Shortage:</strong> {Number(sub.shortageUnits).toFixed(1)} units below threshold
                  </div>
                )}

                <Button
                  variant="outline"
                  size="sm"
                  className="w-full text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                  leftIcon={<TrendingUp className="w-4 h-4" />}
                  onClick={() => openProjectionModal(sub)}
                >
                  View Recovery & Shortage Projection
                </Button>
              </div>
            );
          })}
        </div>
      </div>

      {/* Shortage Projection Modal */}
      {selectedSubject && (
        <Modal
          isOpen={!!selectedSubject}
          onClose={() => { setSelectedSubject(null); setProjection(null); }}
          title={`Shortage Forecast: ${selectedSubject.subjectCode}`}
          subtitle={selectedSubject.subjectName}
          maxWidth="lg"
          footer={
            <Button variant="secondary" onClick={() => { setSelectedSubject(null); setProjection(null); }}>
              Close
            </Button>
          }
        >
          {isProjectionLoading ? (
            <LoadingSpinner size="md" label="Simulating future attendance projections..." />
          ) : projection ? (
            <div className="space-y-5">
              {/* Feasibility Alert */}
              <div
                className={`p-4 rounded-xl border flex items-start gap-3 ${
                  projection.isPossibleToRecover
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                    : 'bg-rose-50 border-rose-200 text-rose-900'
                }`}
              >
                {projection.isPossibleToRecover ? (
                  <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                ) : (
                  <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
                )}
                <div>
                  <h4 className="text-sm font-bold">
                    {projection.isPossibleToRecover
                      ? 'Recovery Achievable'
                      : 'Mathematically Impossible to Meet Threshold'}
                  </h4>
                  <p className="text-xs mt-0.5 leading-relaxed">
                    {projection.isPossibleToRecover
                      ? `You must attend at least ${projection.minimumAdditionalUnitsRequired} more unit(s) out of the next ${projectedUnits} projected classes to maintain adequate standing.`
                      : `Even if you attend 100% of the next ${projectedUnits} sessions, your maximum reachable percentage is below the ${Number(projection.thresholdPercentage).toFixed(0)}% threshold.`}
                  </p>
                </div>
              </div>

              {/* Stat Grid */}
              <div className="grid grid-cols-2 gap-3">
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <span className="text-xs text-slate-500 block">Current Attendance</span>
                  <span className="text-xl font-bold text-slate-900 mt-1 block">
                    {Number(projection.currentPercentage).toFixed(2)}%
                  </span>
                </div>

                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <span className="text-xs text-slate-500 block">Classes You Can Miss</span>
                  <span className="text-xl font-bold text-indigo-600 mt-1 block">
                    {projection.maximumAllowableAbsenceUnits}
                  </span>
                </div>

                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <span className="text-xs text-slate-500 block">Min. Additional Required</span>
                  <span className="text-xl font-bold text-amber-600 mt-1 block">
                    {projection.minimumAdditionalUnitsRequired}
                  </span>
                </div>

                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-center">
                  <span className="text-xs text-slate-500 block">Projected Sessions</span>
                  <span className="text-xl font-bold text-slate-700 mt-1 block">
                    {projection.projectedRemainingUnits}
                  </span>
                </div>
              </div>

              {/* Slider for projected remaining units */}
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                <div className="flex justify-between items-center mb-2">
                  <label htmlFor="projection-slider" className="text-xs font-semibold text-slate-700">
                    Simulate Projected Remaining Classes:
                  </label>
                  <span className="text-sm font-bold text-indigo-600">{projectedUnits} classes</span>
                </div>
                <input
                  id="projection-slider"
                  type="range"
                  min="1"
                  max="40"
                  value={projectedUnits}
                  onChange={(e) => handleRecalculateProjection(parseInt(e.target.value, 10))}
                  className="w-full accent-indigo-600 cursor-pointer"
                />
                <div className="flex justify-between text-[11px] text-slate-400 mt-1">
                  <span>1 class</span>
                  <span>20 classes</span>
                  <span>40 classes</span>
                </div>
              </div>
            </div>
          ) : (
            <p className="text-sm text-slate-500 py-4 text-center">
              Failed to compute projection. This subject may have no conducted sessions yet.
            </p>
          )}
        </Modal>
      )}
    </div>
  );
};

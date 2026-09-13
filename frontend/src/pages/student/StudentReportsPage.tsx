import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { enrollmentApi } from '../../api/enrollmentApi';
import { academicApi } from '../../api/academicApi';
import { reportApi } from '../../api/reportApi';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Download, FileSpreadsheet, CheckCircle2 } from 'lucide-react';

export const StudentReportsPage: React.FC = () => {
  const { user } = useAuth();
  const studentId = user?.studentId;
  const { showToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [periodId, setPeriodId] = useState<string | null>(null);
  const [sectionId, setSectionId] = useState<string | null>(null);

  useEffect(() => {
    async function fetchEnrollment() {
      if (!studentId) return;
      try {
        const enrollments = await enrollmentApi.getStudentEnrollments(studentId);
        if (enrollments.length > 0) {
          const active = enrollments.find((e) => e.status === 'ACTIVE') || enrollments[0];
          const secId = active.sectionId;
          setSectionId(secId);

          // Get periodId from the section's academicPeriodId
          const allPeriods = await academicApi.getPeriods();
          let foundPeriodId: string | null = null;
          for (const period of allPeriods) {
            const sections = await academicApi.getSectionsByPeriod(period.id);
            const found = sections.find((s) => s.id === secId);
            if (found) {
              foundPeriodId = found.academicPeriodId;
              break;
            }
          }
          if (!foundPeriodId && allPeriods.length > 0) {
            foundPeriodId = allPeriods[0].id;
          }
          setPeriodId(foundPeriodId);
        }
      } catch (err) {
        console.error('Failed to load enrollments for reports:', err);
      } finally {
        setIsLoading(false);
      }
    }
    fetchEnrollment();
  }, [studentId]);

  const handleDownload = async () => {
    if (!studentId || !periodId) return;
    setIsDownloading(true);
    try {
      await reportApi.downloadRpt001({
        studentId,
        academicPeriodId: periodId,
        sectionId: sectionId || undefined,
      });
      showToast('Attendance report (RPT-001) downloaded successfully!', 'success');
    } catch (err: unknown) {
      console.error('Download error:', err);
      showToast('Failed to generate attendance report. Please try again.', 'error');
    } finally {
      setIsDownloading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Preparing reports portal..." />;
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Student Reports Portal</h1>
        <p className="text-sm text-slate-500 mt-1">
          Export verified institutional attendance records in standard XLSX format
        </p>
      </div>

      <Card
        title="RPT-001: Individual Student Attendance Statement"
        subtitle="Full semester attendance ledger per course with attendance percentages and shortage indicators."
        className="border-indigo-100"
      >
        <div className="space-y-4">
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-100 flex items-start gap-3">
            <FileSpreadsheet className="w-8 h-8 text-indigo-600 shrink-0" />
            <div className="text-sm">
              <h4 className="font-semibold text-slate-800">Official Institutional Export</h4>
              <p className="text-slate-500 text-xs mt-0.5">
                Contains complete roll-call logs, conduct unit counts, leave breakdowns (medical, duty, on-duty), and semester compliance standing.
              </p>
            </div>
          </div>

          {!periodId && (
            <div className="px-4 py-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800">
              ⚠ No enrollment found. Please contact your department administrator to enroll in a section.
            </div>
          )}

          <div className="flex items-center justify-between pt-2">
            <div className="flex items-center gap-2 text-xs font-medium text-emerald-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Verified for current enrolled semester</span>
            </div>

            <Button
              onClick={handleDownload}
              isLoading={isDownloading}
              disabled={!periodId}
              leftIcon={<Download className="w-4 h-4" />}
            >
              Export XLSX
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
};

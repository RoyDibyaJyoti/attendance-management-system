import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { reportApi } from '../../api/reportApi';
import { useToast } from '../../context/ToastContext';
import { SectionResponse, SubjectResponse, AcademicPeriodResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Select } from '../../components/common/Select';
import { Input } from '../../components/common/Input';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Download, FileSpreadsheet } from 'lucide-react';

export const FacultyReportsPage: React.FC = () => {
  const { showToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);

  // Selection
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedPeriodId, setSelectedPeriodId] = useState('');

  // Downloading state
  const [downloadingRpt, setDownloadingRpt] = useState<string | null>(null);

  useEffect(() => {
    async function init() {
      try {
        const [secs, subs, pers] = await Promise.all([
          academicApi.getAllSections(),
          academicApi.getAllSubjects(),
          academicApi.getPeriods(),
        ]);
        setSections(secs);
        setSubjects(subs);
        setPeriods(pers);

        if (secs.length > 0) setSelectedSectionId(secs[0].id);
        if (subs.length > 0) setSelectedSubjectId(subs[0].id);
        if (pers.length > 0) setSelectedPeriodId(pers[0].id);
      } catch (err) {
        console.error('Failed to load report options:', err);
      } finally {
        setIsLoading(false);
      }
    }
    init();
  }, []);

  const handleDownloadRpt002 = async () => {
    if (!selectedSubjectId || !selectedSectionId || !selectedPeriodId) return;
    setDownloadingRpt('rpt-002');
    try {
      await reportApi.downloadRpt002({
        subjectId: selectedSubjectId,
        sectionId: selectedSectionId,
        academicPeriodId: selectedPeriodId,
      });
      showToast('Subject Attendance Summary (RPT-002) downloaded.', 'success');
    } catch (err) {
      showToast('Failed to download RPT-002.', 'error');
    } finally {
      setDownloadingRpt(null);
    }
  };

  const handleDownloadRpt004 = async () => {
    if (!selectedSubjectId || !selectedSectionId || !selectedPeriodId) return;
    setDownloadingRpt('rpt-004');
    try {
      await reportApi.downloadRpt004({
        subjectId: selectedSubjectId,
        sectionId: selectedSectionId,
        academicPeriodId: selectedPeriodId,
      });
      showToast('Attendance Register (RPT-004) downloaded.', 'success');
    } catch (err) {
      showToast('Failed to download RPT-004.', 'error');
    } finally {
      setDownloadingRpt(null);
    }
  };

  const handleDownloadRpt008 = async () => {
    if (!selectedSubjectId || !selectedSectionId || !selectedPeriodId) return;
    setDownloadingRpt('rpt-008');
    try {
      await reportApi.downloadRpt008({
        subjectId: selectedSubjectId,
        sectionId: selectedSectionId,
        academicPeriodId: selectedPeriodId,
        futureSessions: 20,
        threshold: 75.0,
      });
      showToast('Defaulter Prediction Report (RPT-008) downloaded.', 'success');
    } catch (err) {
      showToast('Failed to download RPT-008.', 'error');
    } finally {
      setDownloadingRpt(null);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading faculty report options..." />;
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Faculty Reports Portal</h1>
        <p className="text-sm text-slate-500 mt-1">
          Export subject rosters, date-wise registers, and future recovery projections
        </p>
      </div>

      {/* Filter Parameters */}
      <Card title="Target Parameters" subtitle="Select section, course, and academic semester">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Select
            label="Section"
            id="rep-section"
            options={sections.map((s) => ({ value: s.id, label: s.name }))}
            value={selectedSectionId}
            onChange={(e) => setSelectedSectionId(e.target.value)}
          />

          <Select
            label="Subject"
            id="rep-subject"
            options={subjects.map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))}
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
          />

          <Select
            label="Semester / Period"
            id="rep-period"
            options={periods.map((p) => ({ value: p.id, label: p.name }))}
            value={selectedPeriodId}
            onChange={(e) => setSelectedPeriodId(e.target.value)}
          />
        </div>
      </Card>

      {/* Available Reports */}
      <div className="space-y-4">
        {/* RPT-002 */}
        <Card className="hover:border-slate-300 transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  RPT-002: Subject Attendance Summary
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 max-w-xl">
                  Per-student roll, attended units, conducted units, attendance percentage, and shortage standing.
                </p>
              </div>
            </div>
            <Button
              size="sm"
              isLoading={downloadingRpt === 'rpt-002'}
              onClick={handleDownloadRpt002}
              leftIcon={<Download className="w-4 h-4" />}
            >
              Export XLSX
            </Button>
          </div>
        </Card>

        {/* RPT-004 */}
        <Card className="hover:border-slate-300 transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  RPT-004: Class Attendance Register
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 max-w-xl">
                  Date-wise session matrix showing daily mark (P, A, DL, ML, OD) for each student across the semester.
                </p>
              </div>
            </div>
            <Button
              size="sm"
              isLoading={downloadingRpt === 'rpt-004'}
              onClick={handleDownloadRpt004}
              leftIcon={<Download className="w-4 h-4" />}
            >
              Export XLSX
            </Button>
          </div>
        </Card>

        {/* RPT-008 */}
        <Card className="hover:border-slate-300 transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  RPT-008: Defaulter Prediction & Recovery
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 max-w-xl">
                  Projects required future attendance to clear shortage threshold (75%) over next 20 sessions.
                </p>
              </div>
            </div>
            <Button
              size="sm"
              isLoading={downloadingRpt === 'rpt-008'}
              onClick={handleDownloadRpt008}
              leftIcon={<Download className="w-4 h-4" />}
            >
              Export XLSX
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
};

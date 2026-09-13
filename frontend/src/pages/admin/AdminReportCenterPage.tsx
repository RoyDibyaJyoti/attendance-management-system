import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { studentApi } from '../../api/studentApi';
import { facultyApi } from '../../api/facultyApi';
import { reportApi } from '../../api/reportApi';
import { useToast } from '../../context/ToastContext';
import {
  DepartmentResponse,
  SectionResponse,
  SubjectResponse,
  AcademicPeriodResponse,
} from '../../types/academic';
import { StudentResponse } from '../../types/student';
import { FacultyResponse } from '../../types/faculty';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Select } from '../../components/common/Select';
import { Input } from '../../components/common/Input';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { FileSpreadsheet, Download, Filter } from 'lucide-react';

export const AdminReportCenterPage: React.FC = () => {
  const { showToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [facultyList, setFacultyList] = useState<FacultyResponse[]>([]);

  // Selected Filter States
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [selectedFacultyId, setSelectedFacultyId] = useState('');
  const [threshold, setThreshold] = useState(75.0);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // Downloading State
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    async function loadDependencies() {
      try {
        const [depts, pers, secs, subs, studs, facs] = await Promise.all([
          academicApi.getDepartments(),
          academicApi.getPeriods(),
          academicApi.getAllSections(),
          academicApi.getAllSubjects(),
          studentApi.getAllStudents(),
          facultyApi.getAllFaculty(),
        ]);

        setDepartments(depts);
        setPeriods(pers);
        setSections(secs);
        setSubjects(subs);
        setStudents(studs);
        setFacultyList(facs);

        if (pers.length > 0) setSelectedPeriodId(pers[0].id);
        if (secs.length > 0) setSelectedSectionId(secs[0].id);
        if (subs.length > 0) setSelectedSubjectId(subs[0].id);
        if (studs.length > 0) setSelectedStudentId(studs[0].id);
        if (facs.length > 0) setSelectedFacultyId(facs[0].id);
      } catch (err) {
        console.error('Failed to load report center options:', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadDependencies();
  }, []);

  const handleExport = async (reportId: string) => {
    if (!selectedPeriodId) {
      showToast('Please select an academic period.', 'warning');
      return;
    }

    setDownloadingId(reportId);
    try {
      switch (reportId) {
        case 'rpt-001':
          if (!selectedStudentId) return showToast('Please select a student for RPT-001', 'warning');
          await reportApi.downloadRpt001({
            studentId: selectedStudentId,
            academicPeriodId: selectedPeriodId,
            sectionId: selectedSectionId || undefined,
          });
          break;
        case 'rpt-002':
          if (!selectedSubjectId || !selectedSectionId)
            return showToast('Select subject and section for RPT-002', 'warning');
          await reportApi.downloadRpt002({
            subjectId: selectedSubjectId,
            sectionId: selectedSectionId,
            academicPeriodId: selectedPeriodId,
          });
          break;
        case 'rpt-003':
          await reportApi.downloadRpt003({
            academicPeriodId: selectedPeriodId,
            sectionId: selectedSectionId || undefined,
            subjectId: selectedSubjectId || undefined,
            threshold,
          });
          break;
        case 'rpt-004':
          if (!selectedSubjectId || !selectedSectionId)
            return showToast('Select subject and section for RPT-004', 'warning');
          await reportApi.downloadRpt004({
            subjectId: selectedSubjectId,
            sectionId: selectedSectionId,
            academicPeriodId: selectedPeriodId,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          });
          break;
        case 'rpt-005':
          await reportApi.downloadRpt005({
            academicPeriodId: selectedPeriodId,
            sectionId: selectedSectionId || undefined,
            threshold,
          });
          break;
        case 'rpt-006':
          await reportApi.downloadRpt006({
            academicPeriodId: selectedPeriodId,
            facultyId: selectedFacultyId || undefined,
          });
          break;
        case 'rpt-007':
          await reportApi.downloadRpt007({
            academicPeriodId: selectedPeriodId,
            sectionId: selectedSectionId || undefined,
            subjectId: selectedSubjectId || undefined,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
          });
          break;
        case 'rpt-008':
          if (!selectedSubjectId || !selectedSectionId)
            return showToast('Select subject and section for RPT-008', 'warning');
          await reportApi.downloadRpt008({
            subjectId: selectedSubjectId,
            sectionId: selectedSectionId,
            academicPeriodId: selectedPeriodId,
            futureSessions: 20,
            threshold,
          });
          break;
      }
      showToast(`${reportId.toUpperCase()} spreadsheet generated and downloaded.`, 'success');
    } catch (err) {
      console.error('Report download error:', err);
      showToast(`Failed to export ${reportId.toUpperCase()}.`, 'error');
    } finally {
      setDownloadingId(null);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading institutional report center..." />;
  }

  const reports = [
    {
      id: 'rpt-001',
      title: 'RPT-001: Student Attendance Statement',
      desc: 'Per-student, per-course attendance breakdown and shortage ledger across semester.',
      color: 'indigo',
    },
    {
      id: 'rpt-002',
      title: 'RPT-002: Subject Attendance Summary',
      desc: 'Class roster summary with attended units, conducted units, and percentages.',
      color: 'emerald',
    },
    {
      id: 'rpt-003',
      title: 'RPT-003: Institutional Defaulter Report',
      desc: 'Identifies all students falling below threshold percentage with shortage metrics.',
      color: 'rose',
    },
    {
      id: 'rpt-004',
      title: 'RPT-004: Daily Attendance Register',
      desc: 'Date-wise session matrix showing daily roll-call mark for every student.',
      color: 'indigo',
    },
    {
      id: 'rpt-005',
      title: 'RPT-005: Overall Attendance Summary',
      desc: 'Aggregated attendance percentage per student across all enrolled courses.',
      color: 'amber',
    },
    {
      id: 'rpt-006',
      title: 'RPT-006: Faculty Marking Compliance',
      desc: 'Tracks marked vs unmarked scheduled sessions by faculty instructor.',
      color: 'purple',
    },
    {
      id: 'rpt-007',
      title: 'RPT-007: Condonation & Duty Leave Register',
      desc: 'Audit log of all excused duty leaves and medical certificates submitted.',
      color: 'blue',
    },
    {
      id: 'rpt-008',
      title: 'RPT-008: Attendance Recovery Prediction',
      desc: 'Mathematical forecast of classes required to clear defaulter status.',
      color: 'emerald',
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Institutional Report Center</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Generate and stream official university XLSX workbooks for audit and administrative compliance
        </p>
      </div>

      {/* Global Filter Toolbar */}
      <Card title="Report Filter Parameters" subtitle="Configure target period, cohort, courses, and thresholds">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Select
            label="Academic Period"
            id="rc-period"
            options={periods.map((p) => ({ value: p.id, label: p.name }))}
            value={selectedPeriodId}
            onChange={(e) => setSelectedPeriodId(e.target.value)}
          />

          <Select
            label="Section"
            id="rc-section"
            options={sections.map((s) => ({ value: s.id, label: s.name }))}
            value={selectedSectionId}
            onChange={(e) => setSelectedSectionId(e.target.value)}
          />

          <Select
            label="Subject"
            id="rc-subject"
            options={subjects.map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))}
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
          />

          <Select
            label="Student (for RPT-001)"
            id="rc-student"
            options={students.map((s) => ({ value: s.id, label: `${s.registrationNumber} — ${s.name}` }))}
            value={selectedStudentId}
            onChange={(e) => setSelectedStudentId(e.target.value)}
          />

          <Select
            label="Faculty (for RPT-006)"
            id="rc-faculty"
            options={facultyList.map((f) => ({ value: f.id, label: `${f.name} (${f.employeeId})` }))}
            value={selectedFacultyId}
            onChange={(e) => setSelectedFacultyId(e.target.value)}
          />

          <Input
            label="Threshold % (RPT-003/005)"
            id="rc-threshold"
            type="number"
            step="0.1"
            value={threshold}
            onChange={(e) => setThreshold(parseFloat(e.target.value))}
          />

          <Input
            label="Date Range Start"
            id="rc-start"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />

          <Input
            label="Date Range End"
            id="rc-end"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>
      </Card>

      {/* Reports Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {reports.map((rpt) => (
          <Card key={rpt.id} className="hover:border-indigo-200 transition-colors">
            <div className="flex flex-col justify-between h-full">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                  <FileSpreadsheet className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">{rpt.title}</h3>
                  <p className="text-xs text-slate-500 mt-1">{rpt.desc}</p>
                </div>
              </div>

              <div className="mt-5 pt-3 border-t border-slate-100 flex justify-end">
                <Button
                  size="sm"
                  isLoading={downloadingId === rpt.id}
                  onClick={() => handleExport(rpt.id)}
                  leftIcon={<Download className="w-4 h-4" />}
                >
                  Export XLSX
                </Button>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { enrollmentApi } from '../../api/enrollmentApi';
import { studentApi } from '../../api/studentApi';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { StudentResponse } from '../../types/student';
import { SectionResponse, AcademicPeriodResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Select } from '../../components/common/Select';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, ArrowRightLeft, UserCheck, CheckCircle2 } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminEnrollmentsPage: React.FC = () => {
  const { showToast } = useToast();
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Enrollment Modal
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState(false);
  const [enrollStudentId, setEnrollStudentId] = useState('');
  const [enrollSectionId, setEnrollSectionId] = useState('');
  const [enrollPeriodId, setEnrollPeriodId] = useState('');
  const [enrollStartDate, setEnrollStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [isEnrolling, setIsEnrolling] = useState(false);

  // Transfer Modal
  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
  const [transferStudentId, setTransferStudentId] = useState('');
  const [fromSectionId, setFromSectionId] = useState('');
  const [toSectionId, setToSectionId] = useState('');
  const [transferPeriodId, setTransferPeriodId] = useState('');
  const [transferDate, setTransferDate] = useState(new Date().toISOString().split('T')[0]);
  const [isTransferring, setIsTransferring] = useState(false);

  useEffect(() => {
    async function loadData() {
      try {
        const [studs, secs, pers] = await Promise.all([
          studentApi.getAllStudents(),
          academicApi.getAllSections(),
          academicApi.getPeriods(),
        ]);
        setStudents(studs);
        setSections(secs);
        setPeriods(pers);

        if (studs.length > 0) {
          setEnrollStudentId(studs[0].id);
          setTransferStudentId(studs[0].id);
        }
        if (secs.length > 0) {
          setEnrollSectionId(secs[0].id);
          setFromSectionId(secs[0].id);
          if (secs.length > 1) setToSectionId(secs[1].id);
        }
        if (pers.length > 0) {
          setEnrollPeriodId(pers[0].id);
          setTransferPeriodId(pers[0].id);
        }
      } catch (err) {
        console.error('Failed to load enrollment dependencies:', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadData();
  }, []);

  const handleEnrollSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!enrollStudentId || !enrollSectionId || !enrollStartDate) return;

    setIsEnrolling(true);
    try {
      await enrollmentApi.enrollStudent({
        studentId: enrollStudentId,
        sectionId: enrollSectionId,
        enrollmentStart: enrollStartDate,
      });
      showToast('Student enrolled into section successfully.', 'success');
      setIsEnrollModalOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to enroll student.', 'error');
      }
    } finally {
      setIsEnrolling(false);
    }
  };

  const handleTransferSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!transferStudentId || !fromSectionId || !toSectionId || !transferDate) return;

    if (fromSectionId === toSectionId) {
      showToast('Destination section must be different from source section.', 'warning');
      return;
    }

    setIsTransferring(true);
    try {
      await enrollmentApi.transferStudent({
        studentId: transferStudentId,
        fromSectionId,
        toSectionId,
        transferDate,
      });
      showToast('Student transferred between sections with temporal windowing.', 'success');
      setIsTransferModalOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to transfer student.', 'error');
      }
    } finally {
      setIsTransferring(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading student enrollment registry..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Student Enrollments & Transfers</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Temporal section assignments with mid-semester transfer preservation
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={() => setIsTransferModalOpen(true)}
            leftIcon={<ArrowRightLeft className="w-4 h-4" />}
          >
            Transfer Student
          </Button>
          <Button
            onClick={() => setIsEnrollModalOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Enroll Student
          </Button>
        </div>
      </div>

      {/* Directory of Enrolled Students */}
      <Card title="Student Roster Overview" subtitle="Quick view of registered learners ready for enrollment">
        <div className="divide-y divide-slate-100">
          {students.map((st) => (
            <div key={st.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-slate-100 text-slate-700 font-bold text-xs flex items-center justify-center">
                  {st.name.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-slate-900">{st.name}</h4>
                  <span className="font-mono text-xs text-indigo-600 font-medium">
                    {st.registrationNumber}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setEnrollStudentId(st.id);
                    setIsEnrollModalOpen(true);
                  }}
                >
                  Enroll
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setTransferStudentId(st.id);
                    setIsTransferModalOpen(true);
                  }}
                >
                  Transfer
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Enroll Modal */}
      <Modal
        isOpen={isEnrollModalOpen}
        onClose={() => setIsEnrollModalOpen(false)}
        title="Enroll Student in Section"
        subtitle="Only sessions conducted on or after enrollment start are included in denominator"
      >
        <form onSubmit={handleEnrollSubmit} className="space-y-4">
          <Select
            label="Student"
            id="enroll-student"
            options={students.map((s) => ({ value: s.id, label: `${s.registrationNumber} — ${s.name}` }))}
            value={enrollStudentId}
            onChange={(e) => setEnrollStudentId(e.target.value)}
            required
          />

          <Select
            label="Section"
            id="enroll-section"
            options={sections.map((s) => ({ value: s.id, label: s.name }))}
            value={enrollSectionId}
            onChange={(e) => setEnrollSectionId(e.target.value)}
            required
          />

          {/* Period shown for informational context only — backend derives period from sectionId */}
          <div className="text-xs text-slate-500 p-3 bg-slate-50 rounded-lg border border-slate-100">
            <span className="font-medium">Linked Period:</span>{' '}
            {sections.find((s) => s.id === enrollSectionId)
              ? periods.find((p) => p.id === sections.find((s) => s.id === enrollSectionId)?.academicPeriodId)?.name || 'Period determined by section'
              : 'Select a section to see linked period'}
          </div>

          <Input
            label="Enrollment Start Date"
            id="enroll-start"
            type="date"
            value={enrollStartDate}
            onChange={(e) => setEnrollStartDate(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsEnrollModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isEnrolling}>
              Confirm Enrollment
            </Button>
          </div>
        </form>
      </Modal>

      {/* Transfer Modal */}
      <Modal
        isOpen={isTransferModalOpen}
        onClose={() => setIsTransferModalOpen(false)}
        title="Transfer Student Between Sections"
        subtitle="Closes previous enrollment and opens new section enrollment on transfer date"
      >
        <form onSubmit={handleTransferSubmit} className="space-y-4">
          <Select
            label="Student"
            id="transfer-student"
            options={students.map((s) => ({ value: s.id, label: `${s.registrationNumber} — ${s.name}` }))}
            value={transferStudentId}
            onChange={(e) => setTransferStudentId(e.target.value)}
            required
          />

          <Select
            label="Current (From) Section"
            id="from-section"
            options={sections.map((s) => ({ value: s.id, label: s.name }))}
            value={fromSectionId}
            onChange={(e) => setFromSectionId(e.target.value)}
            required
          />

          <Select
            label="Destination (To) Section"
            id="to-section"
            options={sections.map((s) => ({ value: s.id, label: s.name }))}
            value={toSectionId}
            onChange={(e) => setToSectionId(e.target.value)}
            required
          />

          {/* Period shown for informational context only */}
          <div className="text-xs text-slate-500 p-3 bg-slate-50 rounded-lg border border-slate-100">
            <span className="font-medium">Target Period:</span>{' '}
            {sections.find((s) => s.id === toSectionId)
              ? periods.find((p) => p.id === sections.find((s) => s.id === toSectionId)?.academicPeriodId)?.name || 'Period determined by section'
              : 'Select destination section to see linked period'}
          </div>

          <Input
            label="Effective Transfer Date"
            id="transfer-date"
            type="date"
            value={transferDate}
            onChange={(e) => setTransferDate(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsTransferModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isTransferring}>
              Execute Transfer
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

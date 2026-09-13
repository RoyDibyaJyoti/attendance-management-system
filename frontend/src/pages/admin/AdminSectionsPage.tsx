import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { SectionResponse, DepartmentResponse, AcademicPeriodResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Award } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminSectionsPage: React.FC = () => {
  const { showToast } = useToast();
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [academicPeriodId, setAcademicPeriodId] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadData = async () => {
    try {
      const [secs, depts, pers] = await Promise.all([
        academicApi.getAllSections(),
        academicApi.getDepartments(),
        academicApi.getPeriods(),
      ]);
      setSections(secs);
      setDepartments(depts);
      setPeriods(pers);
      if (depts.length > 0) setDepartmentId(depts[0].id);
      if (pers.length > 0) setAcademicPeriodId(pers[0].id);
    } catch (err) {
      console.error('Failed to load sections data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !departmentId || !academicPeriodId) return;

    setIsSaving(true);
    try {
      await academicApi.createSection({
        name: name.trim(),
        departmentId,
        academicPeriodId,
      });
      showToast('Section created successfully.', 'success');
      setIsModalOpen(false);
      setName('');
      loadData();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to create section.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading class sections..." />;
  }

  const deptMap = new Map(departments.map((d) => [d.id, d]));
  const periodMap = new Map(periods.map((p) => [p.id, p]));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Class Sections</h1>
          <p className="text-sm text-slate-500 mt-0.5">Cohort groupings for students, timetables, and roll-calls</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Add Section
        </Button>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {sections.map((section) => {
            const dept = deptMap.get(section.departmentId);
            const period = periodMap.get(section.academicPeriodId);

            return (
              <div key={section.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
                <div className="flex items-center gap-3.5">
                  <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                    <Award className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-base font-bold text-slate-900">{section.name}</h4>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Department: <span className="font-semibold text-slate-700">{dept?.name || 'CSE'}</span> |{' '}
                      Semester: <span className="font-semibold text-slate-700">{period?.name || 'Fall 2026'}</span>
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Class Section"
        subtitle="Associate section with a department and academic period"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Section Name"
            id="section-name"
            placeholder="e.g. CSE-A, CSE-B"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Select
            label="Department"
            id="section-dept"
            options={departments.map((d) => ({ value: d.id, label: `${d.code} — ${d.name}` }))}
            value={departmentId}
            onChange={(e) => setDepartmentId(e.target.value)}
            required
          />

          <Select
            label="Academic Period / Semester"
            id="section-period"
            options={periods.map((p) => ({ value: p.id, label: p.name }))}
            value={academicPeriodId}
            onChange={(e) => setAcademicPeriodId(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Save Section
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { SubjectResponse, DepartmentResponse, CourseType } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, BookOpen } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminSubjectsPage: React.FC = () => {
  const { showToast } = useToast();
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [courseType, setCourseType] = useState<CourseType>('THEORY');
  const [creditHours, setCreditHours] = useState(3);
  const [departmentId, setDepartmentId] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadSubjects = async (p: number = 0) => {
    try {
      const [res, depts] = await Promise.all([
        academicApi.getSubjects(p, 10),
        academicApi.getDepartments(),
      ]);
      setSubjects(res.content);
      setPage(res.page);
      setTotalPages(res.totalPages);
      setDepartments(depts);
      if (depts.length > 0) setDepartmentId(depts[0].id);
    } catch (err) {
      console.error('Failed to load subjects:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadSubjects(0);
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim() || !name.trim() || !departmentId) return;

    setIsSaving(true);
    try {
      await academicApi.createSubject({
        code: code.trim().toUpperCase(),
        name: name.trim(),
        courseType,
        creditHours,
        departmentId,
      });
      showToast('Subject created successfully.', 'success');
      setIsModalOpen(false);
      setCode('');
      setName('');
      loadSubjects(page);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to create subject.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading curriculum subjects..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Curriculum Subjects</h1>
          <p className="text-sm text-slate-500 mt-0.5">Theory, Laboratory, and Integrated courses registered in syllabus</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Add Subject
        </Button>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {subjects.map((sub) => (
            <div key={sub.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                  <BookOpen className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                      {sub.code}
                    </span>
                    <h4 className="text-base font-bold text-slate-900">{sub.name}</h4>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Stream: <span className="font-semibold text-slate-700">{sub.courseType}</span> |{' '}
                    Credits: <span className="font-semibold text-slate-700">{sub.creditHours} hrs</span>
                  </p>
                </div>
              </div>

              <Badge
                variant={sub.courseType === 'THEORY' ? 'neutral' : 'purple'}
                size="sm"
              >
                {sub.courseType}
              </Badge>
            </div>
          ))}
        </div>

        {/* Pagination controls */}
        {totalPages > 1 && (
          <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => loadSubjects(page - 1)}
            >
              Previous
            </Button>
            <span className="text-xs text-slate-500">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => loadSubjects(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Register Curriculum Subject"
        subtitle="Specify subject code, title, and course type"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Subject Code"
            id="subject-code"
            placeholder="e.g. CS301, CS302L"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
          />

          <Input
            label="Course Title"
            id="subject-title"
            placeholder="e.g. Operating Systems"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Select
            label="Course Type"
            id="subject-type"
            options={[
              { value: 'THEORY', label: 'THEORY (Standard lecture course)' },
              { value: 'LABORATORY', label: 'LABORATORY (Laboratory practicals)' },
              { value: 'THEORY_INTEGRATED_LABORATORY', label: 'INTEGRATED (Dual stream)' },
            ]}
            value={courseType}
            onChange={(e) => setCourseType(e.target.value as CourseType)}
            required
          />

          <Input
            label="Credit Hours"
            id="subject-credits"
            type="number"
            min={1}
            max={10}
            value={creditHours}
            onChange={(e) => setCreditHours(parseInt(e.target.value, 10))}
            required
          />

          <Select
            label="Department"
            id="subject-dept"
            options={departments.map((d) => ({ value: d.id, label: `${d.code} — ${d.name}` }))}
            value={departmentId}
            onChange={(e) => setDepartmentId(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Register Subject
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

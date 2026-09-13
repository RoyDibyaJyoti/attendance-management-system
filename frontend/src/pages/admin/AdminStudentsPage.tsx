import React, { useState, useEffect } from 'react';
import { studentApi } from '../../api/studentApi';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { StudentResponse } from '../../types/student';
import { DepartmentResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, GraduationCap, Mail, UserCheck } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminStudentsPage: React.FC = () => {
  const { showToast } = useToast();
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [registrationNumber, setRegistrationNumber] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadStudents = async (p: number = 0) => {
    try {
      const [res, depts] = await Promise.all([
        studentApi.getStudents(p, 10),
        academicApi.getDepartments(),
      ]);
      setStudents(res.content);
      setPage(res.page);
      setTotalPages(res.totalPages);
      setDepartments(depts);
      if (depts.length > 0) setDepartmentId(depts[0].id);
    } catch (err) {
      console.error('Failed to load students:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStudents(0);
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!registrationNumber.trim() || !name.trim() || !email.trim() || !departmentId) return;

    setIsSaving(true);
    try {
      await studentApi.createStudent({
        registrationNumber: registrationNumber.trim(),
        name: name.trim(),
        email: email.trim(),
        departmentId,
      });
      showToast('Student registered successfully.', 'success');
      setIsModalOpen(false);
      setRegistrationNumber('');
      setName('');
      setEmail('');
      loadStudents(page);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to register student.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading student directory..." />;
  }

  const deptMap = new Map(departments.map((d) => [d.id, d]));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Student Directory</h1>
          <p className="text-sm text-slate-500 mt-0.5">Enrolled learners, registration numbers, and institutional contact information</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Register Student
        </Button>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {students.map((student) => {
            const dept = deptMap.get(student.departmentId);

            return (
              <div key={student.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
                <div className="flex items-center gap-3.5">
                  <div className="w-10 h-10 rounded-full bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center shrink-0">
                    {student.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-base font-bold text-slate-900">{student.name}</h4>
                      <span className="font-mono text-xs font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                        {student.registrationNumber}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-slate-500 mt-0.5">
                      <span className="flex items-center gap-1">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        {student.email}
                      </span>
                      <span>Department: <strong className="text-slate-700">{dept?.name || 'CSE'}</strong></span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => loadStudents(page - 1)}
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
              onClick={() => loadStudents(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Register Student Record"
        subtitle="Create student profile in institutional registry"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Registration Number"
            id="reg-number"
            placeholder="e.g. REG2026001, REG2026002"
            value={registrationNumber}
            onChange={(e) => setRegistrationNumber(e.target.value)}
            required
          />

          <Input
            label="Full Legal Name"
            id="student-name"
            placeholder="e.g. Alice Smith"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Institutional Email"
            id="student-email"
            type="email"
            placeholder="e.g. alice@univ.edu"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <Select
            label="Department"
            id="student-dept"
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
              Register Student
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

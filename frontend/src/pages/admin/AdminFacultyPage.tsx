import React, { useState, useEffect } from 'react';
import { facultyApi } from '../../api/facultyApi';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { FacultyResponse } from '../../types/faculty';
import { DepartmentResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Users, Mail, UserCheck } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminFacultyPage: React.FC = () => {
  const { showToast } = useToast();
  const [facultyList, setFacultyList] = useState<FacultyResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [includeInactive, setIncludeInactive] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [employeeId, setEmployeeId] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadFaculty = async (p: number = 0) => {
    try {
      const [res, depts] = await Promise.all([
        facultyApi.getFaculty(p, 10, includeInactive),
        academicApi.getDepartments(),
      ]);
      setFacultyList(res.content);
      setPage(res.page);
      setTotalPages(res.totalPages);
      setDepartments(depts);
      if (depts.length > 0) setDepartmentId(depts[0].id);
    } catch (err) {
      console.error('Failed to load faculty:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFaculty(0);
  }, [includeInactive]);

  const toggleStatus = async (id: string, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await facultyApi.deactivateFaculty(id);
        showToast('Faculty member deactivated', 'success');
      } else {
        await facultyApi.reactivateFaculty(id);
        showToast('Faculty member reactivated', 'success');
      }
      loadFaculty(page);
    } catch (err) {
      showToast('Failed to change status', 'error');
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!employeeId.trim() || !name.trim() || !email.trim() || !departmentId) return;

    setIsSaving(true);
    try {
      await facultyApi.createFaculty({
        employeeId: employeeId.trim(),
        name: name.trim(),
        email: email.trim(),
        departmentId,
      });
      showToast('Faculty onboarded successfully.', 'success');
      setIsModalOpen(false);
      setEmployeeId('');
      setName('');
      setEmail('');
      loadFaculty(page);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to onboard faculty member.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading faculty directory..." />;
  }

  const deptMap = new Map(departments.map((d) => [d.id, d]));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Faculty Directory</h1>
          <p className="text-sm text-slate-500 mt-0.5">Teaching personnel, employee identifiers, and departmental affiliations</p>
        </div>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
            <input 
              type="checkbox" 
              className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-600"
              checked={includeInactive}
              onChange={(e) => setIncludeInactive(e.target.checked)}
            />
            Include Inactive
          </label>
          <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            Onboard Faculty
          </Button>
        </div>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {facultyList.map((fac) => {
            const dept = deptMap.get(fac.departmentId);

            return (
              <div key={fac.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
                <div className="flex items-center gap-3.5">
                  <div className={`w-10 h-10 rounded-full font-bold text-xs flex items-center justify-center shrink-0 ${fac.isActive === false ? 'bg-slate-100 text-slate-400' : 'bg-slate-100 text-slate-700'}`}>
                    {fac.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className={`text-base font-bold ${fac.isActive === false ? 'text-slate-500' : 'text-slate-900'}`}>{fac.name}</h4>
                      {fac.isActive === false && (
                        <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 uppercase tracking-wider">Inactive</span>
                      )}
                      <span className={`font-mono text-xs font-semibold px-2 py-0.5 rounded ${fac.isActive === false ? 'text-slate-400 bg-slate-50' : 'text-slate-600 bg-slate-100'}`}>
                        {fac.employeeId}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-slate-500 mt-0.5">
                      <span className="flex items-center gap-1">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        {fac.email}
                      </span>
                      <span>Department: <strong className="text-slate-700">{dept?.name || 'CSE'}</strong></span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Button 
                    variant="secondary" 
                    size="sm"
                    onClick={() => toggleStatus(fac.id, fac.isActive !== false)}
                  >
                    {fac.isActive === false ? 'Reactivate' : 'Deactivate'}
                  </Button>
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
              onClick={() => loadFaculty(page - 1)}
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
              onClick={() => loadFaculty(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Onboard Faculty Member"
        subtitle="Register new teaching staff in institutional records"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Employee Identifier"
            id="employee-id"
            placeholder="e.g. FAC001, FAC002"
            value={employeeId}
            onChange={(e) => setEmployeeId(e.target.value)}
            required
          />

          <Input
            label="Full Name"
            id="faculty-name"
            placeholder="e.g. Dr. Alan Turing"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Official Email Address"
            id="faculty-email"
            type="email"
            placeholder="e.g. turing@univ.edu"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <Select
            label="Department"
            id="faculty-dept"
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
              Onboard Member
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

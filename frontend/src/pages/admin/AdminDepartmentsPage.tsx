import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { DepartmentResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Layers } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminDepartmentsPage: React.FC = () => {
  const { showToast } = useToast();
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [includeInactive, setIncludeInactive] = useState(false);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadDepartments = async () => {
    try {
      const data = await academicApi.getDepartments(includeInactive);
      setDepartments(data);
    } catch (err) {
      console.error('Failed to load departments:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDepartments();
  }, [includeInactive]);

  const toggleStatus = async (id: string, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await academicApi.deactivateDepartment(id);
        showToast('Department deactivated', 'success');
      } else {
        await academicApi.reactivateDepartment(id);
        showToast('Department reactivate', 'success');
      }
      loadDepartments();
    } catch (err) {
      showToast('Failed to change status', 'error');
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim() || !name.trim()) return;

    setIsSaving(true);
    try {
      await academicApi.createDepartment({ code: code.trim().toUpperCase(), name: name.trim() });
      showToast('Department created successfully.', 'success');
      setIsModalOpen(false);
      setCode('');
      setName('');
      loadDepartments();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to create department.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading institutional departments..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Academic Departments</h1>
          <p className="text-sm text-slate-500 mt-0.5">Manage institutional faculties and departmental units</p>
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
            Add Department
          </Button>
        </div>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {departments.map((dept) => (
            <div key={dept.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-xl font-bold text-xs flex items-center justify-center ${dept.isActive === false ? 'bg-slate-100 text-slate-400' : 'bg-indigo-50 text-indigo-600'}`}>
                  <Layers className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className={`text-sm font-bold ${dept.isActive === false ? 'text-slate-500' : 'text-slate-900'}`}>{dept.name}</h4>
                    {dept.isActive === false && (
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 uppercase tracking-wider">Inactive</span>
                    )}
                  </div>
                  <span className={`text-xs font-mono font-semibold px-2 py-0.5 rounded ${dept.isActive === false ? 'text-slate-400 bg-slate-50' : 'text-indigo-600 bg-indigo-50'}`}>
                    {dept.code}
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button 
                  variant="secondary" 
                  size="sm"
                  onClick={() => toggleStatus(dept.id, dept.isActive !== false)}
                >
                  {dept.isActive === false ? 'Reactivate' : 'Deactivate'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Academic Department"
        subtitle="Register a new academic department in the university registry"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Department Code"
            id="dept-code"
            placeholder="e.g. CSE, ECE, MECH"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
          />
          <Input
            label="Department Name"
            id="dept-name"
            placeholder="e.g. Department of Computer Science & Engineering"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Save Department
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

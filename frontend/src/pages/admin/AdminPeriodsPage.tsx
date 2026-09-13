import React, { useState, useEffect } from 'react';
import { academicApi } from '../../api/academicApi';
import { useToast } from '../../context/ToastContext';
import { AcademicPeriodResponse } from '../../types/academic';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Calendar } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminPeriodsPage: React.FC = () => {
  const { showToast } = useToast();
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const loadPeriods = async () => {
    try {
      const data = await academicApi.getPeriods();
      setPeriods(data);
    } catch (err) {
      console.error('Failed to load periods:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !startDate || !endDate) return;

    if (new Date(endDate) < new Date(startDate)) {
      showToast('End date must be after start date.', 'warning');
      return;
    }

    setIsSaving(true);
    try {
      await academicApi.createPeriod({ name: name.trim(), startDate, endDate });
      showToast('Academic period created successfully.', 'success');
      setIsModalOpen(false);
      setName('');
      setStartDate('');
      setEndDate('');
      loadPeriods();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to create academic period.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading academic semesters..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Academic Periods & Semesters</h1>
          <p className="text-sm text-slate-500 mt-0.5">Define bounded date ranges for enrollment windows and attendance calculations</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Add Semester / Period
        </Button>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {periods.map((period) => (
            <div key={period.id} className="p-5 flex items-center justify-between hover:bg-slate-50 transition-colors">
              <div className="flex items-center gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                  <Calendar className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-base font-bold text-slate-900">{period.name}</h4>
                    <Badge variant="success" size="sm" dot>
                      ACTIVE
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Window: <span className="font-medium text-slate-700">{period.startDate}</span> through{' '}
                    <span className="font-medium text-slate-700">{period.endDate}</span>
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Academic Period"
        subtitle="Specify inclusive start and end dates for the semester"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Period / Semester Name"
            id="period-name"
            placeholder="e.g. Fall 2026, Spring 2027"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <Input
            label="Start Date (Inclusive)"
            id="start-date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
          <Input
            label="End Date (Inclusive)"
            id="end-date"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
          />
          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Save Period
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { policyApi } from '../../api/policyApi';
import { useToast } from '../../context/ToastContext';
import { AttendancePolicyResponse } from '../../types/policy';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Settings2, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminPoliciesPage: React.FC = () => {
  const { showToast } = useToast();
  const [policies, setPolicies] = useState<AttendancePolicyResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('Standard University Policy');
  const [threshold, setThreshold] = useState(75.0);
  const [isSaving, setIsSaving] = useState(false);

  const loadPolicies = async () => {
    try {
      const data = await policyApi.getPolicyVersions('Standard University Policy');
      setPolicies(data);
    } catch (err) {
      console.error('Failed to load policies:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPolicies();
  }, []);

  const handleCreateVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || threshold <= 0) return;

    setIsSaving(true);
    try {
      await policyApi.createPolicy({
        name: name.trim(),
        minimumThresholdPercentage: threshold,
        statusContributions: {
          PRESENT: 1.0,
          ABSENT: 0.0,
          DUTY_LEAVE: 1.0,
          MEDICAL_LEAVE: 1.0,
          ON_DUTY: 1.0,
        },
        missingRecordStrategy: 'TREAT_AS_ABSENT',
      });
      showToast('New policy version published successfully.', 'success');
      setIsModalOpen(false);
      loadPolicies();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to create policy version.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading attendance policies..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Attendance Policies</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Immutable, versioned attendance calculation thresholds and status contribution maps
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          Publish New Version
        </Button>
      </div>

      <div className="p-4 rounded-xl bg-indigo-50 border border-indigo-200 text-indigo-900 text-sm flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
        <div>
          <h4 className="font-semibold text-indigo-950">Immutability Guaranteed</h4>
          <p className="text-xs text-indigo-700 mt-0.5">
            Attendance policies are never updated in place. Every modification spawns a new version, ensuring past calculation results remain 100% reproducible for compliance audits.
          </p>
        </div>
      </div>

      <Card title="Policy Version History" subtitle="Standard University Policy revisions">
        <div className="divide-y divide-slate-100">
          {policies.map((p) => (
            <div key={p.id} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50 transition-colors">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                  <Settings2 className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-base font-bold text-slate-900">{p.name}</h4>
                    <span className="font-mono text-xs font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                      v{p.version}
                    </span>
                    <Badge variant={p.active ? 'success' : 'neutral'} size="sm" dot={p.active}>
                      {p.active ? 'ACTIVE' : 'HISTORICAL'}
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Threshold: <strong className="text-slate-800">{p.minimumThresholdPercentage}%</strong> | Missing Record Strategy: <span className="font-mono">{p.missingRecordStrategy}</span>
                  </p>
                </div>
              </div>

              <div className="text-xs text-slate-500 bg-slate-50 p-2.5 rounded-lg border border-slate-200/80">
                <span className="font-semibold text-slate-700 block mb-0.5">Contributions:</span>
                <span>P: 1.0, A: 0.0, DL: 1.0, ML: 1.0, OD: 1.0</span>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Publish Attendance Policy Version"
        subtitle="Creates an immutable new version incremented from current policy"
      >
        <form onSubmit={handleCreateVersion} className="space-y-4">
          <Input
            label="Policy Name"
            id="pol-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Minimum Threshold Percentage"
            id="pol-threshold"
            type="number"
            step="0.01"
            min="50"
            max="100"
            value={threshold}
            onChange={(e) => setThreshold(parseFloat(e.target.value))}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Publish Version
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { studentApi } from '../../api/studentApi';
import { authApi } from '../../api/authApi';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { StudentResponse } from '../../types/student';
import { User, KeyRound, ShieldCheck, Mail } from 'lucide-react';
import { ApiError } from '../../api/client';

export const StudentProfilePage: React.FC = () => {
  const { user } = useAuth();
  const studentId = user?.studentId;
  const { showToast } = useToast();

  const [student, setStudent] = useState<StudentResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Email update state
  const [email, setEmail] = useState('');
  const [isUpdatingEmail, setIsUpdatingEmail] = useState(false);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  useEffect(() => {
    async function loadStudent() {
      if (!studentId) return;
      try {
        const data = await studentApi.getStudentById(studentId);
        setStudent(data);
        setEmail(data.email);
      } catch (err) {
        console.error('Failed to load student details:', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadStudent();
  }, [studentId]);

  const handleUpdateEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!studentId || !email.trim()) return;

    setIsUpdatingEmail(true);
    try {
      const updated = await studentApi.updateStudent(studentId, { email: email.trim() });
      setStudent(updated);
      showToast('Contact email updated successfully.', 'success');
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to update email address.', 'error');
      }
    } finally {
      setIsUpdatingEmail(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword) return;

    if (newPassword !== confirmPassword) {
      showToast('New passwords do not match.', 'error');
      return;
    }

    if (newPassword.length < 8) {
      showToast('Password must be at least 8 characters long.', 'warning');
      return;
    }

    setIsChangingPassword(true);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      showToast('Password successfully changed.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to change password. Please check your current password.', 'error');
      }
    } finally {
      setIsChangingPassword(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading student profile..." />;
  }

  return (
    <div className="max-w-4xl space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Student Profile & Settings</h1>
        <p className="text-sm text-slate-500 mt-1">Manage your identity credentials and contact information</p>
      </div>

      {/* Profile Overview Card */}
      <Card title="Institutional Record" subtitle="Verified university learner profile">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider block">
              Full Legal Name
            </span>
            <span className="text-base font-semibold text-slate-800 mt-0.5 block">{student?.name}</span>
          </div>

          <div>
            <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider block">
              Registration Number
            </span>
            <span className="text-base font-mono font-bold text-indigo-600 mt-0.5 block">
              {student?.registrationNumber}
            </span>
          </div>
        </div>

        {/* Update Contact Email */}
        <form onSubmit={handleUpdateEmail} className="mt-6 pt-6 border-t border-slate-100 space-y-4">
          <Input
            label="Contact Email Address"
            id="student-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            leftIcon={<Mail className="w-4 h-4" />}
            required
          />
          <div className="flex justify-end">
            <Button type="submit" size="sm" isLoading={isUpdatingEmail}>
              Save Contact Email
            </Button>
          </div>
        </form>
      </Card>

      {/* Password Change Card */}
      <Card title="Security Credentials" subtitle="Update your AMCS login password">
        <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
          <Input
            label="Current Password"
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            leftIcon={<KeyRound className="w-4 h-4" />}
            required
          />

          <Input
            label="New Password"
            id="new-password"
            type="password"
            placeholder="Minimum 8 characters"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            leftIcon={<ShieldCheck className="w-4 h-4" />}
            required
          />

          <Input
            label="Confirm New Password"
            id="confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            leftIcon={<ShieldCheck className="w-4 h-4" />}
            required
          />

          <div className="pt-2">
            <Button type="submit" size="sm" isLoading={isChangingPassword}>
              Update Password
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};

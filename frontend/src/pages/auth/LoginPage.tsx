import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { CheckCircle2, Lock, User, KeyRound, ArrowRight, ShieldCheck, GraduationCap, Users } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { ApiError } from '../../api/client';

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // If already authenticated, redirect to appropriate role portal
  if (isAuthenticated && user) {
    if (user.role === 'HOD_ADMIN') return <Navigate to="/admin/dashboard" replace />;
    if (user.role === 'FACULTY') return <Navigate to="/faculty/dashboard" replace />;
    if (user.role === 'STUDENT') return <Navigate to="/student/dashboard" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!usernameOrEmail.trim() || !password) {
      setErrorMessage('Please enter both username/email and password.');
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      await login({ usernameOrEmail: usernameOrEmail.trim(), password });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to sign in. Please verify your credentials and network connection.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickLogin = async (u: string, p: string) => {
    setUsernameOrEmail(u);
    setPassword(p);
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await login({ usernameOrEmail: u, password: p });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to sign in. Make sure the AMCS backend is running.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white mx-auto shadow-md shadow-indigo-200">
          <CheckCircle2 className="w-7 h-7" />
        </div>
        <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900">
          Attendance Management System
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Sign in to access your institutional portal
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-sm border border-slate-200/90 rounded-2xl sm:px-10">
          {errorMessage && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm font-medium flex items-start gap-2.5">
              <Lock className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Username or Email"
              id="username-or-email"
              type="text"
              placeholder="e.g. admin, faculty1, student1"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              leftIcon={<User className="w-4 h-4" />}
              required
              autoFocus
            />

            <Input
              label="Password"
              id="password"
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<KeyRound className="w-4 h-4" />}
              required
            />

            <div className="pt-2">
              <Button
                type="submit"
                className="w-full"
                size="lg"
                isLoading={isLoading}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Sign In
              </Button>
            </div>
          </form>

          {/* Quick Demo Logins Section */}
          <div className="mt-6 pt-6 border-t border-slate-100">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 text-center mb-3">
              One-Click Demo Personas
            </p>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('admin', 'AdminPassword123!')}
                className="p-2.5 rounded-xl border border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50 transition-colors text-center group"
              >
                <ShieldCheck className="w-5 h-5 text-indigo-600 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="block text-xs font-semibold text-slate-800">Admin</span>
                <span className="block text-[10px] text-slate-400">HOD_ADMIN</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('faculty1', 'FacultyPassword123!')}
                className="p-2.5 rounded-xl border border-slate-200 hover:border-emerald-300 hover:bg-emerald-50/50 transition-colors text-center group"
              >
                <Users className="w-5 h-5 text-emerald-600 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="block text-xs font-semibold text-slate-800">Faculty</span>
                <span className="block text-[10px] text-slate-400">Dr. Turing</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('student1', 'StudentPassword123!')}
                className="p-2.5 rounded-xl border border-slate-200 hover:border-blue-300 hover:bg-blue-50/50 transition-colors text-center group"
              >
                <GraduationCap className="w-5 h-5 text-blue-600 mx-auto mb-1 group-hover:scale-110 transition-transform" />
                <span className="block text-xs font-semibold text-slate-800">Student</span>
                <span className="block text-[10px] text-slate-400">Alice</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

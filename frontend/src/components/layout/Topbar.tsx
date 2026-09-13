import React from 'react';
import { LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Topbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200/80 px-8 flex items-center justify-between sticky top-0 z-30 shadow-2xs">
      {/* Title / Institutional Status */}
      <div className="flex items-center gap-3">
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/70">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-pulse" />
          Fall 2026 Academic Term
        </span>
        <span className="text-slate-300">|</span>
        <span className="text-xs text-slate-500 font-medium hidden sm:inline">
          Attendance Engine v0.1.0-prod
        </span>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-4">
        {/* User Pill */}
        <div className="flex items-center gap-2 text-xs font-medium text-slate-700 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/80">
          <UserIcon className="w-3.5 h-3.5 text-slate-400" />
          <span>{user?.email}</span>
        </div>

        {/* Logout Button */}
        <button
          onClick={logout}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-rose-600 hover:text-rose-700 hover:bg-rose-50 rounded-lg transition-colors border border-transparent hover:border-rose-100"
          title="Sign out of AMCS"
        >
          <LogOut className="w-3.5 h-3.5" />
          <span>Sign Out</span>
        </button>
      </div>
    </header>
  );
};

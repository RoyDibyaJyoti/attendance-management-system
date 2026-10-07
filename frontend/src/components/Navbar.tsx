import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Calendar,
  ClipboardList,
  BarChart3,
  FileSpreadsheet,
  Settings,
  LogOut,
  User as UserIcon,
  ShieldCheck,
  GraduationCap,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (!user) return null;

  const isActive = (path: string) => {
    if (path === '/' && location.pathname === '/') return true;
    if (path !== '/' && location.pathname.startsWith(path)) return true;
    return false;
  };

  const navItemClass = (path: string) =>
    `inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
      isActive(path)
        ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-200'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
    }`;

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo & Main Nav */}
          <div className="flex items-center gap-8">
            <Link to="/" className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-indigo-500 flex items-center justify-center text-white shadow-md shadow-indigo-100 font-bold text-lg">
                A
              </div>
              <div>
                <span className="font-bold text-lg text-slate-900 tracking-tight">AMCS</span>
                <span className="ml-1 text-xs font-semibold px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700">v2</span>
              </div>
            </Link>

            <nav className="hidden md:flex items-center gap-1.5">
              {user.role === 'FACULTY' ? (
                <>
                  <Link to="/" className={navItemClass('/')}>
                    <Calendar className="w-4 h-4" />
                    Today's Classes
                  </Link>
                  <Link to="/timetable" className={navItemClass('/timetable')}>
                    <GraduationCap className="w-4 h-4" />
                    My Timetable
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/" className={navItemClass('/')}>
                    <Settings className="w-4 h-4" />
                    Academic Setup
                  </Link>
                  <Link to="/timetable" className={navItemClass('/timetable')}>
                    <Calendar className="w-4 h-4" />
                    Timetable
                  </Link>
                </>
              )}

              <Link to="/register" className={navItemClass('/register')}>
                <ClipboardList className="w-4 h-4" />
                Attendance Register
              </Link>

              <Link to="/reports/monthly" className={navItemClass('/reports/monthly')}>
                <FileSpreadsheet className="w-4 h-4" />
                Monthly Summary
              </Link>

              <Link to="/reports/subject-wise" className={navItemClass('/reports/subject-wise')}>
                <BarChart3 className="w-4 h-4" />
                Subject-wise
              </Link>

              <Link to="/reports/overall" className={navItemClass('/reports/overall')}>
                <ShieldCheck className="w-4 h-4" />
                Overall
              </Link>
            </nav>
          </div>

          {/* User profile & Actions */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2.5 pl-3 border-l border-slate-200">
              <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-medium text-xs">
                {user.fullName.charAt(0)}
              </div>
              <div className="hidden sm:block text-left">
                <div className="text-xs font-semibold text-slate-800 leading-tight">
                  {user.fullName}
                </div>
                <div className="text-[11px] text-slate-500 flex items-center gap-1">
                  <span
                    className={`inline-block w-1.5 h-1.5 rounded-full ${
                      user.role === 'ADMIN' ? 'bg-purple-500' : 'bg-emerald-500'
                    }`}
                  />
                  {user.role} ({user.username})
                </div>
              </div>
            </div>

            <button
              onClick={logout}
              title="Sign Out"
              className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

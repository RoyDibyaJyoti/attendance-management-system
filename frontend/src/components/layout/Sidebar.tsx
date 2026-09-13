import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Calendar,
  FileSpreadsheet,
  FileUp,
  GraduationCap,
  Users,
  Layers,
  BookOpen,
  Settings2,
  Clock,
  ShieldCheck,
  Award,
  CheckCircle2,
  UserCheck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user, isStudent, isFaculty, isAdmin } = useAuth();

  const navItemClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
      isActive
        ? 'bg-indigo-600 text-white shadow-xs'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80'
    }`;

  return (
    <aside className="w-64 bg-white border-r border-slate-200/80 flex flex-col shrink-0 h-screen sticky top-0">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-100">
        <div className="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-sm shadow-indigo-200">
          <CheckCircle2 className="w-5 h-5" />
        </div>
        <div>
          <span className="font-bold text-slate-900 text-base tracking-tight block leading-tight">AMCS</span>
          <span className="text-[11px] font-medium text-slate-400 block tracking-wider uppercase">Attendance System</span>
        </div>
      </div>

      {/* Navigation Links */}
      <div className="flex-1 overflow-y-auto px-4 py-5 space-y-6">
        {/* STUDENT NAVIGATION */}
        {isStudent && (
          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Student Portal</p>
            <nav className="space-y-1">
              <NavLink to="/student/dashboard" className={navItemClass}>
                <LayoutDashboard className="w-4 h-4 shrink-0" />
                <span>My Attendance</span>
              </NavLink>
              <NavLink to="/student/reports" className={navItemClass}>
                <FileSpreadsheet className="w-4 h-4 shrink-0" />
                <span>Download Report (RPT-001)</span>
              </NavLink>
              <NavLink to="/student/profile" className={navItemClass}>
                <UserCheck className="w-4 h-4 shrink-0" />
                <span>My Profile</span>
              </NavLink>
            </nav>
          </div>
        )}

        {/* FACULTY NAVIGATION */}
        {isFaculty && (
          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Faculty Console</p>
            <nav className="space-y-1">
              <NavLink to="/faculty/dashboard" className={navItemClass}>
                <LayoutDashboard className="w-4 h-4 shrink-0" />
                <span>Dashboard</span>
              </NavLink>
              <NavLink to="/faculty/timetable" className={navItemClass}>
                <Calendar className="w-4 h-4 shrink-0" />
                <span>Timetable & Sessions</span>
              </NavLink>
              <NavLink to="/faculty/corrections" className={navItemClass}>
                <Clock className="w-4 h-4 shrink-0" />
                <span>Attendance Corrections</span>
              </NavLink>
              <NavLink to="/faculty/reports" className={navItemClass}>
                <FileSpreadsheet className="w-4 h-4 shrink-0" />
                <span>Faculty Reports</span>
              </NavLink>
              <NavLink to="/faculty/imports" className={navItemClass}>
                <FileUp className="w-4 h-4 shrink-0" />
                <span>Excel Imports</span>
              </NavLink>
            </nav>
          </div>
        )}

        {/* HOD_ADMIN NAVIGATION */}
        {isAdmin && (
          <>
            <div>
              <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Overview</p>
              <nav className="space-y-1">
                <NavLink to="/admin/dashboard" className={navItemClass}>
                  <LayoutDashboard className="w-4 h-4 shrink-0" />
                  <span>Institutional Dashboard</span>
                </NavLink>
              </nav>
            </div>

            <div>
              <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Academic Structure</p>
              <nav className="space-y-1">
                <NavLink to="/admin/departments" className={navItemClass}>
                  <Layers className="w-4 h-4 shrink-0" />
                  <span>Departments</span>
                </NavLink>
                <NavLink to="/admin/periods" className={navItemClass}>
                  <Calendar className="w-4 h-4 shrink-0" />
                  <span>Semesters & Periods</span>
                </NavLink>
                <NavLink to="/admin/sections" className={navItemClass}>
                  <Award className="w-4 h-4 shrink-0" />
                  <span>Sections</span>
                </NavLink>
                <NavLink to="/admin/subjects" className={navItemClass}>
                  <BookOpen className="w-4 h-4 shrink-0" />
                  <span>Subjects</span>
                </NavLink>
                <NavLink to="/admin/policies" className={navItemClass}>
                  <Settings2 className="w-4 h-4 shrink-0" />
                  <span>Attendance Policies</span>
                </NavLink>
              </nav>
            </div>

            <div>
              <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">People & Timetables</p>
              <nav className="space-y-1">
                <NavLink to="/admin/faculty" className={navItemClass}>
                  <Users className="w-4 h-4 shrink-0" />
                  <span>Faculty Directory</span>
                </NavLink>
                <NavLink to="/admin/students" className={navItemClass}>
                  <GraduationCap className="w-4 h-4 shrink-0" />
                  <span>Students Directory</span>
                </NavLink>
                <NavLink to="/admin/enrollments" className={navItemClass}>
                  <UserCheck className="w-4 h-4 shrink-0" />
                  <span>Enrollments & Transfers</span>
                </NavLink>
                <NavLink to="/admin/sessions" className={navItemClass}>
                  <Clock className="w-4 h-4 shrink-0" />
                  <span>Master Timetable</span>
                </NavLink>
              </nav>
            </div>

            <div>
              <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Reports & Bulk Ops</p>
              <nav className="space-y-1">
                <NavLink to="/admin/reports" className={navItemClass}>
                  <FileSpreadsheet className="w-4 h-4 shrink-0" />
                  <span>Report Center (RPT-001–008)</span>
                </NavLink>
                <NavLink to="/admin/imports" className={navItemClass}>
                  <FileUp className="w-4 h-4 shrink-0" />
                  <span>Two-Phase Import Center</span>
                </NavLink>
              </nav>
            </div>
          </>
        )}
      </div>

      {/* User Footer Profile */}
      <div className="p-4 border-t border-slate-100 bg-slate-50/50">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-700 font-bold text-xs flex items-center justify-center">
            {user?.username?.substring(0, 2).toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-slate-900 truncate">{user?.username}</p>
            <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-sm bg-indigo-50 text-indigo-700 border border-indigo-100">
              {user?.role}
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
};

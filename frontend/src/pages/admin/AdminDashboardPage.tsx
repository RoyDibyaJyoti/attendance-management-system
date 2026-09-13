import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { academicApi } from '../../api/academicApi';
import { studentApi } from '../../api/studentApi';
import { facultyApi } from '../../api/facultyApi';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  Layers,
  Calendar,
  Award,
  BookOpen,
  GraduationCap,
  Users,
  FileSpreadsheet,
  FileUp,
  Plus,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';

export const AdminDashboardPage: React.FC = () => {
  const [isLoading, setIsLoading] = useState(true);

  const [departmentCount, setDepartmentCount] = useState(0);
  const [periodCount, setPeriodCount] = useState(0);
  const [sectionCount, setSectionCount] = useState(0);
  const [subjectCount, setSubjectCount] = useState(0);
  const [studentCount, setStudentCount] = useState(0);
  const [facultyCount, setFacultyCount] = useState(0);

  useEffect(() => {
    async function loadStats() {
      try {
        const [depts, pers, secs, subs, studs, facs] = await Promise.all([
          academicApi.getDepartments(),
          academicApi.getPeriods(),
          academicApi.getAllSections(),
          academicApi.getSubjects(0, 1),
          studentApi.getStudents(0, 1),
          facultyApi.getFaculty(0, 1),
        ]);

        setDepartmentCount(depts.length);
        setPeriodCount(pers.length);
        setSectionCount(secs.length);
        setSubjectCount(subs.totalElements);
        setStudentCount(studs.totalElements);
        setFacultyCount(facs.totalElements);
      } catch (err) {
        console.error('Failed to load admin stats:', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadStats();
  }, []);

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading institutional overview..." />;
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
        <div>
          <Badge variant="purple" className="bg-indigo-600/40 text-indigo-200 border-indigo-500/30 mb-2.5">
            Head of Department / Institutional Administrator
          </Badge>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
            AMCS Institutional Control Center
          </h1>
          <p className="text-slate-300 text-sm mt-1">
            Complete institutional control over academic structures, curricular subjects, faculty assignments, and reporting
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link to="/admin/reports">
            <Button
              className="bg-indigo-500 hover:bg-indigo-600 text-white"
              leftIcon={<FileSpreadsheet className="w-4 h-4" />}
            >
              Report Center
            </Button>
          </Link>
          <Link to="/admin/imports">
            <Button
              variant="outline"
              className="bg-white/10 hover:bg-white/20 text-white border-white/20"
              leftIcon={<FileUp className="w-4 h-4" />}
            >
              Bulk Imports
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <StatCard
          title="Departments"
          value={departmentCount}
          subtitle="Academic faculties"
          color="indigo"
          icon={<Layers className="w-6 h-6" />}
        />

        <StatCard
          title="Academic Periods"
          value={periodCount}
          subtitle="Configured semesters & terms"
          color="indigo"
          icon={<Calendar className="w-6 h-6" />}
        />

        <StatCard
          title="Class Sections"
          value={sectionCount}
          subtitle="Active student cohorts"
          color="emerald"
          icon={<Award className="w-6 h-6" />}
        />

        <StatCard
          title="Curriculum Subjects"
          value={subjectCount}
          subtitle="Theory and laboratory courses"
          color="slate"
          icon={<BookOpen className="w-6 h-6" />}
        />

        <StatCard
          title="Enrolled Students"
          value={studentCount}
          subtitle="Registered institutional learners"
          color="slate"
          icon={<GraduationCap className="w-6 h-6" />}
        />

        <StatCard
          title="Faculty Members"
          value={facultyCount}
          subtitle="Teaching staff"
          color="slate"
          icon={<Users className="w-6 h-6" />}
        />
      </div>

      {/* Quick Access Modules */}
      <div>
        <h2 className="text-lg font-bold text-slate-900 mb-4">Quick Management Portals</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <Card
            title="Academic Setup"
            subtitle="Departments, Semesters, Sections, and Courses"
            className="hover:border-slate-300 transition-colors"
          >
            <div className="space-y-2 mt-2">
              <Link to="/admin/departments" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Manage Departments</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/periods" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Manage Semesters & Periods</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/sections" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Manage Class Sections</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/subjects" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Curriculum Subjects</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
            </div>
          </Card>

          <Card
            title="People & Cohorts"
            subtitle="Faculty, Students, and Section Enrollments"
            className="hover:border-slate-300 transition-colors"
          >
            <div className="space-y-2 mt-2">
              <Link to="/admin/faculty" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Faculty Directory & Onboarding</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/students" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Student Directory & Registration</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/enrollments" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Section Enrollments & Transfers</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/sessions" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Master Timetable Management</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
            </div>
          </Card>

          <Card
            title="Reports & Bulk Pipelines"
            subtitle="Streaming XLSX Exports and Two-Phase Staging"
            className="hover:border-slate-300 transition-colors"
          >
            <div className="space-y-2 mt-2">
              <Link to="/admin/reports" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Institutional Report Center (RPT-001–008)</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/imports" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Two-Phase Excel Bulk Importer</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
              <Link to="/admin/policies" className="flex items-center justify-between text-sm py-1 text-slate-600 hover:text-indigo-600">
                <span>Attendance Threshold Policies</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
              </Link>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

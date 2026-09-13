import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ProtectedRoute } from './ProtectedRoute';
import { AppLayout } from '../components/layout/AppLayout';

// Auth Pages
import { LoginPage } from '../pages/auth/LoginPage';
import { UnauthorizedPage } from '../pages/common/UnauthorizedPage';
import { NotFoundPage } from '../pages/common/NotFoundPage';

// Student Pages
import { StudentDashboardPage } from '../pages/student/StudentDashboardPage';
import { StudentReportsPage } from '../pages/student/StudentReportsPage';
import { StudentProfilePage } from '../pages/student/StudentProfilePage';

// Faculty Pages
import { FacultyDashboardPage } from '../pages/faculty/FacultyDashboardPage';
import { FacultyTimetablePage } from '../pages/faculty/FacultyTimetablePage';
import { FacultyRollCallPage } from '../pages/faculty/FacultyRollCallPage';
import { FacultyCorrectionsPage } from '../pages/faculty/FacultyCorrectionsPage';
import { FacultyReportsPage } from '../pages/faculty/FacultyReportsPage';
import { FacultyImportsPage } from '../pages/faculty/FacultyImportsPage';

// Admin Pages
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage';
import { AdminDepartmentsPage } from '../pages/admin/AdminDepartmentsPage';
import { AdminPeriodsPage } from '../pages/admin/AdminPeriodsPage';
import { AdminSectionsPage } from '../pages/admin/AdminSectionsPage';
import { AdminSubjectsPage } from '../pages/admin/AdminSubjectsPage';
import { AdminPoliciesPage } from '../pages/admin/AdminPoliciesPage';
import { AdminFacultyPage } from '../pages/admin/AdminFacultyPage';
import { AdminStudentsPage } from '../pages/admin/AdminStudentsPage';
import { AdminEnrollmentsPage } from '../pages/admin/AdminEnrollmentsPage';
import { AdminSessionsPage } from '../pages/admin/AdminSessionsPage';
import { AdminReportCenterPage } from '../pages/admin/AdminReportCenterPage';
import { AdminImportCenterPage } from '../pages/admin/AdminImportCenterPage';
import { AdminFacultyAssignmentsPage } from '../pages/admin/AdminFacultyAssignmentsPage';

const RootRedirect: React.FC = () => {
  const { isAuthenticated, user, isLoading } = useAuth();

  if (isLoading) return null;
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;

  if (user.role === 'HOD_ADMIN') return <Navigate to="/admin/dashboard" replace />;
  if (user.role === 'FACULTY') return <Navigate to="/faculty/dashboard" replace />;
  if (user.role === 'STUDENT') return <Navigate to="/student/dashboard" replace />;

  return <Navigate to="/login" replace />;
};

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="/" element={<RootRedirect />} />

      {/* Authenticated Layout */}
      <Route element={<AppLayout />}>
        {/* STUDENT PORTAL */}
        <Route element={<ProtectedRoute allowedRoles={['STUDENT']} />}>
          <Route path="/student/dashboard" element={<StudentDashboardPage />} />
          <Route path="/student/reports" element={<StudentReportsPage />} />
          <Route path="/student/profile" element={<StudentProfilePage />} />
        </Route>

        {/* FACULTY PORTAL */}
        <Route element={<ProtectedRoute allowedRoles={['FACULTY', 'HOD_ADMIN']} />}>
          <Route path="/faculty/dashboard" element={<FacultyDashboardPage />} />
          <Route path="/faculty/timetable" element={<FacultyTimetablePage />} />
          <Route path="/faculty/sessions/:sessionId/attendance" element={<FacultyRollCallPage />} />
          <Route path="/faculty/corrections" element={<FacultyCorrectionsPage />} />
          <Route path="/faculty/reports" element={<FacultyReportsPage />} />
          <Route path="/faculty/imports" element={<FacultyImportsPage />} />
        </Route>

        {/* HOD_ADMIN PORTAL */}
        <Route element={<ProtectedRoute allowedRoles={['HOD_ADMIN']} />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/departments" element={<AdminDepartmentsPage />} />
          <Route path="/admin/periods" element={<AdminPeriodsPage />} />
          <Route path="/admin/sections" element={<AdminSectionsPage />} />
          <Route path="/admin/subjects" element={<AdminSubjectsPage />} />
          <Route path="/admin/policies" element={<AdminPoliciesPage />} />
          <Route path="/admin/faculty" element={<AdminFacultyPage />} />
          <Route path="/admin/faculty-assignments" element={<AdminFacultyAssignmentsPage />} />
          <Route path="/admin/students" element={<AdminStudentsPage />} />
          <Route path="/admin/enrollments" element={<AdminEnrollmentsPage />} />
          <Route path="/admin/sessions" element={<AdminSessionsPage />} />
          <Route path="/admin/reports" element={<AdminReportCenterPage />} />
          <Route path="/admin/imports" element={<AdminImportCenterPage />} />
        </Route>
      </Route>

      {/* Catch-all 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

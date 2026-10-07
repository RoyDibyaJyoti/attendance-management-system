import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { LoginView } from './views/LoginView';
import { FacultyDashboardView } from './views/FacultyDashboardView';
import { AdminAcademicSetupView } from './views/AdminAcademicSetupView';
import { TimetableView } from './views/TimetableView';
import { TakeAttendanceView } from './views/TakeAttendanceView';
import { RegisterView } from './views/RegisterView';
import { MonthlySummaryView } from './views/MonthlySummaryView';
import { SubjectWiseSummaryView } from './views/SubjectWiseSummaryView';
import { OverallSummaryView } from './views/OverallSummaryView';

const ProtectedLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-500">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <Navbar />
      <main className="flex-1">{children}</main>
    </div>
  );
};

const HomeRouter: React.FC = () => {
  const { user } = useAuth();
  if (user?.role === 'ADMIN') {
    return <AdminAcademicSetupView />;
  }
  return <FacultyDashboardView />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginView />} />

          <Route
            path="/"
            element={
              <ProtectedLayout>
                <HomeRouter />
              </ProtectedLayout>
            }
          />

          <Route
            path="/timetable"
            element={
              <ProtectedLayout>
                <TimetableView />
              </ProtectedLayout>
            }
          />

          <Route
            path="/sessions/:id/attendance"
            element={
              <ProtectedLayout>
                <TakeAttendanceView />
              </ProtectedLayout>
            }
          />

          <Route
            path="/register"
            element={
              <ProtectedLayout>
                <RegisterView />
              </ProtectedLayout>
            }
          />

          <Route
            path="/reports/monthly"
            element={
              <ProtectedLayout>
                <MonthlySummaryView />
              </ProtectedLayout>
            }
          />

          <Route
            path="/reports/subject-wise"
            element={
              <ProtectedLayout>
                <SubjectWiseSummaryView />
              </ProtectedLayout>
            }
          />

          <Route
            path="/reports/overall"
            element={
              <ProtectedLayout>
                <OverallSummaryView />
              </ProtectedLayout>
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;

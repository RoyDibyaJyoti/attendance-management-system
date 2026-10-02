import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { FacultyRollCallPage } from '../pages/faculty/FacultyRollCallPage';
import { ToastProvider } from '../context/ToastContext';
import { sessionApi } from '../api/sessionApi';
import { attendanceApi } from '../api/attendanceApi';
import { studentApi } from '../api/studentApi';
import { enrollmentApi } from '../api/enrollmentApi';
import { academicApi } from '../api/academicApi';

// Mock APIs
vi.mock('../api/sessionApi');
vi.mock('../api/attendanceApi');
vi.mock('../api/studentApi');
vi.mock('../api/enrollmentApi');
vi.mock('../api/academicApi');

describe('FacultyRollCallPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter initialEntries={['/faculty/roll-call/sess-123']}>
        <ToastProvider>
          <Routes>
            <Route path="/faculty/roll-call/:sessionId" element={<FacultyRollCallPage />} />
            <Route path="/faculty/timetable" element={<div>Timetable Page</div>} />
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    );
  };

  it('renders unique student entries in the roster even with duplicate enrollments', async () => {
    // Setup mocks
    vi.mocked(sessionApi.getSessionById).mockResolvedValue({
      id: 'sess-123',
      sectionId: 'sec-1',
      subjectId: 'sub-1',
      sessionDate: '2026-10-02',
      sessionType: 'THEORY',
      plannedUnits: 1,
      status: 'SCHEDULED',
    } as any);

    vi.mocked(academicApi.getAllSubjects).mockResolvedValue([
      { id: 'sub-1', name: 'Computer Science', code: 'CS101' } as any,
    ]);

    // Duplicate enrollment data
    vi.mocked(enrollmentApi.getSectionEnrollments).mockResolvedValue([
      { id: 'e1', studentId: 'student-A', sectionId: 'sec-1', status: 'ACTIVE' } as any,
      { id: 'e2', studentId: 'student-A', sectionId: 'sec-1', status: 'ACTIVE' } as any,
    ]);

    vi.mocked(studentApi.getAllStudents).mockResolvedValue([
      { id: 'student-A', name: 'Alice Smith', registrationNumber: 'REG001' } as any,
    ]);

    vi.mocked(attendanceApi.recordAttendanceBatch).mockResolvedValue({} as any);

    renderComponent();

    expect(screen.getByText(/Loading student roster/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    // Verify exactly one student entry is rendered despite two ACTIVE enrollments
    const presentButtons = screen.getAllByRole('button', { name: /PRESENT/i });
    // "Mark All Present" button + 1 student button
    expect(presentButtons).toHaveLength(2);

    // Submit the roll-call
    const submitBtn = screen.getByRole('button', { name: /Submit Roll-Call/i });
    expect(submitBtn).toHaveTextContent('1 Records'); // Validates roster length

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(attendanceApi.recordAttendanceBatch).toHaveBeenCalledWith('sess-123', {
        records: [{ studentId: 'student-A', status: 'PRESENT' }],
      });
    });
  });
});

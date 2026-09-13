import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { LoginPage } from '../pages/auth/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { clearAuthToken } from '../api/client';

describe('LoginPage Component', () => {
  beforeEach(() => {
    clearAuthToken();
    vi.restoreAllMocks();
  });

  it('renders sign in heading and persona quick login buttons', () => {
    render(
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    );

    expect(screen.getByText(/Attendance Management System/i)).toBeInTheDocument();
    expect(screen.getByText(/One-Click Demo Personas/i)).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
    expect(screen.getByText('Faculty')).toBeInTheDocument();
    expect(screen.getByText('Student')).toBeInTheDocument();
  });
});

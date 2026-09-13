import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { authApi } from '../api/authApi';
import { setAuthToken, clearAuthToken } from '../api/client';

const TestAuthConsumer: React.FC = () => {
  const { user, isAuthenticated, login, logout, isStudent, isFaculty, isAdmin } = useAuth();
  return (
    <div>
      <span data-testid="auth-status">{isAuthenticated ? 'AUTHENTICATED' : 'ANONYMOUS'}</span>
      <span data-testid="user-role">{user?.role || 'NONE'}</span>
      <span data-testid="is-student">{isStudent ? 'YES' : 'NO'}</span>
      <span data-testid="is-faculty">{isFaculty ? 'YES' : 'NO'}</span>
      <span data-testid="is-admin">{isAdmin ? 'YES' : 'NO'}</span>
      <button onClick={() => login({ usernameOrEmail: 'admin', password: 'password' })}>
        Log In
      </button>
      <button onClick={logout}>Log Out</button>
    </div>
  );
};

describe('AuthContext and Persona State', () => {
  beforeEach(() => {
    clearAuthToken();
    vi.restoreAllMocks();
  });

  it('starts unauthenticated when no token is in storage', async () => {
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('Unauthenticated'));

    render(
      <ToastProvider>
        <AuthProvider>
          <TestAuthConsumer />
        </AuthProvider>
      </ToastProvider>
    );

    expect(screen.getByTestId('auth-status').textContent).toBe('ANONYMOUS');
    expect(screen.getByTestId('user-role').textContent).toBe('NONE');
  });

  it('handles login and correctly derives HOD_ADMIN persona privileges', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'admin-jwt-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      userId: 'user-admin-123',
      username: 'admin',
      email: 'admin@univ.edu',
      role: 'HOD_ADMIN',
      studentId: null,
      facultyId: null,
    });

    render(
      <ToastProvider>
        <AuthProvider>
          <TestAuthConsumer />
        </AuthProvider>
      </ToastProvider>
    );

    const loginBtn = screen.getByText('Log In');
    await act(async () => {
      loginBtn.click();
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('AUTHENTICATED');
    expect(screen.getByTestId('user-role').textContent).toBe('HOD_ADMIN');
    expect(screen.getByTestId('is-admin').textContent).toBe('YES');
    expect(screen.getByTestId('is-faculty').textContent).toBe('NO');
    expect(screen.getByTestId('is-student').textContent).toBe('NO');
  });
});

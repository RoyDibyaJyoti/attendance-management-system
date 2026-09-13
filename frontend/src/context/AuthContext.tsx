import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { LoginRequest, UserProfileResponse, UserRole } from '../types/auth';
import { authApi } from '../api/authApi';
import { clearAuthToken, getAuthToken, setAuthToken } from '../api/client';
import { useToast } from './ToastContext';

interface AuthContextType {
  user: UserProfileResponse | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (req: LoginRequest) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
  isStudent: boolean;
  isFaculty: boolean;
  isAdmin: boolean;
  role: UserRole | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfileResponse | null>(null);
  const [token, setToken] = useState<string | null>(getAuthToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const { showToast } = useToast();

  const logout = useCallback(() => {
    clearAuthToken();
    setToken(null);
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    const currentToken = getAuthToken();
    if (!currentToken) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const profile = await authApi.getMe();
      setUser(profile);
    } catch (error) {
      console.warn('Failed to load user profile, clearing credentials', error);
      logout();
    } finally {
      setIsLoading(false);
    }
  }, [logout]);

  useEffect(() => {
    refreshProfile();

    const handleAuthExpired = () => {
      logout();
      showToast('Session expired. Please log in again.', 'warning');
    };

    window.addEventListener('amcs:auth_expired', handleAuthExpired);
    return () => {
      window.removeEventListener('amcs:auth_expired', handleAuthExpired);
    };
  }, [refreshProfile, logout, showToast]);

  const login = async (req: LoginRequest) => {
    setIsLoading(true);
    try {
      const res = await authApi.login(req);
      setAuthToken(res.token);
      setToken(res.token);

      const profile: UserProfileResponse = {
        userId: res.userId,
        username: res.username,
        email: res.email,
        role: res.role,
        studentId: res.studentId,
        facultyId: res.facultyId,
      };
      setUser(profile);
      showToast(`Welcome back, ${res.username}!`, 'success');
    } catch (err: unknown) {
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const isStudent = user?.role === 'STUDENT';
  const isFaculty = user?.role === 'FACULTY';
  const isAdmin = user?.role === 'HOD_ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!user && !!token,
        login,
        logout,
        refreshProfile,
        isStudent,
        isFaculty,
        isAdmin,
        role: user?.role || null,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

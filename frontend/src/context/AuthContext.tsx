import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiFetch, getAuthToken, setAuthToken, getCurrentUser, setCurrentUser } from '../api';

export interface AuthUser {
  username: string;
  role: 'ADMIN' | 'FACULTY';
  fullName: string;
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(getCurrentUser());
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }

    // Verify token with /me
    apiFetch<AuthUser>('/auth/me')
      .then((me) => {
        setUser(me);
        setCurrentUser(me);
      })
      .catch(() => {
        setAuthToken(null);
        setCurrentUser(null);
        setUser(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const login = async (username: string, password: string): Promise<AuthUser> => {
    // Clear any previous token before attempting login so no stale Authorization header is sent
    setAuthToken(null);
    setCurrentUser(null);

    const data = await apiFetch<{ token: string; username: string; role: 'ADMIN' | 'FACULTY'; fullName: string }>(
      '/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      }
    );

    setAuthToken(data.token);
    const loggedUser: AuthUser = {
      username: data.username,
      role: data.role,
      fullName: data.fullName,
    };
    setUser(loggedUser);
    setCurrentUser(loggedUser);
    return loggedUser;
  };

  const logout = () => {
    setAuthToken(null);
    setCurrentUser(null);
    setUser(null);
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

describe('AMCS Frontend App', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders login screen when not authenticated', () => {
    render(<App />);
    expect(screen.getByText(/AMCS Attendance System/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/admin or faculty1/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
    expect(screen.getByText(/Faculty 1/i)).toBeInTheDocument();
    expect(screen.getByText(/Administrator/i)).toBeInTheDocument();
  });

  it('renders quick demo login shortcuts for faculty and administrator', () => {
    render(<App />);
    expect(screen.getByText(/Quick Demo Access/i)).toBeInTheDocument();
    expect(screen.getByText(/Faculty 1/i)).toBeInTheDocument();
    expect(screen.getByText(/Faculty 2/i)).toBeInTheDocument();
    expect(screen.getByText(/Administrator/i)).toBeInTheDocument();
  });
});

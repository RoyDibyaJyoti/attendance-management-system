import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ErrorBoundary } from '../components/common/ErrorBoundary';

const FaultyComponent: React.FC = () => {
  throw new Error('Simulated render error');
};

describe('ErrorBoundary', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <div>Normal content</div>
      </ErrorBoundary>
    );

    expect(screen.getByText('Normal content')).toBeDefined();
  });

  it('renders fallback error card when a child component throws', () => {
    // Suppress console.error output during deliberate error test
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <FaultyComponent />
      </ErrorBoundary>
    );

    expect(screen.getByText('Something went wrong')).toBeDefined();
    expect(screen.getByText('Simulated render error')).toBeDefined();
    expect(screen.getByText('Reload page')).toBeDefined();
    expect(screen.getByText('Return home')).toBeDefined();

    consoleError.mockRestore();
  });
});

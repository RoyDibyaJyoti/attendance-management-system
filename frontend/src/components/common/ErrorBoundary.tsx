import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';
import { Button } from './Button';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('AMCS Uncaught UI Error:', error, errorInfo);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
          <div className="max-w-md w-full bg-white rounded-2xl shadow-xl border border-slate-200 p-8 text-center">
            <div className="w-14 h-14 bg-rose-100 text-rose-600 rounded-2xl flex items-center justify-center mx-auto mb-5 shadow-sm">
              <AlertTriangle className="w-7 h-7" />
            </div>
            <h1 className="text-xl font-bold text-slate-900 mb-2">Something went wrong</h1>
            <p className="text-sm text-slate-600 mb-6">
              An unexpected error occurred while rendering this view. Our team has been notified.
            </p>
            {this.state.error?.message && (
              <div className="bg-slate-100 rounded-lg p-3 text-xs text-slate-700 font-mono mb-6 text-left overflow-auto max-h-24">
                {this.state.error.message}
              </div>
            )}
            <div className="flex gap-3 justify-center">
              <Button variant="outline" onClick={this.handleReload} leftIcon={<RefreshCw className="w-4 h-4" />}>
                Reload page
              </Button>
              <Button variant="primary" onClick={this.handleGoHome} leftIcon={<Home className="w-4 h-4" />}>
                Return home
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

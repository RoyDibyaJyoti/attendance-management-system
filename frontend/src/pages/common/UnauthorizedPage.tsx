import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button } from '../../components/common/Button';

export const UnauthorizedPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 text-center">
      <div className="max-w-md w-full bg-white p-8 rounded-2xl border border-rose-200 shadow-sm">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-4">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-bold text-slate-900">Access Denied</h1>
        <p className="text-sm text-slate-500 mt-2 mb-6">
          Your current institutional role does not have permission to view or execute operations on this page.
        </p>
        <Link to="/">
          <Button variant="outline" leftIcon={<ArrowLeft className="w-4 h-4" />}>
            Return to Authorized Dashboard
          </Button>
        </Link>
      </div>
    </div>
  );
};

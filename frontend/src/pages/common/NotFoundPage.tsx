import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, FileQuestion } from 'lucide-react';
import { Button } from '../../components/common/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 text-center">
      <div className="max-w-md w-full bg-white p-8 rounded-2xl border border-slate-200 shadow-sm">
        <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-4">
          <FileQuestion className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-bold text-slate-900">Page Not Found</h1>
        <p className="text-sm text-slate-500 mt-2 mb-6">
          The requested route does not exist or you might not have access to this resource.
        </p>
        <Link to="/">
          <Button leftIcon={<ArrowLeft className="w-4 h-4" />}>Return to Dashboard</Button>
        </Link>
      </div>
    </div>
  );
};

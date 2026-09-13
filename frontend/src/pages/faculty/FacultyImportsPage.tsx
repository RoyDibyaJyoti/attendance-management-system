import React from 'react';
import { ImportWizard } from '../../components/importer/ImportWizard';

export const FacultyImportsPage: React.FC = () => {
  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Faculty Bulk Imports</h1>
        <p className="text-sm text-slate-500 mt-1">
          Stage and batch-commit class schedules and attendance spreadsheets via the two-phase pipeline
        </p>
      </div>

      <ImportWizard allowedTypes={['SESSIONS', 'ATTENDANCE_RECORDS']} />
    </div>
  );
};

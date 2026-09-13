import React from 'react';
import { ImportWizard } from '../../components/importer/ImportWizard';

export const AdminImportCenterPage: React.FC = () => {
  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Institutional Import Center</h1>
        <p className="text-sm text-slate-500 mt-1">
          High-throughput two-phase bulk spreadsheet ingestion for students, timetable sessions, and attendance
        </p>
      </div>

      <ImportWizard allowedTypes={['STUDENTS', 'SESSIONS', 'ATTENDANCE_RECORDS']} />
    </div>
  );
};

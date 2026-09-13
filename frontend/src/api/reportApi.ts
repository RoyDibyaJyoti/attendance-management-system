import { downloadFile } from './client';

export const reportApi = {
  // RPT-001: Student Attendance Report
  downloadRpt001: (params: { studentId: string; academicPeriodId: string; sectionId?: string }): Promise<void> => {
    return downloadFile('/reports/rpt-001', `rpt-001-student-${params.studentId}.xlsx`, params);
  },

  // RPT-002: Subject Attendance Summary
  downloadRpt002: (params: { subjectId: string; sectionId: string; academicPeriodId: string }): Promise<void> => {
    return downloadFile('/reports/rpt-002', 'rpt-002-subject-summary.xlsx', params);
  },

  // RPT-003: Defaulter Report
  downloadRpt003: (params: {
    academicPeriodId: string;
    sectionId?: string;
    subjectId?: string;
    threshold?: number;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-003', 'rpt-003-defaulters.xlsx', params);
  },

  // RPT-004: Attendance Register
  downloadRpt004: (params: {
    subjectId: string;
    sectionId: string;
    academicPeriodId: string;
    startDate?: string;
    endDate?: string;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-004', 'rpt-004-attendance-register.xlsx', params);
  },

  // RPT-005: Overall Attendance Summary
  downloadRpt005: (params: {
    academicPeriodId: string;
    sectionId?: string;
    departmentId?: string;
    threshold?: number;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-005', 'rpt-005-overall-summary.xlsx', params);
  },

  // RPT-006: Faculty Marking Compliance
  downloadRpt006: (params: {
    academicPeriodId: string;
    facultyId?: string;
    departmentId?: string;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-006', 'rpt-006-faculty-compliance.xlsx', params);
  },

  // RPT-007: Condonation / Duty Leave Register
  downloadRpt007: (params: {
    academicPeriodId: string;
    sectionId?: string;
    subjectId?: string;
    leaveType?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-007', 'rpt-007-condonation-register.xlsx', params);
  },

  // RPT-008: Prediction Report
  downloadRpt008: (params: {
    subjectId: string;
    sectionId: string;
    academicPeriodId: string;
    futureSessions?: number;
    threshold?: number;
  }): Promise<void> => {
    return downloadFile('/reports/rpt-008', 'rpt-008-prediction.xlsx', params);
  },
};

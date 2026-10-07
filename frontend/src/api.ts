// API client and TypeScript interfaces for AMCS v2

const BASE_URL = '/api/v1';

// Safe storage wrapper for Safari / private browsing environments
const memoryStore: Record<string, string> = {};

function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return memoryStore[key] || null;
  }
}

function safeSetItem(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    memoryStore[key] = value;
  }
}

function safeRemoveItem(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    delete memoryStore[key];
  }
}

export function getAuthToken(): string | null {
  return safeGetItem('amcs_token');
}

export function setAuthToken(token: string | null) {
  if (token) {
    safeSetItem('amcs_token', token);
  } else {
    safeRemoveItem('amcs_token');
  }
}

export interface UserSession {
  username: string;
  role: 'ADMIN' | 'FACULTY';
  fullName: string;
}

export function getCurrentUser(): UserSession | null {
  const raw = safeGetItem('amcs_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSession;
  } catch {
    return null;
  }
}

export function setCurrentUser(user: UserSession | null) {
  if (user) {
    safeSetItem('amcs_user', JSON.stringify(user));
  } else {
    safeRemoveItem('amcs_user');
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  // Only send Authorization header for authenticated requests (NEVER for login)
  if (token && !endpoint.includes('/auth/login')) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if ((res.status === 401 || res.status === 403) && !endpoint.includes('/auth/login')) {
    // Unauthorized or session expired on protected endpoint: clear token
    setAuthToken(null);
    setCurrentUser(null);
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/login';
    }
    throw new Error('Unauthorized or session expired');
  }

  if (!res.ok) {
    let errorMsg = `Request failed: ${res.status}`;
    try {
      const data = await res.json();
      if (data && data.message) errorMsg = data.message;
    } catch {
      // ignore
    }
    throw new Error(errorMsg);
  }

  if (res.status === 204) {
    return {} as T;
  }

  return res.json() as Promise<T>;
}

// ==========================================
// INTERFACES
// ==========================================

export interface User {
  id: number;
  username: string;
  role: 'ADMIN' | 'FACULTY';
  fullName: string;
  active: boolean;
}

export interface AcademicTerm {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  active: boolean;
}

export interface Department {
  id: number;
  code: string;
  name: string;
}

export interface Holiday {
  id: number;
  date: string;
  name: string;
  createdAt?: string;
}

export interface Section {
  id: number;
  name: string;
  department: Department;
  term: AcademicTerm;
}

export interface Subject {
  id: number;
  code: string;
  name: string;
  type: 'THEORY' | 'LAB';
  credits: number;
}

export interface Faculty {
  id: number;
  employeeId: string;
  user: User;
  department: Department;
  active: boolean;
}

export interface Student {
  id: number;
  registrationNumber: string;
  name: string;
  email: string;
  department: Department;
  active: boolean;
}

export interface LabBatch {
  id: number;
  name: string;
  section: Section;
}

export interface Enrollment {
  id: number;
  student: Student;
  section: Section;
  labBatch: LabBatch | null;
}

export interface TimetableEntry {
  id: number;
  term: AcademicTerm;
  section: Section;
  subject: Subject;
  faculty: Faculty;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string;
  labBatch: LabBatch | null;
  active: boolean;
}

export interface ClassSession {
  id: number;
  timetableEntry: TimetableEntry;
  sessionDate: string;
  status: 'OPEN' | 'SUBMITTED';
  createdAt: string;
  submittedAt: string | null;
}

export interface TodayClassDto {
  timetableEntry: TimetableEntry;
  session: ClassSession | null;
}

export interface RosterStudent {
  studentId: number;
  registrationNumber: string;
  name: string;
  labBatchId: number | null;
  labBatchName: string | null;
  status: 'PRESENT' | 'ABSENT';
}

export interface SessionRosterResponse {
  sessionId: number;
  sessionDate: string;
  status: 'OPEN' | 'SUBMITTED';
  subjectCode: string;
  subjectName: string;
  subjectType: 'THEORY' | 'LAB';
  sectionName: string;
  labBatchId: number | null;
  labBatchName: string | null;
  facultyName: string;
  totalStudents: number;
  students: RosterStudent[];
}

export interface RegisterSessionColumn {
  id: number;
  date: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  subjectCode: string;
  subjectName: string;
  subjectType: string;
  labBatchId: number | null;
  labBatchName: string | null;
  room: string;
}

export interface RegisterStudentRow {
  studentId: number;
  registrationNumber: string;
  name: string;
  labBatchId: number | null;
  labBatchName: string | null;
  attendance: Record<number, string>;
  presentCount: number;
  absentCount: number;
  totalEligible: number;
  percentage: number;
}

export interface RegisterResponse {
  sectionId: number;
  sectionName: string;
  month: number | null;
  year: number | null;
  totalSessions: number;
  sessions: RegisterSessionColumn[];
  students: RegisterStudentRow[];
}

export interface SubjectAttendanceStat {
  subjectCode: string;
  subjectName: string;
  subjectType: string;
  present: number;
  absent: number;
  total: number;
  percentage: number;
}

export interface StudentMonthlySummary {
  studentId: number;
  registrationNumber: string;
  name: string;
  labBatchName: string | null;
  subjects: Record<string, SubjectAttendanceStat>;
  totalPresent: number;
  totalEligible: number;
  overallPercentage: number;
}

export interface MonthlySummaryResponse {
  sectionId: number;
  sectionName: string;
  month: number;
  year: number;
  totalSubmittedSessions: number;
  subjectCodes: string[];
  students: StudentMonthlySummary[];
  sectionAveragePercentage: number;
}

export interface SubjectComponentStat {
  present: number;
  total: number;
  percentage: number;
}

export interface SubjectWiseDetail {
  subjectCode: string;
  subjectName: string;
  theory: SubjectComponentStat;
  lab: SubjectComponentStat;
  combined: SubjectComponentStat;
}

export interface StudentSubjectWiseSummary {
  studentId: number;
  registrationNumber: string;
  name: string;
  labBatchName: string | null;
  subjects: Record<string, SubjectWiseDetail>;
  totalPresent: number;
  totalEligible: number;
  overallPercentage: number;
}

export interface SubjectWiseSummaryResponse {
  sectionId: number;
  sectionName: string;
  baseSubjectCodes: string[];
  students: StudentSubjectWiseSummary[];
  sectionAveragePercentage: number;
}

export interface StudentOverallSummary {
  studentId: number;
  registrationNumber: string;
  name: string;
  labBatchName: string | null;
  attendedClasses: number;
  absentClasses: number;
  totalClasses: number;
  percentage: number;
  status: 'GOOD' | 'WARNING' | 'CRITICAL';
}

export interface OverallSummaryResponse {
  sectionId: number;
  sectionName: string;
  totalStudents: number;
  totalSubmittedSessions: number;
  averageAttendancePercentage: number;
  goodCount: number;
  warningCount: number;
  criticalCount: number;
  students: StudentOverallSummary[];
}

// Calendar Status Interface
export interface CalendarDayStatus {
  date: string;
  dayOfWeek: string;
  isWorkingDay: boolean;
  isSunday: boolean;
  isSecondSaturday: boolean;
  isDeclaredHoliday: boolean;
  holidayName: string | null;
  message: string;
}

// Schedule Import Interfaces
export interface ScheduleImportRequest {
  academicTerm: {
    name: string;
    semester?: string;
    startDate?: string;
    endDate?: string;
  };
  departments?: Array<{ code: string; name: string }>;
  sections?: Array<{ name: string; department: string; year?: number }>;
  faculty?: Array<{ name: string; employeeId?: string; department: string; username?: string }>;
  subjects?: Array<{ code: string; name: string; type: 'THEORY' | 'LAB'; credits?: number }>;
  labBatches?: Array<{ name: string; section: string }>;
  timetableEntries: Array<{
    section: string;
    subject: string;
    faculty: string;
    day: string;
    startTime: string;
    endTime: string;
    room?: string;
    labBatch?: string;
  }>;
}

export interface ImportPreviewResponse {
  valid: boolean;
  sectionsCount: number;
  facultyCount: number;
  subjectsCount: number;
  labBatchesCount: number;
  timetableEntriesCount: number;
  autoCreatedFaculty: Array<{
    name: string;
    employeeId: string;
    department: string;
  }>;
  warnings: string[];
  errors: string[];
}

export interface ImportCommitResponse {
  success: boolean;
  message: string;
  sectionsCreated: number;
  facultyProfilesCreated: number;
  subjectsCreated: number;
  labBatchesCreated: number;
  timetableEntriesCreated: number;
}


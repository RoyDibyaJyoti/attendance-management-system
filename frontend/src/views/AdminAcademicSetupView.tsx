import React, { useState, useEffect } from 'react';
import {
  apiFetch,
  Student,
  Subject,
  Faculty,
  Section,
  AcademicTerm,
  Department,
  LabBatch,
  Enrollment,
  Holiday,
} from '../api';
import {
  Users,
  BookOpen,
  GraduationCap,
  Layers,
  Calendar,
  Search,
  CheckCircle2,
  AlertCircle,
  FlaskConical,
  Plus,
  Pencil,
  Trash2,
  X,
  Check,
  Building2,
  UserPlus,
  UserCheck,
  Sparkles,
} from 'lucide-react';
import { ScheduleImportModal } from '../components/ScheduleImportModal';

export const AdminAcademicSetupView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<
    'STUDENTS' | 'ENROLLMENT' | 'BATCHES' | 'SUBJECTS' | 'FACULTY' | 'STRUCTURE' | 'HOLIDAYS'
  >('STUDENTS');

  // Master Data States
  const [students, setStudents] = useState<Student[]>([]);
  const [studentSearch, setStudentSearch] = useState('');
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [faculty, setFaculty] = useState<Faculty[]>([]);
  const [terms, setTerms] = useState<AcademicTerm[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);
  const [batches, setBatches] = useState<LabBatch[]>([]);
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [holidays, setHolidays] = useState<Holiday[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modals state
  const [modalType, setModalType] = useState<string | null>(null);
  const [editingItem, setEditingItem] = useState<any | null>(null);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<{
    type: string;
    id: number;
    title: string;
  } | null>(null);

  // Form states
  const [holidayForm, setHolidayForm] = useState({
    date: '',
    name: '',
  });

  // Form states
  const [studentForm, setStudentForm] = useState({
    registrationNumber: '',
    name: '',
    email: '',
    departmentId: 0,
    active: true,
  });

  const [subjectForm, setSubjectForm] = useState({
    code: '',
    name: '',
    type: 'THEORY',
    credits: 3,
  });

  const [facultyForm, setFacultyForm] = useState({
    employeeId: '',
    fullName: '',
    username: '',
    password: '',
    departmentId: 0,
    active: true,
  });

  const [termForm, setTermForm] = useState({
    name: '',
    startDate: '',
    endDate: '',
    active: true,
  });

  const [deptForm, setDeptForm] = useState({
    code: '',
    name: '',
  });

  const [sectionForm, setSectionForm] = useState({
    name: '',
    departmentId: 0,
    termId: 0,
  });

  const [batchForm, setBatchForm] = useState({
    name: '',
    sectionId: 0,
  });

  const [enrollForm, setEnrollForm] = useState({
    studentId: 0,
    sectionId: 0,
    labBatchId: 0,
  });
  const [enrollSearchQuery, setEnrollSearchQuery] = useState('');
  const [enrollmentRosterQuery, setEnrollmentRosterQuery] = useState('');

  useEffect(() => {
    loadAllData();
  }, []);

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(null), 3500);
  };

  const loadAllData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [
        studentsRes,
        subjectsRes,
        facultyRes,
        termsRes,
        deptsRes,
        sectionsRes,
        holidaysRes,
      ] = await Promise.all([
        apiFetch<{ content: Student[] }>('/students?size=300'),
        apiFetch<Subject[]>('/subjects'),
        apiFetch<Faculty[]>('/faculty'),
        apiFetch<AcademicTerm[]>('/terms'),
        apiFetch<Department[]>('/departments'),
        apiFetch<Section[]>('/sections'),
        apiFetch<Holiday[]>('/holidays'),
      ]);

      setStudents(studentsRes.content || []);
      setSubjects(subjectsRes);
      setFaculty(facultyRes);
      setTerms(termsRes);
      setDepartments(deptsRes);
      setSections(sectionsRes);
      setHolidays(holidaysRes);

      const secId = selectedSectionId || (sectionsRes.length > 0 ? sectionsRes[0].id : null);
      if (secId) {
        setSelectedSectionId(secId);
        await loadSectionDetails(secId);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load master setup data');
    } finally {
      setLoading(false);
    }
  };

  const loadSectionDetails = async (secId: number) => {
    try {
      const [batchRes, enrollRes] = await Promise.all([
        apiFetch<LabBatch[]>(`/lab-batches?sectionId=${secId}`),
        apiFetch<Enrollment[]>(`/enrollments?sectionId=${secId}`),
      ]);
      setBatches(batchRes);
      setEnrollments(enrollRes);
    } catch (err: any) {
      setError(err.message || 'Failed to load section data');
    }
  };

  const handleSectionChange = async (secId: number) => {
    setSelectedSectionId(secId);
    await loadSectionDetails(secId);
  };

  // ==========================================
  // STUDENT CRUD
  // ==========================================
  const openAddStudent = () => {
    setStudentForm({
      registrationNumber: '',
      name: '',
      email: '',
      departmentId: departments[0]?.id || 0,
      active: true,
    });
    setEditingItem(null);
    setModalType('STUDENT');
  };

  const openEditStudent = (s: Student) => {
    setStudentForm({
      registrationNumber: s.registrationNumber,
      name: s.name,
      email: s.email || '',
      departmentId: s.department?.id || departments[0]?.id || 0,
      active: s.active,
    });
    setEditingItem(s);
    setModalType('STUDENT');
  };

  const handleSaveStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/students/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(studentForm),
        });
        showSuccess(`Student ${studentForm.registrationNumber} updated.`);
      } else {
        await apiFetch('/students', {
          method: 'POST',
          body: JSON.stringify(studentForm),
        });
        showSuccess(`Student ${studentForm.registrationNumber} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // SUBJECT CRUD
  // ==========================================
  const openAddSubject = () => {
    setSubjectForm({
      code: '',
      name: '',
      type: 'THEORY',
      credits: 3,
    });
    setEditingItem(null);
    setModalType('SUBJECT');
  };

  const openEditSubject = (s: Subject) => {
    setSubjectForm({
      code: s.code,
      name: s.name,
      type: s.type,
      credits: s.credits,
    });
    setEditingItem(s);
    setModalType('SUBJECT');
  };

  const handleSaveSubject = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/subjects/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(subjectForm),
        });
        showSuccess(`Subject ${subjectForm.code} updated.`);
      } else {
        await apiFetch('/subjects', {
          method: 'POST',
          body: JSON.stringify(subjectForm),
        });
        showSuccess(`Subject ${subjectForm.code} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // FACULTY CRUD
  // ==========================================
  const openAddFaculty = () => {
    setFacultyForm({
      employeeId: '',
      fullName: '',
      username: '',
      password: '',
      departmentId: departments[0]?.id || 0,
      active: true,
    });
    setEditingItem(null);
    setModalType('FACULTY');
  };

  const openEditFaculty = (f: Faculty) => {
    setFacultyForm({
      employeeId: f.employeeId,
      fullName: f.user?.fullName || '',
      username: f.user?.username || '',
      password: '',
      departmentId: f.department?.id || departments[0]?.id || 0,
      active: f.active,
    });
    setEditingItem(f);
    setModalType('FACULTY');
  };

  const handleSaveFaculty = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/faculty/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            employeeId: facultyForm.employeeId,
            fullName: facultyForm.fullName,
            departmentId: facultyForm.departmentId,
            active: facultyForm.active,
            password: facultyForm.password.trim() || undefined,
          }),
        });
        showSuccess(`Faculty ${facultyForm.fullName} updated.`);
      } else {
        await apiFetch('/faculty', {
          method: 'POST',
          body: JSON.stringify(facultyForm),
        });
        showSuccess(`Faculty ${facultyForm.fullName} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // LAB BATCH CRUD
  // ==========================================
  const openAddBatch = () => {
    setBatchForm({
      name: '',
      sectionId: selectedSectionId || sections[0]?.id || 0,
    });
    setEditingItem(null);
    setModalType('BATCH');
  };

  const openEditBatch = (b: LabBatch) => {
    setBatchForm({
      name: b.name,
      sectionId: b.section?.id || selectedSectionId || 0,
    });
    setEditingItem(b);
    setModalType('BATCH');
  };

  const handleSaveBatch = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/lab-batches/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(batchForm),
        });
        showSuccess(`Batch ${batchForm.name} updated.`);
      } else {
        await apiFetch('/lab-batches', {
          method: 'POST',
          body: JSON.stringify(batchForm),
        });
        showSuccess(`Batch ${batchForm.name} created.`);
      }
      setModalType(null);
      if (selectedSectionId) await loadSectionDetails(selectedSectionId);
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // ENROLLMENT MANAGEMENT
  // ==========================================
  const openEnrollModal = () => {
    setEnrollSearchQuery('');
    // find students not enrolled in this section yet
    const enrolledIds = new Set(enrollments.map((e) => e.student.id));
    const availableStudents = students.filter((s) => !enrolledIds.has(s.id));
    setEnrollForm({
      studentId: availableStudents[0]?.id || 0,
      sectionId: selectedSectionId || 0,
      labBatchId: 0,
    });
    setModalType('ENROLL');
  };

  const handleSaveEnrollment = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiFetch('/enrollments', {
        method: 'POST',
        body: JSON.stringify({
          studentId: enrollForm.studentId,
          sectionId: enrollForm.sectionId,
          labBatchId: enrollForm.labBatchId > 0 ? enrollForm.labBatchId : null,
        }),
      });
      showSuccess('Student enrolled successfully.');
      setModalType(null);
      if (selectedSectionId) await loadSectionDetails(selectedSectionId);
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleUpdateStudentBatch = async (enrollmentId: number, batchId: number | null) => {
    try {
      const url = batchId && batchId > 0
        ? `/enrollments/${enrollmentId}/batch?labBatchId=${batchId}`
        : `/enrollments/${enrollmentId}/batch`;
      await apiFetch(url, { method: 'PUT' });
      showSuccess('Lab batch updated.');
      if (selectedSectionId) await loadSectionDetails(selectedSectionId);
    } catch (err: any) {
      alert('Failed to update lab batch: ' + err.message);
    }
  };

  const handleUnenroll = async (enrollmentId: number, studentName: string) => {
    if (!confirm(`Are you sure you want to remove ${studentName} from this section?`)) return;
    try {
      await apiFetch(`/enrollments/${enrollmentId}`, { method: 'DELETE' });
      showSuccess(`Removed ${studentName} from section.`);
      if (selectedSectionId) await loadSectionDetails(selectedSectionId);
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // TERMS & DEPARTMENTS & SECTIONS
  // ==========================================
  const openAddTerm = () => {
    setTermForm({ name: '', startDate: '', endDate: '', active: true });
    setEditingItem(null);
    setModalType('TERM');
  };

  const openEditTerm = (t: AcademicTerm) => {
    setTermForm({
      name: t.name,
      startDate: t.startDate,
      endDate: t.endDate,
      active: t.active,
    });
    setEditingItem(t);
    setModalType('TERM');
  };

  const handleSaveTerm = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/terms/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(termForm),
        });
        showSuccess(`Term ${termForm.name} updated.`);
      } else {
        await apiFetch('/terms', {
          method: 'POST',
          body: JSON.stringify(termForm),
        });
        showSuccess(`Term ${termForm.name} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const openAddDept = () => {
    setDeptForm({ code: '', name: '' });
    setEditingItem(null);
    setModalType('DEPT');
  };

  const openEditDept = (d: Department) => {
    setDeptForm({ code: d.code, name: d.name });
    setEditingItem(d);
    setModalType('DEPT');
  };

  const handleSaveDept = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/departments/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(deptForm),
        });
        showSuccess(`Department ${deptForm.code} updated.`);
      } else {
        await apiFetch('/departments', {
          method: 'POST',
          body: JSON.stringify(deptForm),
        });
        showSuccess(`Department ${deptForm.code} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const openAddSection = () => {
    setSectionForm({
      name: '',
      departmentId: departments[0]?.id || 0,
      termId: terms[0]?.id || 0,
    });
    setEditingItem(null);
    setModalType('SECTION');
  };

  const openEditSection = (s: Section) => {
    setSectionForm({
      name: s.name,
      departmentId: s.department?.id || departments[0]?.id || 0,
      termId: s.term?.id || terms[0]?.id || 0,
    });
    setEditingItem(s);
    setModalType('SECTION');
  };

  const handleSaveSection = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/sections/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(sectionForm),
        });
        showSuccess(`Section ${sectionForm.name} updated.`);
      } else {
        await apiFetch('/sections', {
          method: 'POST',
          body: JSON.stringify(sectionForm),
        });
        showSuccess(`Section ${sectionForm.name} created.`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // HOLIDAY CRUD
  // ==========================================
  const openAddHoliday = () => {
    setHolidayForm({
      date: '',
      name: '',
    });
    setEditingItem(null);
    setModalType('HOLIDAY');
  };

  const openEditHoliday = (h: Holiday) => {
    setHolidayForm({
      date: h.date,
      name: h.name,
    });
    setEditingItem(h);
    setModalType('HOLIDAY');
  };

  const handleSaveHoliday = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingItem) {
        await apiFetch(`/holidays/${editingItem.id}`, {
          method: 'PUT',
          body: JSON.stringify(holidayForm),
        });
        showSuccess(`Holiday updated: ${holidayForm.name}`);
      } else {
        await apiFetch('/holidays', {
          method: 'POST',
          body: JSON.stringify(holidayForm),
        });
        showSuccess(`Holiday declared: ${holidayForm.name}`);
      }
      setModalType(null);
      await loadAllData();
    } catch (err: any) {
      alert(err.message);
    }
  };

  // ==========================================
  // GENERIC DELETE EXECUTION
  // ==========================================
  const executeDelete = async () => {
    if (!deleteConfirm) return;
    const { type, id, title } = deleteConfirm;
    try {
      let endpoint = '';
      if (type === 'STUDENT') endpoint = `/students/${id}`;
      else if (type === 'SUBJECT') endpoint = `/subjects/${id}`;
      else if (type === 'FACULTY') endpoint = `/faculty/${id}`;
      else if (type === 'TERM') endpoint = `/terms/${id}`;
      else if (type === 'DEPT') endpoint = `/departments/${id}`;
      else if (type === 'SECTION') endpoint = `/sections/${id}`;
      else if (type === 'BATCH') endpoint = `/lab-batches/${id}`;
      else if (type === 'HOLIDAY') endpoint = `/holidays/${id}`;

      await apiFetch(endpoint, { method: 'DELETE' });
      showSuccess(`${title} deleted/deactivated successfully.`);
      setDeleteConfirm(null);
      await loadAllData();
    } catch (err: any) {
      alert('Delete failed: ' + err.message);
    }
  };

  const filteredStudents = students.filter(
    (s) =>
      s.registrationNumber.toLowerCase().includes(studentSearch.toLowerCase()) ||
      s.name.toLowerCase().includes(studentSearch.toLowerCase())
  );

  const selectedSection = sections.find((s) => s.id === selectedSectionId);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-700 text-xs font-semibold mb-2">
            <Layers className="w-3.5 h-3.5" />
            Institutional Administration
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Academic Data & Master Setup
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Full Create, Read, Update, and Management for all AMCS Academic Entities.
          </p>
        </div>

        <button
          onClick={() => setIsImportModalOpen(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white text-xs font-bold transition shadow-md shadow-indigo-100 self-start sm:self-auto"
        >
          <Sparkles className="w-4 h-4" />
          Import Schedule (AI / JSON)
        </button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-rose-500 hover:text-rose-800">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
          <span className="font-semibold">{successMsg}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-1 overflow-x-auto">
        <button
          onClick={() => setActiveTab('STUDENTS')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'STUDENTS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Users className="w-4 h-4" />
          Students ({students.length})
        </button>

        <button
          onClick={() => setActiveTab('ENROLLMENT')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'ENROLLMENT'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <UserPlus className="w-4 h-4" />
          Section Enrollment ({enrollments.length})
        </button>

        <button
          onClick={() => setActiveTab('BATCHES')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'BATCHES'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FlaskConical className="w-4 h-4" />
          Lab Batches ({batches.length})
        </button>

        <button
          onClick={() => setActiveTab('SUBJECTS')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'SUBJECTS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          Subjects ({subjects.length})
        </button>

        <button
          onClick={() => setActiveTab('FACULTY')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'FACULTY'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <GraduationCap className="w-4 h-4" />
          Faculty ({faculty.length})
        </button>

        <button
          onClick={() => setActiveTab('STRUCTURE')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'STRUCTURE'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Building2 className="w-4 h-4" />
          Terms, Depts & Sections
        </button>

        <button
          onClick={() => setActiveTab('HOLIDAYS')}
          className={`py-3 px-4 font-bold text-xs flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'HOLIDAYS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Calendar className="w-4 h-4" />
          Holidays ({holidays.length})
        </button>
      </div>

      {/* Main Content Area */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
          <p className="text-xs font-semibold">Loading master records...</p>
        </div>
      ) : (
        <div>
          {/* TAB 1: STUDENTS */}
          {activeTab === 'STUDENTS' && (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="relative flex-1 max-w-md">
                  <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={studentSearch}
                    onChange={(e) => setStudentSearch(e.target.value)}
                    placeholder="Search students by roll or name..."
                    className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
                  />
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-xs text-slate-500 font-medium">
                    {filteredStudents.length} students found
                  </div>
                  <button
                    onClick={openAddStudent}
                    className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-all"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Add Student
                  </button>
                </div>
              </div>

              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="overflow-x-auto max-h-[600px]">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-700 uppercase font-bold sticky top-0 border-b border-slate-200">
                      <tr>
                        <th className="py-3 px-4 w-12 text-center">#</th>
                        <th className="py-3 px-4 w-40">Registration No</th>
                        <th className="py-3 px-4">Student Name</th>
                        <th className="py-3 px-4">Email</th>
                        <th className="py-3 px-4 w-28">Department</th>
                        <th className="py-3 px-4 w-24 text-center">Status</th>
                        <th className="py-3 px-4 w-24 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                      {filteredStudents.map((s, idx) => (
                        <tr key={s.id} className="hover:bg-slate-50/70">
                          <td className="py-2.5 px-4 text-center text-slate-400 font-mono">
                            {idx + 1}
                          </td>
                          <td className="py-2.5 px-4 font-bold text-slate-900">
                            {s.registrationNumber}
                          </td>
                          <td className="py-2.5 px-4 font-sans font-medium text-slate-800">
                            {s.name}
                          </td>
                          <td className="py-2.5 px-4 text-slate-500 font-sans">
                            {s.email || '—'}
                          </td>
                          <td className="py-2.5 px-4 font-sans font-semibold text-slate-700">
                            {s.department?.code}
                          </td>
                          <td className="py-2.5 px-4 text-center font-sans">
                            <span
                              className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                s.active
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : 'bg-slate-100 text-slate-500'
                              }`}
                            >
                              {s.active ? 'Active' : 'Inactive'}
                            </span>
                          </td>
                          <td className="py-2.5 px-4 text-right font-sans space-x-1">
                            <button
                              onClick={() => openEditStudent(s)}
                              title="Edit Student"
                              className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded"
                            >
                              <Pencil className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() =>
                                setDeleteConfirm({
                                  type: 'STUDENT',
                                  id: s.id,
                                  title: `Student ${s.name} (${s.registrationNumber})`,
                                })
                              }
                              title="Delete Student"
                              className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: SECTION ENROLLMENT */}
          {activeTab === 'ENROLLMENT' && (
            <div className="space-y-6">
              {/* Section Header & Selector */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                    <UserPlus className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-slate-500 font-medium">Managing Enrollment For:</div>
                    <div className="flex items-center gap-2 mt-0.5">
                      <select
                        value={selectedSectionId || ''}
                        onChange={(e) => handleSectionChange(Number(e.target.value))}
                        className="py-1.5 px-3 bg-slate-50 border border-slate-300 rounded-lg text-xs font-bold text-slate-900"
                      >
                        {sections.map((s) => (
                          <option key={s.id} value={s.id}>
                            Section: {s.name} ({s.department?.code} • {s.term?.name})
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="text-xs text-slate-500 font-semibold">
                    Total Enrolled: <span className="text-indigo-600 font-black">{enrollments.length}</span>
                  </div>
                  <button
                    onClick={openEnrollModal}
                    className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Enroll Student
                  </button>
                </div>
              </div>

              {/* Enrolled Students Table */}
              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="p-4 bg-slate-50 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h3 className="text-xs font-bold uppercase text-slate-700 tracking-wider">
                      Enrolled Students in {selectedSection?.name || 'Section'} ({enrollments.length})
                    </h3>
                    <span className="text-[11px] text-slate-500">
                      Directly assign or clear Lab Batches below
                    </span>
                  </div>
                  <div className="relative max-w-xs w-full">
                    <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
                    <input
                      type="text"
                      placeholder="Filter roster by name or reg no..."
                      value={enrollmentRosterQuery}
                      onChange={(e) => setEnrollmentRosterQuery(e.target.value)}
                      className="w-full pl-8 pr-3 py-1.5 bg-white border border-slate-300 rounded-xl text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>
                <div className="overflow-x-auto max-h-[550px]">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 sticky top-0 border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-4 w-12 text-center">#</th>
                        <th className="py-2.5 px-4 w-40">Registration No</th>
                        <th className="py-2.5 px-4">Student Name</th>
                        <th className="py-2.5 px-4 w-56">Assigned Lab Batch</th>
                        <th className="py-2.5 px-4 w-28 text-right">Remove</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                      {(() => {
                        const filtered = enrollments.filter((e) => {
                          if (!enrollmentRosterQuery.trim()) return true;
                          const q = enrollmentRosterQuery.toLowerCase().trim();
                          return (
                            e.student?.name.toLowerCase().includes(q) ||
                            e.student?.registrationNumber.toLowerCase().includes(q)
                          );
                        });
                        if (filtered.length === 0) {
                          return (
                            <tr>
                              <td colSpan={5} className="py-8 text-center text-slate-400 font-sans italic">
                                {enrollmentRosterQuery
                                  ? `No enrolled students matching "${enrollmentRosterQuery}"`
                                  : "No students enrolled in this section yet. Click 'Enroll Student' above."}
                              </td>
                            </tr>
                          );
                        }
                        return filtered.map((e, idx) => (
                          <tr key={e.id} className="hover:bg-slate-50/70">
                            <td className="py-2 px-4 text-center text-slate-400">
                              {idx + 1}
                            </td>
                            <td className="py-2 px-4 font-bold text-slate-900">
                              {e.student?.registrationNumber}
                            </td>
                            <td className="py-2 px-4 font-sans font-medium text-slate-800">
                              {e.student?.name}
                            </td>
                            <td className="py-2 px-4 font-sans">
                              <select
                                value={e.labBatch?.id || 0}
                                onChange={(evt) =>
                                  handleUpdateStudentBatch(
                                    e.id,
                                    evt.target.value === '0' ? null : Number(evt.target.value)
                                  )
                                }
                                className={`py-1 px-2.5 rounded-lg text-xs font-semibold border ${
                                  e.labBatch
                                    ? 'bg-purple-50 text-purple-800 border-purple-200'
                                    : 'bg-slate-50 text-slate-500 border-slate-200'
                                }`}
                              >
                                <option value="0">— Unassigned —</option>
                                {batches.map((b) => (
                                  <option key={b.id} value={b.id}>
                                    {b.name}
                                  </option>
                                ))}
                              </select>
                            </td>
                            <td className="py-2 px-4 text-right font-sans">
                              <button
                                onClick={() => handleUnenroll(e.id, e.student?.name)}
                                title="Unenroll student"
                                className="py-1 px-2 text-rose-600 hover:bg-rose-50 rounded text-xs font-semibold"
                              >
                                Remove
                              </button>
                            </td>
                          </tr>
                        ));
                      })()}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: LAB BATCHES */}
          {activeTab === 'BATCHES' && (
            <div className="space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-500 font-semibold">Section:</span>
                  <select
                    value={selectedSectionId || ''}
                    onChange={(e) => handleSectionChange(Number(e.target.value))}
                    className="py-1.5 px-3 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-900 shadow-sm"
                  >
                    {sections.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.department?.code})
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  onClick={openAddBatch}
                  className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Add Lab Batch
                </button>
              </div>

              {/* Batches Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {batches.length === 0 ? (
                  <div className="col-span-3 bg-white rounded-2xl border border-slate-200 p-8 text-center text-slate-400 italic text-xs">
                    No lab batches created for this section yet. Click 'Add Lab Batch' to create one.
                  </div>
                ) : (
                  batches.map((b) => {
                    const count = enrollments.filter((e) => e.labBatch?.id === b.id).length;
                    return (
                      <div
                        key={b.id}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between"
                      >
                        <div>
                          <div className="flex items-start justify-between">
                            <div className="flex items-center gap-3 mb-2">
                              <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center font-bold">
                                <FlaskConical className="w-5 h-5" />
                              </div>
                              <div>
                                <h3 className="font-bold text-slate-900 text-sm">{b.name}</h3>
                                <span className="text-xs text-slate-500">
                                  Section {b.section?.name || selectedSection?.name}
                                </span>
                              </div>
                            </div>

                            <div className="flex items-center gap-1">
                              <button
                                onClick={() => openEditBatch(b)}
                                className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded"
                              >
                                <Pencil className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() =>
                                  setDeleteConfirm({
                                    type: 'BATCH',
                                    id: b.id,
                                    title: `Lab Batch ${b.name}`,
                                  })
                                }
                                className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>
                        </div>

                        <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center text-xs">
                          <span className="text-slate-500 font-medium">Assigned Students:</span>
                          <span className="font-black text-purple-700 text-sm">{count}</span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}

          {/* TAB 4: SUBJECTS */}
          {activeTab === 'SUBJECTS' && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <p className="text-xs text-slate-500">
                  Manage curriculum courses. Clearly differentiate between THEORY and LAB components.
                </p>
                <button
                  onClick={openAddSubject}
                  className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Add Subject
                </button>
              </div>

              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-700 uppercase font-bold border-b border-slate-200">
                    <tr>
                      <th className="py-3 px-4 w-12 text-center">#</th>
                      <th className="py-3 px-4 w-32">Course Code</th>
                      <th className="py-3 px-4">Course Name</th>
                      <th className="py-3 px-4 w-36">Type</th>
                      <th className="py-3 px-4 w-28 text-center">Credits</th>
                      <th className="py-3 px-4 w-24 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {subjects.map((sub, idx) => {
                      const isLab = sub.type === 'LAB';
                      return (
                        <tr key={sub.id} className="hover:bg-slate-50/70">
                          <td className="py-3 px-4 text-center text-slate-400 font-mono">
                            {idx + 1}
                          </td>
                          <td className="py-3 px-4 font-black text-slate-900 font-mono">
                            {sub.code}
                          </td>
                          <td className="py-3 px-4 font-medium text-slate-800">
                            {sub.name}
                          </td>
                          <td className="py-3 px-4">
                            <span
                              className={`px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1 ${
                                isLab
                                  ? 'bg-purple-100 text-purple-700'
                                  : 'bg-blue-100 text-blue-700'
                              }`}
                            >
                              {isLab ? <FlaskConical className="w-3 h-3" /> : <BookOpen className="w-3 h-3" />}
                              {sub.type}
                            </span>
                          </td>
                          <td className="py-3 px-4 text-center font-bold text-slate-700">
                            {sub.credits}
                          </td>
                          <td className="py-3 px-4 text-right space-x-1">
                            <button
                              onClick={() => openEditSubject(sub)}
                              title="Edit Subject"
                              className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded"
                            >
                              <Pencil className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() =>
                                setDeleteConfirm({
                                  type: 'SUBJECT',
                                  id: sub.id,
                                  title: `Subject ${sub.code} (${sub.name})`,
                                })
                              }
                              title="Delete Subject"
                              className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 5: FACULTY */}
          {activeTab === 'FACULTY' && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <p className="text-xs text-slate-500">
                  Manage teaching faculty accounts and departmental associations.
                </p>
                <button
                  onClick={openAddFaculty}
                  className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Add Faculty
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {faculty.map((f) => (
                  <div
                    key={f.id}
                    className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex items-start justify-between"
                  >
                    <div className="flex items-start gap-3.5">
                      <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-black text-base">
                        {f.user?.fullName?.charAt(0) || 'F'}
                      </div>
                      <div>
                        <h3 className="font-bold text-slate-900 text-sm">
                          {f.user?.fullName}
                        </h3>
                        <div className="text-xs text-slate-500 mt-0.5">
                          Username: <code className="font-mono font-bold text-slate-700">{f.user?.username}</code>
                        </div>
                        <div className="text-xs text-slate-500">
                          Employee ID: <span className="font-mono font-semibold">{f.employeeId}</span>
                        </div>
                        <div className="text-xs text-slate-500">
                          Department: <span className="font-semibold text-slate-700">{f.department?.name}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          f.active
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {f.active ? 'Active' : 'Inactive'}
                      </span>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => openEditFaculty(f)}
                          className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded"
                          title="Edit Faculty"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() =>
                            setDeleteConfirm({
                              type: 'FACULTY',
                              id: f.id,
                              title: `Faculty ${f.user?.fullName} (${f.employeeId})`,
                            })
                          }
                          className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded"
                          title="Delete Faculty"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 6: TERMS, DEPARTMENTS & SECTIONS */}
          {activeTab === 'STRUCTURE' && (
            <div className="space-y-8">
              {/* ACADEMIC TERMS */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
                <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">Academic Terms</h3>
                    <p className="text-xs text-slate-500">Manage semesters and active academic sessions</p>
                  </div>
                  <button
                    onClick={openAddTerm}
                    className="py-1.5 px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Add Term
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {terms.map((t) => (
                    <div
                      key={t.id}
                      className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50 flex items-center justify-between"
                    >
                      <div>
                        <div className="font-bold text-slate-900 text-xs">{t.name}</div>
                        <div className="text-[11px] text-slate-500 mt-0.5 font-mono">
                          {t.startDate} to {t.endDate}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {t.active && (
                          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald-100 text-emerald-800">
                            Active
                          </span>
                        )}
                        <button
                          onClick={() => openEditTerm(t)}
                          className="p-1 text-slate-400 hover:text-indigo-600 rounded"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() =>
                            setDeleteConfirm({
                              type: 'TERM',
                              id: t.id,
                              title: `Academic Term ${t.name}`,
                            })
                          }
                          className="p-1 text-slate-400 hover:text-rose-600 rounded"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* DEPARTMENTS */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
                <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">Academic Departments</h3>
                    <p className="text-xs text-slate-500">Degree-offering departments</p>
                  </div>
                  <button
                    onClick={openAddDept}
                    className="py-1.5 px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Add Department
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {departments.map((d) => (
                    <div
                      key={d.id}
                      className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50 flex items-center justify-between"
                    >
                      <div>
                        <div className="font-black text-slate-900 text-xs font-mono">{d.code}</div>
                        <div className="text-[11px] text-slate-600 mt-0.5 font-medium">{d.name}</div>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => openEditDept(d)}
                          className="p-1 text-slate-400 hover:text-indigo-600 rounded"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() =>
                            setDeleteConfirm({
                              type: 'DEPT',
                              id: d.id,
                              title: `Department ${d.code} (${d.name})`,
                            })
                          }
                          className="p-1 text-slate-400 hover:text-rose-600 rounded"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* SECTIONS */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
                <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                  <div>
                    <h3 className="font-bold text-sm text-slate-900">Class Sections</h3>
                    <p className="text-xs text-slate-500">Cohort sections tied to Department & Term</p>
                  </div>
                  <button
                    onClick={openAddSection}
                    className="py-1.5 px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Add Section
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {sections.map((sec) => (
                    <div
                      key={sec.id}
                      className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50 flex items-center justify-between"
                    >
                      <div>
                        <div className="font-bold text-slate-900 text-xs">Section {sec.name}</div>
                        <div className="text-[11px] text-slate-500 mt-0.5">
                          Dept: {sec.department?.code} • Term: {sec.term?.name}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => openEditSection(sec)}
                          className="p-1 text-slate-400 hover:text-indigo-600 rounded"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() =>
                            setDeleteConfirm({
                              type: 'SECTION',
                              id: sec.id,
                              title: `Section ${sec.name}`,
                            })
                          }
                          className="p-1 text-slate-400 hover:text-rose-600 rounded"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 7: HOLIDAYS */}
          {activeTab === 'HOLIDAYS' && (
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Institutional Calendar & Declared Holidays
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Declare non-working holidays to prevent invalid class session opening on specific dates.
                  </p>
                </div>
                <button
                  onClick={openAddHoliday}
                  className="py-2 px-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm transition-all"
                >
                  <Plus className="w-4 h-4" />
                  Declare Holiday
                </button>
              </div>

              {holidays.length === 0 ? (
                <div className="text-center py-12 text-slate-400 text-xs">
                  No holidays declared yet. All regular scheduled classes may take attendance.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-200 text-slate-400 font-bold uppercase tracking-wider text-[10px]">
                        <th className="py-3 px-4">Date</th>
                        <th className="py-3 px-4">Day of Week</th>
                        <th className="py-3 px-4">Holiday Name / Reason</th>
                        <th className="py-3 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {holidays.map((h) => {
                        const [y, m, d] = h.date.split('-').map(Number);
                        const dayName = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][
                          new Date(y, m - 1, d).getDay()
                        ];

                        return (
                          <tr key={h.id} className="hover:bg-slate-50/80 transition-colors">
                            <td className="py-3 px-4 font-mono font-bold text-slate-900">
                              {h.date}
                            </td>
                            <td className="py-3 px-4 font-semibold text-slate-600">
                              {dayName}
                            </td>
                            <td className="py-3 px-4 font-bold text-slate-800">
                              {h.name}
                            </td>
                            <td className="py-3 px-4 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <button
                                  onClick={() => openEditHoliday(h)}
                                  className="p-1 text-slate-400 hover:text-indigo-600 rounded"
                                  title="Edit Holiday"
                                >
                                  <Pencil className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() =>
                                    setDeleteConfirm({
                                      type: 'HOLIDAY',
                                      id: h.id,
                                      title: `Holiday ${h.name} (${h.date})`,
                                    })
                                  }
                                  className="p-1 text-slate-400 hover:text-rose-600 rounded"
                                  title="Delete Holiday"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ==========================================
          MODALS
      ========================================== */}

      {/* STUDENT MODAL */}
      {modalType === 'STUDENT' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Student' : 'Add New Student'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveStudent} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Registration Number</label>
                <input
                  type="text"
                  required
                  value={studentForm.registrationNumber}
                  onChange={(e) => setStudentForm({ ...studentForm, registrationNumber: e.target.value })}
                  placeholder="e.g. 325106410125"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Student Full Name</label>
                <input
                  type="text"
                  required
                  value={studentForm.name}
                  onChange={(e) => setStudentForm({ ...studentForm, name: e.target.value })}
                  placeholder="e.g. Aarav Sharma"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Email Address</label>
                <input
                  type="email"
                  value={studentForm.email}
                  onChange={(e) => setStudentForm({ ...studentForm, email: e.target.value })}
                  placeholder="aarav@example.edu"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Department</label>
                <select
                  value={studentForm.departmentId}
                  onChange={(e) => setStudentForm({ ...studentForm, departmentId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                >
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.code} — {d.name}
                    </option>
                  ))}
                </select>
              </div>

              {editingItem && (
                <div className="flex items-center gap-2 pt-1">
                  <input
                    type="checkbox"
                    id="studentActiveCheck"
                    checked={studentForm.active}
                    onChange={(e) => setStudentForm({ ...studentForm, active: e.target.checked })}
                    className="w-4 h-4 text-indigo-600 rounded"
                  />
                  <label htmlFor="studentActiveCheck" className="font-semibold text-slate-700">
                    Active Student Status
                  </label>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Student
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SUBJECT MODAL */}
      {modalType === 'SUBJECT' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Subject' : 'Add New Subject'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveSubject} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Subject Code</label>
                <input
                  type="text"
                  required
                  value={subjectForm.code}
                  onChange={(e) => setSubjectForm({ ...subjectForm, code: e.target.value })}
                  placeholder="e.g. CS501"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Subject Name</label>
                <input
                  type="text"
                  required
                  value={subjectForm.name}
                  onChange={(e) => setSubjectForm({ ...subjectForm, name: e.target.value })}
                  placeholder="e.g. Operating Systems"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Subject Type</label>
                <div className="grid grid-cols-2 gap-3">
                  <label
                    className={`flex items-center justify-center gap-2 p-3 rounded-xl border cursor-pointer font-bold transition-all ${
                      subjectForm.type === 'THEORY'
                        ? 'border-blue-600 bg-blue-50/50 text-blue-700'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    <input
                      type="radio"
                      name="subType"
                      value="THEORY"
                      checked={subjectForm.type === 'THEORY'}
                      onChange={() => setSubjectForm({ ...subjectForm, type: 'THEORY' })}
                      className="sr-only"
                    />
                    <BookOpen className="w-4 h-4" />
                    THEORY
                  </label>

                  <label
                    className={`flex items-center justify-center gap-2 p-3 rounded-xl border cursor-pointer font-bold transition-all ${
                      subjectForm.type === 'LAB'
                        ? 'border-purple-600 bg-purple-50/50 text-purple-700'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    <input
                      type="radio"
                      name="subType"
                      value="LAB"
                      checked={subjectForm.type === 'LAB'}
                      onChange={() => setSubjectForm({ ...subjectForm, type: 'LAB' })}
                      className="sr-only"
                    />
                    <FlaskConical className="w-4 h-4" />
                    LAB
                  </label>
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Credits</label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  required
                  value={subjectForm.credits}
                  onChange={(e) => setSubjectForm({ ...subjectForm, credits: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Subject
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* FACULTY MODAL */}
      {modalType === 'FACULTY' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Faculty Account' : 'Add New Faculty'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveFaculty} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={facultyForm.fullName}
                  onChange={(e) => setFacultyForm({ ...facultyForm, fullName: e.target.value })}
                  placeholder="e.g. Dr. Ravi Kumar"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Employee ID</label>
                <input
                  type="text"
                  required
                  value={facultyForm.employeeId}
                  onChange={(e) => setFacultyForm({ ...facultyForm, employeeId: e.target.value })}
                  placeholder="e.g. FAC-CSE-004"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs text-slate-900"
                />
              </div>

              {!editingItem && (
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Login Username</label>
                  <input
                    type="text"
                    required
                    value={facultyForm.username}
                    onChange={(e) => setFacultyForm({ ...facultyForm, username: e.target.value })}
                    placeholder="e.g. faculty4"
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs text-slate-900"
                  />
                </div>
              )}

              <div>
                <label className="block font-bold text-slate-700 mb-1">
                  {editingItem ? 'New Password (leave blank to keep current)' : 'Account Password'}
                </label>
                <input
                  type="password"
                  required={!editingItem}
                  value={facultyForm.password}
                  onChange={(e) => setFacultyForm({ ...facultyForm, password: e.target.value })}
                  placeholder={editingItem ? '••••••••' : 'Enter password'}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Department</label>
                <select
                  value={facultyForm.departmentId}
                  onChange={(e) => setFacultyForm({ ...facultyForm, departmentId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                >
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.code} — {d.name}
                    </option>
                  ))}
                </select>
              </div>

              {editingItem && (
                <div className="flex items-center gap-2 pt-1">
                  <input
                    type="checkbox"
                    id="facActiveCheck"
                    checked={facultyForm.active}
                    onChange={(e) => setFacultyForm({ ...facultyForm, active: e.target.checked })}
                    className="w-4 h-4 text-indigo-600 rounded"
                  />
                  <label htmlFor="facActiveCheck" className="font-semibold text-slate-700">
                    Active Faculty Account
                  </label>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Faculty
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* LAB BATCH MODAL */}
      {modalType === 'BATCH' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Lab Batch' : 'Create Lab Batch'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveBatch} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Batch Name</label>
                <input
                  type="text"
                  required
                  value={batchForm.name}
                  onChange={(e) => setBatchForm({ ...batchForm, name: e.target.value })}
                  placeholder="e.g. LAB-1"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-bold text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Assigned Section</label>
                <select
                  value={batchForm.sectionId}
                  onChange={(e) => setBatchForm({ ...batchForm, sectionId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                >
                  {sections.map((s) => (
                    <option key={s.id} value={s.id}>
                      Section {s.name} ({s.department?.code})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Batch
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ENROLL STUDENT MODAL — SEARCHABLE SELECTOR */}
      {modalType === 'ENROLL' && (() => {
        const enrolledIds = new Set(enrollments.map((e) => e.student.id));
        const filteredStudents = students.filter((s) => {
          if (!enrollSearchQuery.trim()) return true;
          const q = enrollSearchQuery.toLowerCase().trim();
          return (
            s.name.toLowerCase().includes(q) ||
            s.registrationNumber.toLowerCase().includes(q) ||
            (s.department?.code && s.department.code.toLowerCase().includes(q))
          );
        });

        const selectedStudent = students.find((s) => s.id === enrollForm.studentId);
        const isSelectedEnrolled = selectedStudent ? enrolledIds.has(selectedStudent.id) : false;

        return (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
            <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] flex flex-col">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3 flex-shrink-0">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Enroll Student in {selectedSection?.name}
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Search by student name or registration number
                  </p>
                </div>
                <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                  <X className="w-4 h-4" />
                </button>
              </div>

              <form onSubmit={handleSaveEnrollment} className="space-y-4 text-xs flex flex-col flex-1 overflow-hidden">
                {/* Search Input */}
                <div className="flex-shrink-0">
                  <label className="block font-bold text-slate-700 mb-1">
                    Search Students ({filteredStudents.length} of {students.length})
                  </label>
                  <div className="relative">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      placeholder="Search name (e.g. Rahman) or reg no (e.g. 325106410042)..."
                      value={enrollSearchQuery}
                      onChange={(e) => setEnrollSearchQuery(e.target.value)}
                      className="w-full pl-9 pr-8 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                    {enrollSearchQuery && (
                      <button
                        type="button"
                        onClick={() => setEnrollSearchQuery('')}
                        className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Filtered Student List */}
                <div className="flex-1 overflow-y-auto border border-slate-200 rounded-xl divide-y divide-slate-100 min-h-[180px] max-h-[260px] bg-slate-50/50">
                  {filteredStudents.length === 0 ? (
                    <div className="p-8 text-center text-slate-400 italic">
                      No students matching "{enrollSearchQuery}"
                    </div>
                  ) : (
                    filteredStudents.map((s) => {
                      const isEnrolled = enrolledIds.has(s.id);
                      const isSelected = enrollForm.studentId === s.id;

                      return (
                        <div
                          key={s.id}
                          onClick={() => {
                            if (!isEnrolled) {
                              setEnrollForm({ ...enrollForm, studentId: s.id });
                            }
                          }}
                          className={`p-3 flex items-center justify-between transition-all ${
                            isEnrolled
                              ? 'bg-slate-100/70 opacity-60 cursor-not-allowed'
                              : isSelected
                              ? 'bg-indigo-50/90 border-l-4 border-indigo-600 cursor-pointer'
                              : 'hover:bg-white cursor-pointer'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <input
                              type="radio"
                              name="studentEnrollSelection"
                              disabled={isEnrolled}
                              checked={isSelected}
                              onChange={() => {
                                if (!isEnrolled) {
                                  setEnrollForm({ ...enrollForm, studentId: s.id });
                                }
                              }}
                              className="text-indigo-600 focus:ring-indigo-500 cursor-pointer disabled:cursor-not-allowed"
                            />
                            <div>
                              <div className="font-bold text-slate-900 text-xs flex items-center gap-2">
                                {s.name}
                                <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-slate-200 text-slate-700">
                                  {s.department?.code}
                                </span>
                              </div>
                              <div className="font-mono text-[11px] text-indigo-700 font-bold mt-0.5">
                                {s.registrationNumber}
                              </div>
                            </div>
                          </div>

                          <div>
                            {isEnrolled ? (
                              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-200 text-slate-600">
                                Already Enrolled
                              </span>
                            ) : isSelected ? (
                              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-600 text-white">
                                Selected
                              </span>
                            ) : null}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>

                {/* Selected Student Confirmation & Lab Batch Selection */}
                <div className="flex-shrink-0 space-y-3 pt-2">
                  {selectedStudent ? (
                    <div className="p-2.5 rounded-xl bg-indigo-50/60 border border-indigo-100 text-xs flex items-center justify-between">
                      <div className="text-indigo-950 font-medium truncate">
                        Selected: <strong className="font-bold text-indigo-900">{selectedStudent.name}</strong> ({selectedStudent.registrationNumber})
                      </div>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-200 text-indigo-900">
                        {selectedSection?.name}
                      </span>
                    </div>
                  ) : (
                    <div className="p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs italic">
                      Please select a student above to enroll into {selectedSection?.name}.
                    </div>
                  )}

                  <div>
                    <label className="block font-bold text-slate-700 mb-1">
                      Initial Lab Batch Assignment (Optional)
                    </label>
                    <select
                      value={enrollForm.labBatchId}
                      onChange={(e) => setEnrollForm({ ...enrollForm, labBatchId: Number(e.target.value) })}
                      className="w-full p-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-medium"
                    >
                      <option value={0}>— Unassigned —</option>
                      {batches.map((b) => (
                        <option key={b.id} value={b.id}>
                          {b.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Modal Actions */}
                <div className="flex justify-end gap-2 pt-3 border-t border-slate-100 flex-shrink-0">
                  <button
                    type="button"
                    onClick={() => setModalType(null)}
                    className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={!enrollForm.studentId || isSelectedEnrolled}
                    className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 disabled:hover:bg-indigo-600 text-white font-bold shadow-sm flex items-center gap-1.5"
                  >
                    <UserCheck className="w-3.5 h-3.5" />
                    Complete Enrollment
                  </button>
                </div>
              </form>
            </div>
          </div>
        );
      })()}

      {/* TERM MODAL */}
      {modalType === 'TERM' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Academic Term' : 'Add Academic Term'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveTerm} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Term Name</label>
                <input
                  type="text"
                  required
                  value={termForm.name}
                  onChange={(e) => setTermForm({ ...termForm, name: e.target.value })}
                  placeholder="e.g. Autumn 2026"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Start Date</label>
                  <input
                    type="date"
                    required
                    value={termForm.startDate}
                    onChange={(e) => setTermForm({ ...termForm, startDate: e.target.value })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">End Date</label>
                  <input
                    type="date"
                    required
                    value={termForm.endDate}
                    onChange={(e) => setTermForm({ ...termForm, endDate: e.target.value })}
                    className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-mono"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="termActiveCheck"
                  checked={termForm.active}
                  onChange={(e) => setTermForm({ ...termForm, active: e.target.checked })}
                  className="w-4 h-4 text-indigo-600 rounded"
                />
                <label htmlFor="termActiveCheck" className="font-semibold text-slate-700">
                  Current Active Term
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Term
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DEPARTMENT MODAL */}
      {modalType === 'DEPT' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Department' : 'Add Department'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveDept} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Department Code</label>
                <input
                  type="text"
                  required
                  value={deptForm.code}
                  onChange={(e) => setDeptForm({ ...deptForm, code: e.target.value.toUpperCase() })}
                  placeholder="e.g. CSE"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-mono font-bold text-xs text-slate-900 uppercase"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Department Full Name</label>
                <input
                  type="text"
                  required
                  value={deptForm.name}
                  onChange={(e) => setDeptForm({ ...deptForm, name: e.target.value })}
                  placeholder="e.g. Computer Science and Engineering"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Department
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* SECTION MODAL */}
      {modalType === 'SECTION' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Section' : 'Add Section'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveSection} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Section Name</label>
                <input
                  type="text"
                  required
                  value={sectionForm.name}
                  onChange={(e) => setSectionForm({ ...sectionForm, name: e.target.value })}
                  placeholder="e.g. CSE-A1"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-bold text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Department</label>
                <select
                  value={sectionForm.departmentId}
                  onChange={(e) => setSectionForm({ ...sectionForm, departmentId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                >
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.code} — {d.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Academic Term</label>
                <select
                  value={sectionForm.termId}
                  onChange={(e) => setSectionForm({ ...sectionForm, termId: Number(e.target.value) })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 font-semibold"
                >
                  {terms.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Section
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* HOLIDAY MODAL */}
      {modalType === 'HOLIDAY' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-sm">
                {editingItem ? 'Edit Declared Holiday' : 'Declare New Holiday'}
              </h3>
              <button onClick={() => setModalType(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveHoliday} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Holiday Date</label>
                <input
                  type="date"
                  required
                  value={holidayForm.date}
                  onChange={(e) => setHolidayForm({ ...holidayForm, date: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-bold text-xs text-slate-900"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Holiday Name / Reason</label>
                <input
                  type="text"
                  required
                  value={holidayForm.name}
                  onChange={(e) => setHolidayForm({ ...holidayForm, name: e.target.value })}
                  placeholder="e.g. Mahatma Gandhi Jayanti"
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl font-medium text-xs text-slate-900"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  className="py-2 px-4 rounded-xl text-slate-600 font-semibold hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-sm"
                >
                  Save Holiday
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CONFIRMATION MODAL FOR DELETION */}
      {deleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-sm w-full p-6 space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="font-black text-slate-900 text-base">Confirm Deletion</h3>
              <p className="text-xs text-slate-500 mt-1">
                Are you sure you want to delete/deactivate{' '}
                <span className="font-bold text-slate-800">{deleteConfirm.title}</span>?
              </p>
            </div>

            <div className="flex justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeleteConfirm(null)}
                className="py-2 px-4 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={executeDelete}
                className="py-2 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-sm"
              >
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Schedule Import Modal */}
      <ScheduleImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onSuccess={(result) => {
          setIsImportModalOpen(false);
          showSuccess(
            `Schedule imported successfully! ${result.timetableEntriesCreated} entries, ${result.sectionsCreated} sections created.`
          );
          loadAllData();
        }}
      />
    </div>
  );
};

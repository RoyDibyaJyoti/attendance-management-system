import React, { useState, useEffect } from 'react';
import { facultyAssignmentApi, FacultyAssignmentResponse } from '../../api/facultyAssignmentApi';
import { academicApi } from '../../api/academicApi';
import { facultyApi } from '../../api/facultyApi';
import { useToast } from '../../context/ToastContext';
import { DepartmentResponse, SectionResponse, SubjectResponse, AcademicPeriodResponse } from '../../types/academic';
import { FacultyResponse } from '../../types/faculty';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Plus, Users, Calendar, Briefcase } from 'lucide-react';
import { ApiError } from '../../api/client';

export const AdminFacultyAssignmentsPage: React.FC = () => {
  const { showToast } = useToast();
  const [assignments, setAssignments] = useState<FacultyAssignmentResponse[]>([]);
  
  const [periods, setPeriods] = useState<AcademicPeriodResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [faculty, setFaculty] = useState<FacultyResponse[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [facultyId, setFacultyId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [sectionId, setSectionId] = useState('');
  const [academicPeriodId, setAcademicPeriodId] = useState('');
  const [assignmentStart, setAssignmentStart] = useState(new Date().toISOString().split('T')[0]);
  const [isSaving, setIsSaving] = useState(false);

  const loadData = async (p: number = 0) => {
    try {
      const [assnRes, per, dep, sec, sub, fac] = await Promise.all([
        facultyAssignmentApi.getAssignments(undefined, undefined, p, 10),
        academicApi.getPeriods(),
        academicApi.getDepartments(),
        academicApi.getAllSections(),
        academicApi.getAllSubjects(),
        facultyApi.getAllFaculty()
      ]);
      setAssignments(assnRes.content);
      setPage(assnRes.page);
      setTotalPages(assnRes.totalPages);
      setPeriods(per);
      setDepartments(dep);
      setSections(sec);
      setSubjects(sub);
      setFaculty(fac);
      
      if (per.length > 0 && !academicPeriodId) setAcademicPeriodId(per[0].id);
      if (sec.length > 0 && !sectionId) setSectionId(sec[0].id);
      if (sub.length > 0 && !subjectId) setSubjectId(sub[0].id);
      if (fac.length > 0 && !facultyId) setFacultyId(fac[0].id);
    } catch (err) {
      console.error('Failed to load data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData(0);
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!facultyId || !subjectId || !sectionId || !academicPeriodId || !assignmentStart) return;

    setIsSaving(true);
    try {
      await facultyAssignmentApi.assignFaculty({
        facultyId,
        subjectId,
        sectionId,
        academicPeriodId,
        assignmentStart
      });
      showToast('Faculty assignment created successfully.', 'success');
      setIsModalOpen(false);
      loadData(page);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        showToast(err.message, 'error');
      } else {
        showToast('Failed to assign faculty.', 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleEndAssignment = async (id: string) => {
    const today = new Date().toISOString().split('T')[0];
    try {
      await facultyAssignmentApi.endAssignment(id, today);
      showToast('Assignment ended successfully.', 'success');
      loadData(page);
    } catch (err) {
      showToast('Failed to end assignment.', 'error');
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" label="Loading assignments..." />;
  }

  const facMap = new Map(faculty.map((f) => [f.id, f]));
  const subMap = new Map(subjects.map((s) => [s.id, s]));
  const secMap = new Map(sections.map((s) => [s.id, s]));
  const perMap = new Map(periods.map((p) => [p.id, p]));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Faculty Assignments</h1>
          <p className="text-sm text-slate-500 mt-0.5">Manage temporal teaching assignments for faculty</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
          New Assignment
        </Button>
      </div>

      <Card>
        <div className="divide-y divide-slate-100">
          {assignments.map((assn) => {
            const fac = facMap.get(assn.facultyId);
            const sub = subMap.get(assn.subjectId);
            const sec = secMap.get(assn.sectionId);
            const per = perMap.get(assn.academicPeriodId);

            return (
              <div key={assn.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition-colors">
                <div className="flex items-center gap-3.5">
                  <div className={`w-10 h-10 rounded-full font-bold text-xs flex items-center justify-center shrink-0 ${assn.status === 'INACTIVE' ? 'bg-slate-100 text-slate-400' : 'bg-indigo-50 text-indigo-700'}`}>
                    <Briefcase className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className={`text-base font-bold ${assn.status === 'INACTIVE' ? 'text-slate-500' : 'text-slate-900'}`}>{fac?.name || 'Unknown Faculty'}</h4>
                      <span className={`font-mono text-xs font-semibold px-2 py-0.5 rounded border ${assn.status === 'INACTIVE' ? 'text-slate-500 bg-slate-50 border-slate-200' : 'text-indigo-700 bg-indigo-50 border-indigo-100'}`}>
                        {sub?.code || 'Unknown'} — {sec?.name || 'Unknown'}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-slate-500 mt-0.5">
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {assn.assignmentStart} to {assn.assignmentEnd || 'Present'}
                      </span>
                      <span>Period: <strong className="text-slate-700">{per?.name || 'Unknown'}</strong></span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {assn.status === 'ACTIVE' && (
                    <Button 
                      variant="secondary" 
                      size="sm"
                      onClick={() => handleEndAssignment(assn.id)}
                    >
                      End Assignment
                    </Button>
                  )}
                </div>
              </div>
            );
          })}
          {assignments.length === 0 && (
            <div className="p-8 text-center text-slate-500">
              No faculty assignments found. Create one to get started.
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => loadData(page - 1)}
            >
              Previous
            </Button>
            <span className="text-xs text-slate-500">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => loadData(page + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Assign Faculty"
        subtitle="Create a new teaching assignment"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Select
            label="Faculty Member"
            id="assn-fac"
            options={faculty.filter(f => f.isActive !== false).map((f) => ({ value: f.id, label: `${f.name} (${f.employeeId})` }))}
            value={facultyId}
            onChange={(e) => setFacultyId(e.target.value)}
            required
          />

          <Select
            label="Subject"
            id="assn-sub"
            options={subjects.filter(s => s.isActive !== false).map((s) => ({ value: s.id, label: `${s.code} — ${s.name}` }))}
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            required
          />

          <Select
            label="Section"
            id="assn-sec"
            options={sections.filter(s => s.isActive !== false).map((s) => ({ value: s.id, label: s.name }))}
            value={sectionId}
            onChange={(e) => setSectionId(e.target.value)}
            required
          />

          <Select
            label="Academic Period"
            id="assn-per"
            options={periods.map((p) => ({ value: p.id, label: p.name }))}
            value={academicPeriodId}
            onChange={(e) => setAcademicPeriodId(e.target.value)}
            required
          />
          
          <Input
            label="Start Date"
            id="assn-start"
            type="date"
            value={assignmentStart}
            onChange={(e) => setAssignmentStart(e.target.value)}
            required
          />

          <div className="flex justify-end gap-3 pt-3">
            <Button variant="secondary" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSaving}>
              Create Assignment
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

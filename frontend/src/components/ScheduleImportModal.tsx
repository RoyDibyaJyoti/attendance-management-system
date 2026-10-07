import React, { useState } from 'react';
import {
  apiFetch,
  ScheduleImportRequest,
  ImportPreviewResponse,
  ImportCommitResponse,
} from '../api';
import {
  X,
  Copy,
  Check,
  FileText,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  Upload,
  UserCheck,
  Layers,
  Sparkles,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (result: ImportCommitResponse) => void;
}

export const AI_IMPORT_PROMPT = `You are an expert academic routine and timetable extractor. Convert the attached university/college timetable PDF or document into the exact JSON schema required by AMCS (Attendance Management & Control System).

CRITICAL INSTRUCTION:
Output ONLY raw, valid JSON matching the schema below. Do not wrap in markdown codeblocks if possible, or use standard \`\`\`json. Do not include introductory or explanatory conversational text.

RULES & CONSTRAINTS:
1. Do not invent missing information. Use null where the source does not provide a value.
2. Do not merge different sections. Different sections (e.g. CSE-A1, CSE-A2, IT-A1) have different routines.
3. Do not assume all sections share the same timetable.
4. Combined Lab Slots: If multiple lab batches (e.g., LAB-1, LAB-2, LAB-3) take different subjects in the same time slot, output each as an individual entry in "timetableEntries" with its own "subject", "faculty", and "labBatch".
5. Days of Week: Must be one of ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"].
6. Time Format: Must be 24-hour "HH:mm" (e.g. "09:00", "10:40", "14:20").
7. Subject Types: Must be either "THEORY" or "LAB".
8. Faculty: Provide full name and department. Provide employeeId if mentioned or construct a standard ID (e.g. "FAC-CSE-001").

JSON SCHEMA SPECIFICATION:
{
  "academicTerm": {
    "name": "2026-27 ODD",
    "semester": "ODD",
    "startDate": "2026-07-01",
    "endDate": "2026-12-31"
  },
  "departments": [
    { "code": "CSE", "name": "Computer Science and Engineering" },
    { "code": "IT", "name": "Information Technology" }
  ],
  "sections": [
    { "name": "CSE-A1", "department": "CSE", "year": 2 },
    { "name": "CSE-A2", "department": "CSE", "year": 2 },
    { "name": "IT-A1", "department": "IT", "year": 2 }
  ],
  "faculty": [
    { "name": "Dr. Anitha Reddy", "employeeId": "FAC-CSE-007", "department": "CSE" },
    { "name": "Prof. Rajesh Sharma", "employeeId": "FAC-CSE-008", "department": "CSE" }
  ],
  "subjects": [
    { "code": "CS201", "name": "Operating Systems", "type": "THEORY", "credits": 3 },
    { "code": "CS201L", "name": "Operating Systems Lab", "type": "LAB", "credits": 2 }
  ],
  "labBatches": [
    { "name": "LAB-1", "section": "CSE-A1" },
    { "name": "LAB-2", "section": "CSE-A1" }
  ],
  "timetableEntries": [
    {
      "section": "CSE-A1",
      "subject": "CS201",
      "faculty": "FAC-CSE-007",
      "day": "MONDAY",
      "startTime": "09:00",
      "endTime": "10:40",
      "room": "LH-101",
      "labBatch": null
    },
    {
      "section": "CSE-A1",
      "subject": "CS201L",
      "faculty": "FAC-CSE-008",
      "day": "MONDAY",
      "startTime": "11:00",
      "endTime": "12:40",
      "room": "LAB-2",
      "labBatch": "LAB-1"
    }
  ]
}

Now read the attached timetable document and generate the complete JSON.`;

const SAMPLE_JSON = `{
  "academicTerm": {
    "name": "2026-27 ODD",
    "semester": "ODD",
    "startDate": "2026-07-01",
    "endDate": "2026-12-31"
  },
  "departments": [
    { "code": "CSE", "name": "Computer Science and Engineering" },
    { "code": "IT", "name": "Information Technology" }
  ],
  "sections": [
    { "name": "CSE-A1", "department": "CSE", "year": 2 },
    { "name": "CSE-A2", "department": "CSE", "year": 2 },
    { "name": "IT-A1", "department": "IT", "year": 2 }
  ],
  "faculty": [
    { "name": "Dr. Anitha Reddy", "employeeId": "FAC-CSE-007", "department": "CSE" },
    { "name": "Prof. K. Ramesh", "employeeId": "FAC-IT-002", "department": "IT" }
  ],
  "subjects": [
    { "code": "CS201", "name": "Operating Systems", "type": "THEORY", "credits": 3 },
    { "code": "IT201", "name": "Web Technologies", "type": "THEORY", "credits": 3 },
    { "code": "CS201L", "name": "Operating Systems Lab", "type": "LAB", "credits": 2 }
  ],
  "labBatches": [
    { "name": "LAB-1", "section": "CSE-A1" },
    { "name": "LAB-2", "section": "CSE-A1" }
  ],
  "timetableEntries": [
    {
      "section": "CSE-A1",
      "subject": "CS201",
      "faculty": "Dr. Anitha Reddy",
      "day": "MONDAY",
      "startTime": "09:00",
      "endTime": "10:40",
      "room": "LH-101"
    },
    {
      "section": "CSE-A2",
      "subject": "CS201",
      "faculty": "Dr. Anitha Reddy",
      "day": "MONDAY",
      "startTime": "11:00",
      "endTime": "12:40",
      "room": "LH-102"
    },
    {
      "section": "IT-A1",
      "subject": "IT201",
      "faculty": "Prof. K. Ramesh",
      "day": "MONDAY",
      "startTime": "09:00",
      "endTime": "10:40",
      "room": "IT-201"
    }
  ]
}`;

export const ScheduleImportModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [tab, setTab] = useState<'PROMPT' | 'INPUT' | 'PREVIEW'>('PROMPT');
  const [copied, setCopied] = useState(false);
  const [jsonText, setJsonText] = useState('');
  const [jsonError, setJsonError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [previewData, setPreviewData] = useState<ImportPreviewResponse | null>(null);
  const [commitResult, setCommitResult] = useState<ImportCommitResponse | null>(null);

  if (!isOpen) return null;

  const handleCopyPrompt = () => {
    navigator.clipboard.writeText(AI_IMPORT_PROMPT);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleLoadSample = () => {
    setJsonText(SAMPLE_JSON);
    setJsonError(null);
  };

  const handleValidateAndPreview = async () => {
    setJsonError(null);
    let parsed: ScheduleImportRequest;
    try {
      parsed = JSON.parse(jsonText);
    } catch (e: any) {
      setJsonError('Invalid JSON format: ' + e.message);
      return;
    }

    setLoading(true);
    try {
      const preview = await apiFetch<ImportPreviewResponse>('/admin/import/preview', {
        method: 'POST',
        body: JSON.stringify(parsed),
      });
      setPreviewData(preview);
      setTab('PREVIEW');
    } catch (e: any) {
      setJsonError('Validation error: ' + e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!previewData || !previewData.valid) return;
    setLoading(true);
    try {
      const parsed = JSON.parse(jsonText);
      const res = await apiFetch<ImportCommitResponse>('/admin/import/commit', {
        method: 'POST',
        body: JSON.stringify(parsed),
      });
      setCommitResult(res);
      onSuccess(res);
    } catch (e: any) {
      setJsonError('Import failed: ' + e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      setJsonText(content);
      setJsonError(null);
    };
    reader.readAsText(file);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[92vh] flex flex-col border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-slate-50 to-indigo-50/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-100">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 tracking-tight">Import Academic Schedule</h2>
              <p className="text-xs text-slate-500">AI-assisted routine extraction & multi-section transactional import</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="px-6 pt-3 border-b border-slate-200 bg-slate-50/50 flex gap-2">
          <button
            onClick={() => setTab('PROMPT')}
            className={`px-4 py-2.5 text-xs font-semibold rounded-t-lg transition-all flex items-center gap-2 border-b-2 ${
              tab === 'PROMPT'
                ? 'border-indigo-600 text-indigo-700 bg-white shadow-sm'
                : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <FileText className="w-4 h-4" />
            1. AI Prompt Instructions
          </button>

          <button
            onClick={() => setTab('INPUT')}
            className={`px-4 py-2.5 text-xs font-semibold rounded-t-lg transition-all flex items-center gap-2 border-b-2 ${
              tab === 'INPUT'
                ? 'border-indigo-600 text-indigo-700 bg-white shadow-sm'
                : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Upload className="w-4 h-4" />
            2. Paste / Upload JSON
          </button>

          <button
            onClick={() => {
              if (previewData) setTab('PREVIEW');
            }}
            disabled={!previewData}
            className={`px-4 py-2.5 text-xs font-semibold rounded-t-lg transition-all flex items-center gap-2 border-b-2 ${
              tab === 'PREVIEW'
                ? 'border-indigo-600 text-indigo-700 bg-white shadow-sm'
                : !previewData
                ? 'border-transparent text-slate-400 cursor-not-allowed opacity-60'
                : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Layers className="w-4 h-4" />
            3. Preview & Commit
            {previewData && (
              <span
                className={`ml-1 text-[10px] px-1.5 py-0.5 rounded font-bold ${
                  previewData.valid ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                }`}
              >
                {previewData.valid ? 'Valid' : 'Errors'}
              </span>
            )}
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6">
          {commitResult ? (
            /* Success State */
            <div className="py-8 text-center space-y-4">
              <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-bold text-slate-900">Schedule Imported Successfully!</h3>
              <p className="text-sm text-slate-600 max-w-md mx-auto">{commitResult.message}</p>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-w-lg mx-auto pt-2 text-left">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="text-xs text-slate-500">Sections Created</div>
                  <div className="text-lg font-bold text-slate-900">{commitResult.sectionsCreated}</div>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="text-xs text-slate-500">Faculty Profiles</div>
                  <div className="text-lg font-bold text-slate-900">{commitResult.facultyProfilesCreated}</div>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="text-xs text-slate-500">Subjects Created</div>
                  <div className="text-lg font-bold text-slate-900">{commitResult.subjectsCreated}</div>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="text-xs text-slate-500">Lab Batches</div>
                  <div className="text-lg font-bold text-slate-900">{commitResult.labBatchesCreated}</div>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 col-span-2 sm:col-span-2">
                  <div className="text-xs text-slate-500">Timetable Entries</div>
                  <div className="text-lg font-bold text-indigo-600">{commitResult.timetableEntriesCreated}</div>
                </div>
              </div>

              <div className="pt-4">
                <button
                  onClick={onClose}
                  className="px-6 py-2.5 bg-indigo-600 text-white rounded-xl text-sm font-semibold hover:bg-indigo-700 transition shadow-sm"
                >
                  Done & Refresh Data
                </button>
              </div>
            </div>
          ) : tab === 'PROMPT' ? (
            /* Tab 1: AI Prompt Guide */
            <div className="space-y-4">
              <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-4 flex items-start gap-3">
                <Sparkles className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
                <div className="text-xs text-indigo-900 leading-relaxed">
                  <strong className="font-semibold">How to use AI Timetable Extraction:</strong>
                  <ol className="list-decimal ml-4 mt-1 space-y-1">
                    <li>Click <strong>"Copy AI Prompt"</strong> below to copy our controlled JSON schema prompt.</li>
                    <li>Open <strong>ChatGPT</strong> (GPT-4 / Claude / Gemini) and paste the prompt.</li>
                    <li>Attach your university routine PDF, image, or scan.</li>
                    <li>Copy the resulting JSON response and paste it into Step 2 here.</li>
                  </ol>
                </div>
              </div>

              <div className="relative">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Controlled AI Prompt (Copy & Provide to LLM)
                  </span>
                  <button
                    onClick={handleCopyPrompt}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-indigo-600 text-white text-xs font-semibold rounded-lg hover:bg-indigo-700 transition shadow-sm"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-300" /> : <Copy className="w-3.5 h-3.5" />}
                    {copied ? 'Copied Prompt!' : 'Copy AI Prompt'}
                  </button>
                </div>

                <div className="bg-slate-900 text-slate-100 p-4 rounded-xl font-mono text-xs overflow-x-auto max-h-80 leading-relaxed border border-slate-800">
                  <pre className="whitespace-pre-wrap">{AI_IMPORT_PROMPT}</pre>
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <button
                  onClick={() => setTab('INPUT')}
                  className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-semibold hover:bg-indigo-700 transition shadow-sm"
                >
                  Next: Paste JSON
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : tab === 'INPUT' ? (
            /* Tab 2: Input JSON */
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Paste Generated JSON</h3>
                  <p className="text-xs text-slate-500">Paste raw JSON returned by ChatGPT/Gemini or load sample data</p>
                </div>
                <div className="flex items-center gap-2">
                  <label className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 text-slate-700 text-xs font-medium rounded-lg hover:bg-slate-200 cursor-pointer transition">
                    <Upload className="w-3.5 h-3.5" />
                    Upload .json
                    <input type="file" accept=".json" onChange={handleFileUpload} className="hidden" />
                  </label>
                  <button
                    onClick={handleLoadSample}
                    className="px-3 py-1.5 bg-slate-100 text-indigo-700 text-xs font-medium rounded-lg hover:bg-indigo-50 border border-indigo-200 transition"
                  >
                    Load Sample Routine
                  </button>
                  {jsonText && (
                    <button
                      onClick={() => setJsonText('')}
                      className="px-2.5 py-1.5 text-xs text-rose-600 hover:bg-rose-50 rounded-lg transition"
                    >
                      Clear
                    </button>
                  )}
                </div>
              </div>

              {jsonError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 text-rose-500 shrink-0 mt-0.5" />
                  <div className="font-medium whitespace-pre-wrap">{jsonError}</div>
                </div>
              )}

              <textarea
                value={jsonText}
                onChange={(e) => setJsonText(e.target.value)}
                placeholder="Paste JSON here (must include academicTerm, sections, faculty, subjects, timetableEntries)..."
                rows={14}
                className="w-full font-mono text-xs p-4 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-slate-50/30 text-slate-800 leading-relaxed outline-none"
              />

              <div className="flex items-center justify-between pt-2">
                <button
                  onClick={() => setTab('PROMPT')}
                  className="px-4 py-2 border border-slate-200 text-slate-600 text-xs font-medium rounded-xl hover:bg-slate-50 transition"
                >
                  Back to Prompt Guide
                </button>

                <button
                  onClick={handleValidateAndPreview}
                  disabled={!jsonText.trim() || loading}
                  className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 text-white rounded-xl text-xs font-semibold hover:bg-indigo-700 transition disabled:opacity-50 shadow-sm"
                >
                  {loading && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                  Validate & Preview Import
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            /* Tab 3: Preview & Commit */
            previewData && (
              <div className="space-y-5">
                {/* Validation Status Banner */}
                <div
                  className={`p-4 rounded-xl border flex items-start gap-3 ${
                    previewData.valid
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                      : 'bg-rose-50 border-rose-200 text-rose-900'
                  }`}
                >
                  {previewData.valid ? (
                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
                  )}
                  <div>
                    <h4 className="text-sm font-bold">
                      {previewData.valid ? 'Import Validation Passed' : 'Validation Failed — Issues Must Be Resolved'}
                    </h4>
                    <p className="text-xs opacity-90 mt-0.5">
                      {previewData.valid
                        ? 'All sections, subjects, faculty schedules, and time constraints are valid and ready to commit.'
                        : 'Please address the errors below before committing the timetable.'}
                    </p>
                  </div>
                </div>

                {/* Entity Summary Cards */}
                <div>
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">Import Summary</h4>
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                      <div className="text-xs text-slate-500">Sections</div>
                      <div className="text-lg font-bold text-slate-900">{previewData.sectionsCount}</div>
                    </div>
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                      <div className="text-xs text-slate-500">Faculty</div>
                      <div className="text-lg font-bold text-slate-900">{previewData.facultyCount}</div>
                    </div>
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                      <div className="text-xs text-slate-500">Subjects</div>
                      <div className="text-lg font-bold text-slate-900">{previewData.subjectsCount}</div>
                    </div>
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                      <div className="text-xs text-slate-500">Lab Batches</div>
                      <div className="text-lg font-bold text-slate-900">{previewData.labBatchesCount}</div>
                    </div>
                    <div className="bg-indigo-50/70 p-3 rounded-xl border border-indigo-200 col-span-2 sm:col-span-1">
                      <div className="text-xs text-indigo-700 font-medium">Timetable Entries</div>
                      <div className="text-lg font-bold text-indigo-700">{previewData.timetableEntriesCount}</div>
                    </div>
                  </div>
                </div>

                {/* Auto-Created Faculty Profiles Notice */}
                {previewData.autoCreatedFaculty && previewData.autoCreatedFaculty.length > 0 && (
                  <div className="p-4 bg-sky-50 border border-sky-200 rounded-xl space-y-2">
                    <div className="flex items-center gap-2 text-xs font-bold text-sky-900">
                      <UserCheck className="w-4 h-4 text-sky-600" />
                      Auto-Created Faculty Profiles ({previewData.autoCreatedFaculty.length})
                    </div>
                    <p className="text-xs text-sky-800">
                      The following faculty members do not exist in the database and will have profiles created automatically.
                      Their login accounts are locked (<span className="font-semibold">inactive = false</span>) until an administrator assigns credentials.
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                      {previewData.autoCreatedFaculty.map((f, i) => (
                        <div key={i} className="bg-white p-2.5 rounded-lg border border-sky-100 flex items-center justify-between text-xs">
                          <div>
                            <div className="font-semibold text-slate-900">{f.name}</div>
                            <div className="text-[11px] text-slate-500">Dept: {f.department} • Emp ID: {f.employeeId}</div>
                          </div>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-100 text-amber-800">
                            Locked / Action Req
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Errors List */}
                {previewData.errors && previewData.errors.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-xs font-bold text-rose-700 uppercase tracking-wider flex items-center gap-1.5">
                      <AlertCircle className="w-4 h-4" />
                      Validation Errors ({previewData.errors.length})
                    </div>
                    <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 space-y-1.5 max-h-40 overflow-y-auto">
                      {previewData.errors.map((err, i) => (
                        <div key={i} className="text-xs text-rose-800 flex items-start gap-2">
                          <span className="font-mono text-[10px] bg-rose-200 px-1 rounded text-rose-900 shrink-0">ERR</span>
                          <span>{err}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Warnings List */}
                {previewData.warnings && previewData.warnings.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-xs font-bold text-amber-700 uppercase tracking-wider flex items-center gap-1.5">
                      <AlertTriangle className="w-4 h-4" />
                      Import Warnings ({previewData.warnings.length})
                    </div>
                    <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 space-y-1.5 max-h-40 overflow-y-auto">
                      {previewData.warnings.map((warn, i) => (
                        <div key={i} className="text-xs text-amber-900 flex items-start gap-2">
                          <span className="font-mono text-[10px] bg-amber-200 px-1 rounded text-amber-900 shrink-0">WARN</span>
                          <span>{warn}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Footer Controls */}
                <div className="flex items-center justify-between pt-4 border-t border-slate-100">
                  <button
                    onClick={() => setTab('INPUT')}
                    className="px-4 py-2 border border-slate-200 text-slate-600 text-xs font-medium rounded-xl hover:bg-slate-50 transition"
                  >
                    Back to Edit JSON
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={onClose}
                      className="px-4 py-2 text-xs text-slate-500 hover:text-slate-700 transition"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleCommit}
                      disabled={!previewData.valid || loading}
                      className="inline-flex items-center gap-2 px-6 py-2.5 bg-indigo-600 text-white rounded-xl text-xs font-semibold hover:bg-indigo-700 transition disabled:opacity-50 shadow-sm"
                    >
                      {loading && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                      Confirm & Commit Import Transactionally
                    </button>
                  </div>
                </div>
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
};

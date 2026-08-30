# AMCS REST API Design Specification

**Base Path:** `/api/v1`  
**API Protocol:** HTTP/1.1 & HTTP/2 over TLS  
**Payload Format:** `application/json; charset=utf-8`  
**OpenAPI Specification:** Swagger UI at `/swagger-ui.html`, OpenAPI 3 JSON at `/v3/api-docs`

---

## 1. Design Principles

1. **Explicit Business Operations:** Rather than generic HTTP `PUT` updates on entities, domain state transitions are represented as dedicated operations (e.g. `/sessions/{id}/attendance`, `/sessions/{id}/cancel`, `/attendance/records/{id}/correction`).
2. **DTO Boundary Enforcement:** No JPA entities, Hibernate proxies, or internal database metadata are ever exposed. Request and Response DTOs form an immutable contract.
3. **Pure Domain Delegation:** Calculation and prediction endpoints invoke the pure `AttendanceCalculationEngine`. Zero mathematical logic is present in controllers or repositories.
4. **Idempotency & Concurrency:** Session mutations are protected by optimistic locking (`@Version` $\to$ HTTP 409). Attendance submissions are protected by database unique constraints (`uq_attendance_session_student`) and deterministic re-submission verification.

---

## 2. API Endpoint Catalog

### 2.1 Student Management
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/students` | Register a new student | `CreateStudentRequest` | `StudentResponse` | 201 |
| `GET` | `/api/v1/students/{id}` | Get student by UUID | — | `StudentResponse` | 200 / 404 |
| `GET` | `/api/v1/students/registration/{regNo}` | Get student by registration number | — | `StudentResponse` | 200 / 404 |
| `GET` | `/api/v1/students` | Paginated search of students | Query params (`page`, `size`, `deptId`) | `PagedResponse<StudentResponse>` | 200 |
| `PATCH` | `/api/v1/students/{id}` | Update student contact details | `UpdateStudentRequest` | `StudentResponse` | 200 / 404 |

### 2.2 Academic Structure & Curriculum
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/academic/departments` | Create academic department | `CreateDepartmentRequest` | `DepartmentResponse` | 201 / 409 |
| `GET` | `/api/v1/academic/departments` | List all departments | — | `List<DepartmentResponse>` | 200 |
| `POST` | `/api/v1/academic/periods` | Create semester/academic period | `CreateAcademicPeriodRequest` | `AcademicPeriodResponse` | 201 / 409 |
| `GET` | `/api/v1/academic/periods` | List academic periods | — | `List<AcademicPeriodResponse>` | 200 |
| `POST` | `/api/v1/academic/sections` | Create class section | `CreateSectionRequest` | `SectionResponse` | 201 / 409 |
| `GET` | `/api/v1/academic/sections` | List sections by period | Query param (`periodId`) | `List<SectionResponse>` | 200 |
| `POST` | `/api/v1/academic/subjects` | Create subject/course | `CreateSubjectRequest` | `SubjectResponse` | 201 / 409 |
| `GET` | `/api/v1/academic/subjects` | List subjects | Query params (`deptId`, `type`) | `PagedResponse<SubjectResponse>` | 200 |

### 2.3 Faculty Management
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/faculty` | Create faculty member | `CreateFacultyRequest` | `FacultyResponse` | 201 / 409 |
| `GET` | `/api/v1/faculty/{id}` | Get faculty by ID | — | `FacultyResponse` | 200 / 404 |
| `GET` | `/api/v1/faculty` | List faculty members | Query param (`deptId`) | `PagedResponse<FacultyResponse>` | 200 |

### 2.4 Enrollments & Lab Cohorts
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/enrollments` | Enroll student in section | `EnrollStudentRequest` | `EnrollmentResponse` | 201 / 422 |
| `POST` | `/api/v1/enrollments/transfer` | Transfer student to new section | `TransferStudentRequest` | `EnrollmentResponse` | 200 / 422 |
| `GET` | `/api/v1/students/{id}/enrollments` | View student enrollment history | — | `List<EnrollmentResponse>` | 200 |
| `POST` | `/api/v1/lab-groups` | Create lab group for section | `CreateLabGroupRequest` | `LabGroupResponse` | 201 / 409 |
| `POST` | `/api/v1/lab-groups/assign` | Assign student to lab group | `AssignLabGroupRequest` | `LabGroupMembershipResponse` | 201 / 422 |

### 2.5 Sessions & Timetabling
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/sessions` | Schedule a new session | `CreateSessionRequest` | `SessionResponse` | 201 / 422 |
| `GET` | `/api/v1/sessions/{id}` | Get session details | — | `SessionResponse` | 200 / 404 |
| `GET` | `/api/v1/sessions` | Query sessions (paginated) | Query (`subjectId`, `sectionId`, `date`, `status`) | `PagedResponse<SessionResponse>` | 200 |
| `POST` | `/api/v1/sessions/{id}/cancel` | Cancel scheduled session | `CancelSessionRequest` | `SessionResponse` | 200 / 409 / 422 |
| `POST` | `/api/v1/sessions/{id}/reschedule`| Reschedule to replacement slot | `RescheduleSessionRequest` | `SessionResponse` | 200 / 409 / 422 |

### 2.6 Attendance Recording & Correction (Core Business Operations)
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/sessions/{id}/attendance` | Atomic roll-call submission | `RecordAttendanceBatchRequest` | `SessionAttendanceSummaryResponse` | 200 / 201 / 409 / 422 |
| `GET` | `/api/v1/sessions/{id}/attendance` | View session roll-call sheet | — | `SessionAttendanceSummaryResponse` | 200 / 404 |
| `POST` | `/api/v1/attendance/records/{id}/correction` | Audited attendance correction | `CorrectAttendanceRecordRequest` | `AttendanceCorrectionResponse` | 200 / 404 / 409 / 422 |
| `GET` | `/api/v1/students/{id}/attendance-records` | Student raw attendance log | Query (`subjectId`, `startDate`, `endDate`) | `PagedResponse<AttendanceRecordResponse>` | 200 |

### 2.7 Attendance Calculations & Analytics (Pure Engine Invocations)
| Method | Endpoint | Description | Query Parameters | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `GET` | `/api/v1/students/{id}/attendance/subjects/{subjectId}` | Calculate student attendance for subject | `policyId` (optional override) | `SubjectAttendanceSummaryResponse` | 200 / 404 / 422 |
| `GET` | `/api/v1/students/{id}/attendance/summary` | Calculate attendance across all enrolled subjects | `periodId` (required) | `StudentAttendanceOverviewResponse` | 200 / 404 / 422 |
| `GET` | `/api/v1/students/{id}/attendance/overall` | Calculate institutional aggregate attendance | `periodId`, `strategy` (optional) | `OverallAttendanceSummaryResponse` | 200 / 404 / 422 |
| `GET` | `/api/v1/students/{id}/attendance/subjects/{subjectId}/shortage` | Shortage analysis & predictive projections ($x_{\min}, y_{\max}$) | `remainingUnits` (optional) | `ShortageProjectionResponse` | 200 / 404 / 422 |

### 2.8 Policy Management
| Method | Endpoint | Description | Request DTO | Response DTO | HTTP Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/policies` | Publish a new policy version | `CreateAttendancePolicyRequest` | `AttendancePolicyResponse` | 201 / 409 / 422 |
| `GET` | `/api/v1/policies/{name}/versions` | List all historical versions of a policy | — | `List<AttendancePolicyResponse>` | 200 |
| `GET` | `/api/v1/policies/{id}` | Get specific policy snapshot by ID | — | `AttendancePolicyResponse` | 200 / 404 |

---

## 3. Sample Payloads

### 3.1 Attendance Roll-Call Batch Submission (`POST /api/v1/sessions/{id}/attendance`)
```json
{
  "records": [
    { "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "status": "PRESENT" },
    { "studentId": "7b134d12-1111-4562-b3fc-2c963f66afa7", "status": "ABSENT" },
    { "studentId": "8c245e23-2222-4562-b3fc-2c963f66afa8", "status": "DUTY_LEAVE" }
  ]
}
```

### 3.2 Subject Attendance Calculation Response
```json
{
  "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "subjectId": "9d356f34-3333-4562-b3fc-2c963f66afa9",
  "subjectCode": "CS201",
  "subjectName": "Data Structures & Algorithms",
  "policyName": "Standard Institutional Policy",
  "policyVersion": 1,
  "thresholdPercentage": 75.00,
  "conductedUnits": 40.00,
  "attendedUnits": 32.00,
  "attendancePercentage": 80.00,
  "classification": "ADEQUATE",
  "isAdequate": true,
  "isShortage": false,
  "shortageUnits": 0.00,
  "surplusUnits": 2.00,
  "missingRecordCount": 0,
  "isIncomplete": false
}
```

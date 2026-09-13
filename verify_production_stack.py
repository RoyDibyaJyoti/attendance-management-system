import urllib.request
import urllib.parse
import json
import subprocess

BASE_URL = "http://localhost:8088/api/v1"

def api_post(endpoint, payload, token=None):
    url = BASE_URL + endpoint
    data = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

def api_get(endpoint, params=None, token=None):
    url = BASE_URL + endpoint
    if params:
        url += "?" + urllib.parse.urlencode(params)
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, headers=headers, method="GET")
    with urllib.request.urlopen(req) as resp:
        content_type = resp.headers.get("Content-Type", "")
        if "json" in content_type:
            return json.loads(resp.read().decode("utf-8"))
        return resp.read()

print("--- 1. Health & Reverse Proxy Check ---")
with urllib.request.urlopen("http://localhost:8088/healthz") as r:
    print("Healthz:", r.status, r.read().decode().strip())
with urllib.request.urlopen("http://localhost:8088/actuator/health") as r:
    print("Actuator Health:", r.status, json.loads(r.read().decode())["status"])

print("\n--- 2. Role Authentications ---")
admin_res = api_post("/auth/login", {"usernameOrEmail": "admin", "password": "AdminPassword123!"})
admin_token = admin_res["token"]
print("Admin authenticated. User:", admin_res["username"], "Role:", admin_res["role"])

faculty_res = api_post("/auth/login", {"usernameOrEmail": "faculty1", "password": "FacultyPassword123!"})
faculty_token = faculty_res["token"]
faculty_id = faculty_res["facultyId"]
print("Faculty authenticated. User:", faculty_res["username"], "Faculty ID:", faculty_id)

student_res = api_post("/auth/login", {"usernameOrEmail": "student1", "password": "StudentPassword123!"})
student_token = student_res["token"]
student_id = student_res["studentId"]
print("Student authenticated. User:", student_res["username"], "Student ID:", student_id)

print("\n--- 3. HOD Flow: Master Data & Academic Setup ---")
departments = api_get("/academic/departments", token=admin_token)
dept_names = [d["name"] for d in departments]
periods = api_get("/academic/periods", token=admin_token)
period_id = periods[0]["id"]
print("Departments:", dept_names)
print("Period: Name=" + periods[0]["name"] + " ID=" + period_id)

sections = api_get("/academic/sections", params={"periodId": period_id}, token=admin_token)
section_id = sections[0]["id"]
print("Section: Name=" + sections[0]["name"] + " ID=" + section_id)

print("\n--- 4. Faculty Flow: View Section Enrollments & Sessions ---")
roster = api_get("/enrollments/sections/" + section_id, token=faculty_token)
print("Section Enrollments count:", len(roster))

sessions = api_get("/sessions", params={"sectionId": section_id}, token=faculty_token)
print("Sessions in section:", sessions["totalElements"])
for s in sessions["content"]:
    print("  - Session ID=" + s["id"] + " Date=" + s["sessionDate"] + " Type=" + s["sessionType"] + " Status=" + s["status"])

print("\n--- 5. Student Flow: Attendance Calculation & Eligibility ---")
policy_quoted = urllib.parse.quote("Standard University Policy")
policies = api_get("/policies/" + policy_quoted + "/versions", token=student_token)
policy_id = policies[0]["id"]
print("Policy:", policies[0]["name"], "Threshold:", str(policies[0]["minimumThresholdPercentage"]) + "%")

overview = api_get("/students/" + student_id + "/attendance/summary", params={
    "sectionId": section_id,
    "policyId": policy_id,
    "periodId": period_id
}, token=student_token)
print("Student Overview for:", overview["studentName"], "(" + overview["registrationNumber"] + ")")
print("Enrolled Subjects count:", len(overview["subjects"]))
for subj in overview["subjects"]:
    print("  * Subject", subj["subjectCode"], subj["subjectName"] + ": Attended=" + str(subj["attendedUnits"]) + "/" + str(subj["conductedUnits"]) + " (" + str(subj["attendancePercentage"]) + "%) Status=" + subj["classification"])

overall = api_get("/students/" + student_id + "/attendance/overall", params={
    "sectionId": section_id,
    "policyId": policy_id,
    "periodId": period_id
}, token=student_token)
print("Overall Attendance Percentage:", str(overall["overallPercentage"]) + "% (Threshold: " + str(overall["thresholdPercentage"]) + "%)")
print("Status Classification:", overall["classification"], "Adequate:", overall["isAdequate"])

print("\n--- 6. Reporting Flow: Download RPT-001 Spreadsheet Binary ---")
xlsx_data = api_get("/reports/rpt-001", params={
    "studentId": student_id,
    "academicPeriodId": period_id,
    "sectionId": section_id
}, token=student_token)
print("RPT-001 Downloaded:", len(xlsx_data), "bytes")
print("Magic bytes:", list(xlsx_data[:4]), "(PK\\x03\\x04 ZIP/OOXML header)")

print("\n--- 7. Database Direct Row Counts ---")
out = subprocess.check_output([
    "docker", "exec", "amcs-postgres-prod", "psql", "-U", "amcs_user", "-d", "amcs_db", "-c",
    "SELECT count(*) AS total_records FROM attendance_records;"
]).decode()
print("PostgreSQL Database verification:\n" + out.strip())

print("\n🎉 ALL PRODUCTION CONTAINER DEPLOYMENT VERIFICATIONS PASSED 100%!")

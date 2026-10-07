package com.amcs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static String adminToken;
    private static String facultyToken;
    private static String faculty2Token;
    private static Long theorySessionId;
    private static Long labSessionId;
    private static Long sectionId;
    private static boolean cleaned = false;

    @BeforeEach
    void setupTokens() throws Exception {
        if (!cleaned) {
            jdbcTemplate.execute("DELETE FROM attendance_records");
            jdbcTemplate.execute("DELETE FROM class_sessions");
            jdbcTemplate.execute("DELETE FROM timetable_entries WHERE faculty_id IN (SELECT id FROM faculty WHERE employee_id LIKE 'FAC-TEST%')");
            jdbcTemplate.execute("DELETE FROM faculty WHERE employee_id LIKE 'FAC-TEST%'");
            jdbcTemplate.execute("DELETE FROM users WHERE username LIKE 'proftest%' OR username LIKE 'fac_test%'");
            jdbcTemplate.execute("DELETE FROM lab_batches WHERE name LIKE 'LAB-TEST%'");
            jdbcTemplate.execute("UPDATE timetable_entries SET active = true WHERE id <= 20");
            cleaned = true;
        }
        if (adminToken == null) {
            String adminLogin = """
                {"username": "admin", "password": "password123"}
                """;
            String res = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(adminLogin))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(res);
            adminToken = node.get("token").asText();
        }

        if (facultyToken == null) {
            String facLogin = """
                {"username": "faculty1", "password": "password123"}
                """;
            String res = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(facLogin))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(res);
            facultyToken = node.get("token").asText();
        }

        if (faculty2Token == null) {
            String fac2Login = """
                {"username": "faculty2", "password": "password123"}
                """;
            String res = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fac2Login))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(res);
            faculty2Token = node.get("token").asText();
        }
    }

    @Test
    @Order(1)
    void testAuthMeEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonNode -> {
                    JsonNode node = objectMapper.readTree(jsonNode.getResponse().getContentAsString());
                    assertEquals("admin", node.get("username").asText());
                    assertEquals("ADMIN", node.get("role").asText());
                });

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonNode -> {
                    JsonNode node = objectMapper.readTree(jsonNode.getResponse().getContentAsString());
                    assertEquals("faculty1", node.get("username").asText());
                    assertEquals("FACULTY", node.get("role").asText());
                });
    }

    @Test
    @Order(2)
    void testFacultyTimetableAndGetTheoryTimetableEntry() throws Exception {
        String res = mockMvc.perform(get("/api/v1/timetable/my")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode entries = objectMapper.readTree(res);
        assertTrue(entries.isArray() && entries.size() > 0);

        // Find OS Theory timetable entry (no lab batch)
        Long osTheoryEntryId = null;
        Long osLabBatch1EntryId = null;

        for (JsonNode e : entries) {
            sectionId = e.get("section").get("id").asLong();
            String code = e.get("subject").get("code").asText();
            boolean isLab = e.hasNonNull("labBatch");
            if ("OS".equals(code) && !isLab && "MONDAY".equalsIgnoreCase(e.get("dayOfWeek").asText())) {
                osTheoryEntryId = e.get("id").asLong();
            }
            if ("OS-LAB".equals(code) && isLab && osLabBatch1EntryId == null) {
                osLabBatch1EntryId = e.get("id").asLong();
            }
        }

        assertNotNull(osTheoryEntryId, "OS Theory entry must exist for faculty1");

        // 3. Open a theory session
        LocalDate date = LocalDate.of(2026, 9, 7);
        String createSessionPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "%s"}
            """, osTheoryEntryId, date);

        String sessionRes = mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSessionPayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        JsonNode sessionNode = objectMapper.readTree(sessionRes);
        theorySessionId = sessionNode.get("id").asLong();
        assertEquals("OPEN", sessionNode.get("status").asText());
        assertEquals("2026-09-07", sessionNode.get("sessionDate").asText());
    }

    @Test
    @Order(3)
    void testTheoryRosterHasExactly124StudentsWithoutDuplicates() throws Exception {
        assertNotNull(theorySessionId);

        String rosterRes = mockMvc.perform(get("/api/v1/sessions/" + theorySessionId + "/roster")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rosterNode = objectMapper.readTree(rosterRes);
        assertEquals(124, rosterNode.get("totalStudents").asInt(), "Theory class roster must have exactly 124 students");

        JsonNode students = rosterNode.get("students");
        assertEquals(124, students.size(), "Students list size must be exactly 124");

        // Check for duplicates
        Set<Long> studentIds = new HashSet<>();
        Set<String> regNumbers = new HashSet<>();
        for (JsonNode s : students) {
            long sid = s.get("studentId").asLong();
            String reg = s.get("registrationNumber").asText();
            assertTrue(studentIds.add(sid), "Duplicate studentId found: " + sid);
            assertTrue(regNumbers.add(reg), "Duplicate registrationNumber found: " + reg);
            // Default status must be PRESENT
            assertEquals("PRESENT", s.get("status").asText());
        }

        assertEquals(124, studentIds.size());
    }

    @Test
    @Order(4)
    void testSubmitTheoryAttendanceAndVerifyPersistence() throws Exception {
        assertNotNull(theorySessionId);

        // Fetch roster to get student IDs
        String rosterRes = mockMvc.perform(get("/api/v1/sessions/" + theorySessionId + "/roster")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode roster = objectMapper.readTree(rosterRes);
        JsonNode students = roster.get("students");

        // Mark student index 2, 4, 7 as ABSENT, all others PRESENT
        List<Map<String, Object>> records = new ArrayList<>();
        int absentCount = 0;
        for (int i = 0; i < students.size(); i++) {
            long sid = students.get(i).get("studentId").asLong();
            String status = (i == 2 || i == 4 || i == 7) ? "ABSENT" : "PRESENT";
            if ("ABSENT".equals(status)) absentCount++;
            records.add(Map.of("studentId", sid, "status", status));
        }

        Map<String, Object> payload = Map.of("records", records);
        String submitJson = objectMapper.writeValueAsString(payload);

        // Submit attendance
        mockMvc.perform(post("/api/v1/attendance/sessions/" + theorySessionId + "/submit")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.totalRecords", is(124)))
                .andExpect(jsonPath("$.absentCount", is(absentCount)))
                .andExpect(jsonPath("$.presentCount", is(124 - absentCount)));

        // Verify that submitting AGAIN is rejected (immutable once SUBMITTED)
        mockMvc.perform(post("/api/v1/attendance/sessions/" + theorySessionId + "/submit")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been submitted")));

        // Reload roster and verify persisted statuses
        String reloadRes = mockMvc.perform(get("/api/v1/sessions/" + theorySessionId + "/roster")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode reloadedRoster = objectMapper.readTree(reloadRes);
        assertEquals("SUBMITTED", reloadedRoster.get("status").asText());
        JsonNode reloadedStudents = reloadedRoster.get("students");
        assertEquals("ABSENT", reloadedStudents.get(2).get("status").asText());
        assertEquals("ABSENT", reloadedStudents.get(4).get("status").asText());
        assertEquals("ABSENT", reloadedStudents.get(7).get("status").asText());
        assertEquals("PRESENT", reloadedStudents.get(0).get("status").asText());
        assertEquals("PRESENT", reloadedStudents.get(1).get("status").asText());
    }

    @Test
    @Order(5)
    void testOpenLabSessionAndVerifyOnlyBatchStudentsAppear() throws Exception {
        // Find OS-LAB Batch 1 entry
        String res = mockMvc.perform(get("/api/v1/timetable/my")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode entries = objectMapper.readTree(res);
        Long osLabEntryId = null;
        for (JsonNode e : entries) {
            String code = e.get("subject").get("code").asText();
            if ("OS-LAB".equals(code) && e.hasNonNull("labBatch")) {
                osLabEntryId = e.get("id").asLong();
                break;
            }
        }
        assertNotNull(osLabEntryId, "OS-LAB entry must exist");

        // Open lab session
        LocalDate date = LocalDate.of(2026, 9, 10);
        String createSessionPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "%s"}
            """, osLabEntryId, date);

        String sessionRes = mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSessionPayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        JsonNode sessionNode = objectMapper.readTree(sessionRes);
        labSessionId = sessionNode.get("id").asLong();

        // Get lab session roster
        String rosterRes = mockMvc.perform(get("/api/v1/sessions/" + labSessionId + "/roster")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rosterNode = objectMapper.readTree(rosterRes);
        // Batch 1 has 41 students!
        assertEquals(41, rosterNode.get("totalStudents").asInt(), "Lab Batch 1 must contain exactly 41 students");

        JsonNode labStudents = rosterNode.get("students");
        assertEquals(41, labStudents.size());

        // Verify students 1 to 41 are in Batch 1 and students 42+ are excluded
        for (JsonNode s : labStudents) {
            String reg = s.get("registrationNumber").asText();
            long regNum = Long.parseLong(reg);
            assertTrue(regNum <= 325106410041L, "Student " + reg + " should belong to Batch 1");
            assertEquals("Batch 1", s.get("labBatchName").asText());
        }

        // Submit lab attendance: all PRESENT
        List<Map<String, Object>> records = new ArrayList<>();
        for (JsonNode s : labStudents) {
            records.add(Map.of("studentId", s.get("studentId").asLong(), "status", "PRESENT"));
        }
        String submitJson = objectMapper.writeValueAsString(Map.of("records", records));

        mockMvc.perform(post("/api/v1/attendance/sessions/" + labSessionId + "/submit")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.totalRecords", is(41)));
    }

    @Test
    @Order(6)
    void testAttendanceRegisterNonApplicableLabExcludedFromDenominator() throws Exception {
        assertNotNull(sectionId);

        String regRes = mockMvc.perform(get("/api/v1/attendance/register?sectionId=" + sectionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        JsonNode regNode = objectMapper.readTree(regRes);
        assertEquals(124, regNode.get("students").size());
        assertTrue(regNode.get("totalSessions").asInt() >= 2);

        JsonNode students = regNode.get("students");

        // Student 1 (Batch 1): was eligible for both theory and lab!
        // Should have "P" for theory, "P" for lab -> totalEligible = 2, present = 2, pct = 100.0%
        JsonNode student1 = students.get(0);
        assertEquals("325106410001", student1.get("registrationNumber").asText());
        assertEquals(2, student1.get("totalEligible").asInt());
        assertEquals(2, student1.get("presentCount").asInt());
        assertEquals(100.0, student1.get("percentage").asDouble());

        // Student 3 (index 2): was marked ABSENT for theory, eligible for lab with PRESENT
        // totalEligible = 2, present = 1, absent = 1, pct = 50.0%
        JsonNode student3 = students.get(2);
        assertEquals(2, student3.get("totalEligible").asInt());
        assertEquals(1, student3.get("presentCount").asInt());
        assertEquals(1, student3.get("absentCount").asInt());
        assertEquals(50.0, student3.get("percentage").asDouble());

        // Student 50 (index 49, Batch 2): was eligible for theory (marked PRESENT),
        // but NOT eligible for OS-LAB Batch 1!
        // In OS-LAB Batch 1 column, should have "—" (em dash)!
        // totalEligible = 1 (NOT 2!), present = 1, absent = 0, pct = 100.0%!
        JsonNode student50 = students.get(49);
        assertEquals(1, student50.get("totalEligible").asInt(), "Batch 2 student must have totalEligible=1, excluding lab batch 1");
        assertEquals(1, student50.get("presentCount").asInt());
        assertEquals(100.0, student50.get("percentage").asDouble());
        JsonNode attMap50 = student50.get("attendance");
        assertEquals("—", attMap50.get(String.valueOf(labSessionId)).asText(), "Non-applicable lab cell must be em-dash");
    }

    @Test
    @Order(7)
    void testMonthlySubjectWiseAndOverallSummaries() throws Exception {
        assertNotNull(sectionId);

        // 1. Monthly Summary
        mockMvc.perform(get("/api/v1/attendance/summary/monthly?sectionId=" + sectionId + "&month=9&year=2026")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(124)))
                .andExpect(jsonPath("$.totalSubmittedSessions", greaterThanOrEqualTo(2)));

        // 2. Subject-wise Summary
        mockMvc.perform(get("/api/v1/attendance/summary/subject-wise?sectionId=" + sectionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students", hasSize(124)))
                .andExpect(jsonPath("$.baseSubjectCodes", hasItem("OS")));

        // 3. Overall Summary
        mockMvc.perform(get("/api/v1/attendance/summary/overall?sectionId=" + sectionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents", is(124)))
                .andExpect(jsonPath("$.students", hasSize(124)))
                .andExpect(jsonPath("$.averageAttendancePercentage", greaterThan(0.0)));
    }

    @Test
    @Order(8)
    void testAdminCrudOperations() throws Exception {
        // 1. Create and Update Faculty
        String createFacJson = """
            {
                "employeeId": "FAC-TEST-999",
                "fullName": "Prof. Test Admin",
                "username": "proftest999",
                "password": "password123",
                "departmentId": 1
            }
            """;
        String facRes = mockMvc.perform(post("/api/v1/faculty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFacJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is("FAC-TEST-999")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn().getResponse().getContentAsString();
        Long newFacId = objectMapper.readTree(facRes).get("id").asLong();

        // Update Faculty (edit full name, active status, new employee ID)
        String updateFacJson = """
            {
                "employeeId": "FAC-TEST-999-EDITED",
                "fullName": "Prof. Test Admin Senior",
                "departmentId": 1,
                "active": false
            }
            """;
        mockMvc.perform(put("/api/v1/faculty/" + newFacId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateFacJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is("FAC-TEST-999-EDITED")))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.user.fullName", is("Prof. Test Admin Senior")));

        // 2. Student Update Active Status
        String studentUpdateJson = """
            {
                "registrationNumber": "325106410001",
                "name": "Abhishek Sharma",
                "email": "abhishek.sharma@test.edu",
                "departmentId": 1,
                "active": true
            }
            """;
        mockMvc.perform(put("/api/v1/students/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentUpdateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.email", is("abhishek.sharma@test.edu")));

        // 3. Create, Update and Delete Lab Batch
        String createBatchJson = """
            {"name": "LAB-TEST", "sectionId": 1}
            """;
        String batchRes = mockMvc.perform(post("/api/v1/lab-batches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("LAB-TEST")))
                .andReturn().getResponse().getContentAsString();
        Long newBatchId = objectMapper.readTree(batchRes).get("id").asLong();

        String updateBatchJson = """
            {"name": "LAB-TEST-RENAMED", "sectionId": 1}
            """;
        mockMvc.perform(put("/api/v1/lab-batches/" + newBatchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("LAB-TEST-RENAMED")));

        // 4. Lab Batch assignment & unassignment on Enrollment
        // Unassign batch from student 1 enrollment
        mockMvc.perform(put("/api/v1/enrollments/1/batch")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labBatch").doesNotExist());

        // Reassign back to batch 1
        mockMvc.perform(put("/api/v1/enrollments/1/batch?labBatchId=1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labBatch.id", is(1)));

        // 5. Timetable Validations (Invalid time check & Lab batch requirement check)
        String invalidTimeJson = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 1,
                "dayOfWeek": "MONDAY",
                "startTime": "11:00:00",
                "endTime": "10:00:00",
                "room": "Room 101"
            }
            """;
        mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTimeJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("End time must be after start time")));

        // Lab subject requires lab batch
        // Subject 2 is OS LAB
        String missingBatchLabJson = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 2,
                "facultyId": 1,
                "dayOfWeek": "FRIDAY",
                "startTime": "14:00:00",
                "endTime": "15:40:00",
                "room": "Lab 1"
            }
            """;
        mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingBatchLabJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Lab batch is required for LAB subject")));

        // Valid Timetable Entry Creation (with faculty assignment)
        String validEntryJson = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 1,
                "dayOfWeek": "FRIDAY",
                "startTime": "14:00:00",
                "endTime": "15:40:00",
                "room": "Room 405"
            }
            """;
        String ttRes = mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEntryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.code", is("OS")))
                .andExpect(jsonPath("$.faculty.id", is(1)))
                .andReturn().getResponse().getContentAsString();
        Long newTtId = objectMapper.readTree(ttRes).get("id").asLong();

        // Update timetable entry: reassign faculty!
        String reassignFacultyJson = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 2,
                "dayOfWeek": "FRIDAY",
                "startTime": "14:00:00",
                "endTime": "15:40:00",
                "room": "Room 405"
            }
            """;
        mockMvc.perform(put("/api/v1/timetable/" + newTtId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignFacultyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faculty.id", is(2)));

        // Delete (deactivate) timetable entry
        mockMvc.perform(delete("/api/v1/timetable/" + newTtId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Clean up test batch
        mockMvc.perform(delete("/api/v1/lab-batches/" + newBatchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Clean up test faculty
        mockMvc.perform(delete("/api/v1/faculty/" + newFacId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    void testFacultyAuthorizationStrictness() throws Exception {
        // Find a timetable entry owned by faculty2 (e.g. DMS on Wednesday)
        String ttRes = mockMvc.perform(get("/api/v1/timetable?facultyId=2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode entries = objectMapper.readTree(ttRes);
        assertTrue(entries.size() > 0, "Faculty 2 must have timetable entries");

        Long fac2EntryId = null;
        String scheduledDay = null;
        for (JsonNode e : entries) {
            fac2EntryId = e.get("id").asLong();
            scheduledDay = e.get("dayOfWeek").asText();
            break;
        }

        // 1. Faculty 1 tries to open Faculty 2's timetable entry -> MUST BE FORBIDDEN (403)
        // Pick a valid past date for this day
        LocalDate validDateForFac2 = "WEDNESDAY".equals(scheduledDay)
                ? LocalDate.of(2026, 9, 9)
                : LocalDate.of(2026, 9, 8); // Tuesday

        String openAttemptPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "%s"}
            """, fac2EntryId, validDateForFac2);

        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken) // faculty1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openAttemptPayload))
                .andExpect(status().isForbidden());

        // 2. Faculty 2 opens their own class -> SUCCESS (201 or 200)
        String fac2SessionRes = mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + faculty2Token) // faculty2
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openAttemptPayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        Long fac2SessionId = objectMapper.readTree(fac2SessionRes).get("id").asLong();

        // 3. Faculty 1 tries to access Faculty 2's session details -> FORBIDDEN (403)
        mockMvc.perform(get("/api/v1/sessions/" + fac2SessionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());

        // 4. Faculty 1 tries to access Faculty 2's session roster -> FORBIDDEN (403)
        mockMvc.perform(get("/api/v1/sessions/" + fac2SessionId + "/roster")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());

        // 5. Faculty 1 tries to view Faculty 2's session attendance -> FORBIDDEN (403)
        mockMvc.perform(get("/api/v1/attendance/sessions/" + fac2SessionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());

        // 6. Faculty 1 tries to submit attendance for Faculty 2's session -> FORBIDDEN (403)
        String fakeSubmitPayload = """
            {"records": [{"studentId": 1, "status": "PRESENT"}]}
            """;
        mockMvc.perform(post("/api/v1/attendance/sessions/" + fac2SessionId + "/submit")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fakeSubmitPayload))
                .andExpect(status().isForbidden());

        // 7. Admin CAN access session details and roster
        mockMvc.perform(get("/api/v1/sessions/" + fac2SessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sessions/" + fac2SessionId + "/roster")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    void testTimetableDateAndHolidayRules() throws Exception {
        // Faculty 1's OS Theory entry is scheduled on MONDAY
        String myTtRes = mockMvc.perform(get("/api/v1/timetable/my")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode myEntries = objectMapper.readTree(myTtRes);
        Long osMondayEntryId = null;
        for (JsonNode e : myEntries) {
            if ("MONDAY".equalsIgnoreCase(e.get("dayOfWeek").asText())) {
                osMondayEntryId = e.get("id").asLong();
                break;
            }
        }
        assertNotNull(osMondayEntryId, "Monday timetable entry must exist for faculty1");

        // 1. Wrong weekday: try Tuesday (2026-09-08) for a Monday class -> REJECTED (400)
        String wrongDayPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-08"}
            """, osMondayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongDayPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("scheduled for MONDAY")));

        // 2. Future date: try future Monday (2026-10-12) -> REJECTED (400)
        String futureDatePayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-10-12"}
            """, osMondayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureDatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot create session for future date")));

        // 3. Declare a holiday on Monday 2026-09-21 ("College Foundation Day")
        String createHolidayJson = """
            {"date": "2026-09-21", "name": "College Foundation Day"}
            """;
        String holRes = mockMvc.perform(post("/api/v1/holidays")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createHolidayJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("College Foundation Day")))
                .andReturn().getResponse().getContentAsString();
        Long holidayId = objectMapper.readTree(holRes).get("id").asLong();

        // 4. Attempt to open class on holiday Monday (2026-09-21) -> REJECTED (400)
        String holidaySessionPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-21"}
            """, osMondayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holidaySessionPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("College Foundation Day")));

        // 5. Open class on non-holiday past Monday (2026-09-14) -> ALLOWED (201/200)
        String normalMondayPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-14"}
            """, osMondayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(normalMondayPayload))
                .andExpect(status().is2xxSuccessful());

        // 6. Delete the holiday
        mockMvc.perform(delete("/api/v1/holidays/" + holidayId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 7. Same date (2026-09-21) is now usable!
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holidaySessionPayload))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @Order(11)
    void testSaturdayTimetableEntryAndRules() throws Exception {
        // 1. Admin creates a Saturday timetable entry for Faculty 1
        String saturdayEntryJson = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 1,
                "dayOfWeek": "SATURDAY",
                "startTime": "09:00:00",
                "endTime": "10:40:00",
                "room": "Room 408"
            }
            """;
        String ttRes = mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saturdayEntryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek", is("SATURDAY")))
                .andReturn().getResponse().getContentAsString();
        Long saturdayEntryId = objectMapper.readTree(ttRes).get("id").asLong();

        // 2. Faculty 1 sees the Saturday entry in their timetable
        mockMvc.perform(get("/api/v1/timetable/my")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].dayOfWeek", hasItem("SATURDAY")));

        // 3. Attempting Friday date on Saturday entry -> REJECTED (400)
        String fridayPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-18"}
            """, saturdayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fridayPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("scheduled for SATURDAY")));

        // 4. Attempting Saturday date (2026-09-19) -> ALLOWED (201/200)
        String saturdayPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-19"}
            """, saturdayEntryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saturdayPayload))
                .andExpect(status().is2xxSuccessful());

        // 5. Clean up Saturday entry
        mockMvc.perform(delete("/api/v1/timetable/" + saturdayEntryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}

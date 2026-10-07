package com.amcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase3FeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.amcs.faculty.FacultyRepository facultyRepo;

    @Autowired
    private com.amcs.auth.UserRepository userRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String facultyToken;

    @BeforeEach
    void setup() throws Exception {
        jdbcTemplate.execute("DELETE FROM attendance_records WHERE session_id IN (SELECT id FROM class_sessions WHERE timetable_entry_id IN (SELECT id FROM timetable_entries WHERE faculty_id IN (SELECT id FROM faculty WHERE employee_id LIKE 'FAC-TEST%')))");
        jdbcTemplate.execute("DELETE FROM class_sessions WHERE timetable_entry_id IN (SELECT id FROM timetable_entries WHERE faculty_id IN (SELECT id FROM faculty WHERE employee_id LIKE 'FAC-TEST%'))");
        jdbcTemplate.execute("DELETE FROM timetable_entries WHERE faculty_id IN (SELECT id FROM faculty WHERE employee_id LIKE 'FAC-TEST%') OR room LIKE '%IT%'");
        jdbcTemplate.execute("DELETE FROM faculty WHERE employee_id LIKE 'FAC-TEST%'");
        jdbcTemplate.execute("DELETE FROM users WHERE username LIKE 'fac_test%' OR username LIKE 'proftest%'");
        jdbcTemplate.execute("DELETE FROM lab_batches WHERE name LIKE 'LAB-B%' OR name LIKE 'LAB-TEST%'");
        jdbcTemplate.execute("DELETE FROM subjects WHERE code LIKE 'IT201%'");
        jdbcTemplate.execute("DELETE FROM sections WHERE name = 'IT-A1'");
        jdbcTemplate.execute("DELETE FROM departments WHERE code = 'TEST_IT'");
        jdbcTemplate.execute("DELETE FROM academic_terms WHERE name = 'Fall 2026 Test Term'");

        String adminLogin = """
            {"username": "admin", "password": "password123"}
            """;
        String res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLogin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(res).get("token").asText();

        String facLogin = """
            {"username": "faculty1", "password": "password123"}
            """;
        String res2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facLogin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        facultyToken = objectMapper.readTree(res2).get("token").asText();
    }

    /**
     * Requirement 1: Safari Login CORS Verification
     * Test that 127.0.0.1 and IPv6 origins return HTTP 200 with proper Access-Control-Allow-Origin
     */
    @Test
    void testSafariCorsOriginSupport() throws Exception {
        // 1. Origin http://127.0.0.1:5173 (Safari default)
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "faculty1", "password": "password123"}
                            """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(jsonPath("$.token", notNullValue()));

        // 2. Origin http://localhost:5173 (Chrome default)
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "faculty1", "password": "password123"}
                            """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    /**
     * Requirement 3A: Sunday Timetable Creation Blocked
     */
    @Test
    void testSundayTimetableCreationBlocked() throws Exception {
        String sundayEntry = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 1,
                "dayOfWeek": "SUNDAY",
                "startTime": "10:00:00",
                "endTime": "11:40:00",
                "room": "Room 501"
            }
            """;
        mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sundayEntry))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Sunday is a weekly holiday")));
    }

    /**
     * Requirement 3B: Second Saturday Institutional Holiday Blocks Attendance
     */
    @Test
    void testSecondSaturdayAttendanceBlocked() throws Exception {
        // 1. Create a Saturday timetable entry
        String satEntry = """
            {
                "termId": 1,
                "sectionId": 1,
                "subjectId": 1,
                "facultyId": 1,
                "dayOfWeek": "SATURDAY",
                "startTime": "09:00:00",
                "endTime": "10:40:00",
                "room": "Room 401"
            }
            """;
        String res = mockMvc.perform(post("/api/v1/timetable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(satEntry))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long entryId = objectMapper.readTree(res).get("id").asLong();

        // 2. Attempt session creation on 2026-09-12 (Second Saturday of Sep 2026: 8 <= 12 <= 14)
        // Must be rejected as non-working day (Second Saturday — Institutional Holiday)
        String secondSatPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-12"}
            """, entryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSatPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Second Saturday — Institutional Holiday")));

        // 3. Attempt session creation on 2026-09-05 (1st Saturday) -> Working Saturday!
        String firstSatPayload = String.format("""
            {"timetableEntryId": %d, "sessionDate": "2026-09-05"}
            """, entryId);
        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstSatPayload))
                .andExpect(status().is2xxSuccessful());

        // Clean up
        mockMvc.perform(delete("/api/v1/timetable/" + entryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    /**
     * Requirement 3D: Calendar Day Status Endpoint
     */
    @Test
    void testCalendarStatusEndpoint() throws Exception {
        // Sunday
        mockMvc.perform(get("/api/v1/calendar/status?date=2026-10-04")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonWorkingDay", is(true)))
                .andExpect(jsonPath("$.holidayType", is("WEEKLY")))
                .andExpect(jsonPath("$.reason", containsString("Sunday")));

        // Second Saturday
        mockMvc.perform(get("/api/v1/calendar/status?date=2026-10-10")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonWorkingDay", is(true)))
                .andExpect(jsonPath("$.holidayType", is("INSTITUTIONAL")))
                .andExpect(jsonPath("$.reason", containsString("Second Saturday")));

        // Regular Working Day (Monday 2026-10-05)
        mockMvc.perform(get("/api/v1/calendar/status?date=2026-10-05")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonWorkingDay", is(false)));
    }

    /**
     * Requirement 5-11: Schedule Import Preview & Transactional Commit
     */
    @Test
    void testScheduleImportPreviewAndCommit() throws Exception {
        String importJson = """
            {
                "academicTerm": {
                    "name": "Fall 2026 Test Term"
                },
                "departments": [
                    { "code": "TEST_IT", "name": "Information Technology" }
                ],
                "sections": [
                    { "name": "IT-A1", "departmentCode": "TEST_IT", "year": 2 }
                ],
                "faculty": [
                    {
                        "name": "Dr. Rajesh Sharma",
                        "employeeId": "FAC-TEST-099",
                        "departmentCode": "TEST_IT"
                    }
                ],
                "subjects": [
                    { "code": "IT201", "name": "Data Structures", "type": "THEORY", "credits": 4 },
                    { "code": "IT201-LAB", "name": "Data Structures Lab", "type": "LAB", "credits": 2 }
                ],
                "labBatches": [
                    { "name": "LAB-B1", "sectionName": "IT-A1" },
                    { "name": "LAB-B2", "sectionName": "IT-A1" }
                ],
                "timetableEntries": [
                    {
                        "sectionName": "IT-A1",
                        "subjectCode": "IT201",
                        "facultyEmployeeId": "FAC-TEST-099",
                        "facultyName": "Dr. Rajesh Sharma",
                        "dayOfWeek": "MONDAY",
                        "startTime": "09:00",
                        "endTime": "10:40",
                        "room": "Room IT-101",
                        "labBatchName": null
                    },
                    {
                        "sectionName": "IT-A1",
                        "subjectCode": "IT201-LAB",
                        "facultyEmployeeId": "FAC-TEST-099",
                        "facultyName": "Dr. Rajesh Sharma",
                        "dayOfWeek": "TUESDAY",
                        "startTime": "14:00",
                        "endTime": "15:40",
                        "room": "Lab 2",
                        "labBatchName": "LAB-B1"
                    }
                ]
            }
            """;

        // 1. Preview
        mockMvc.perform(post("/api/v1/admin/import/preview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canCommit", is(true)))
                .andExpect(jsonPath("$.timetableEntryCount", is(2)))
                .andExpect(jsonPath("$.actions", hasItem(containsString("Dr. Rajesh Sharma"))));

        // 2. Commit
        mockMvc.perform(post("/api/v1/admin/import/commit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.createdTimetableEntries", is(2)));

        // 3. Verify auto-created faculty profile has locked login credentials
        var faculty = facultyRepo.findByEmployeeId("FAC-TEST-099");
        assertTrue(faculty.isPresent(), "Auto-created faculty profile should exist");
        assertEquals("Dr. Rajesh Sharma", faculty.get().getUser().getFullName());
        assertFalse(faculty.get().getUser().isActive(), "Auto-created faculty login account must be locked (active=false)");

        // 4. Verify imported timetable entry is queryable
        mockMvc.perform(get("/api/v1/timetable?facultyId=" + faculty.get().getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].subject.code", anyOf(is("IT201"), is("IT201-LAB"))));
    }
}

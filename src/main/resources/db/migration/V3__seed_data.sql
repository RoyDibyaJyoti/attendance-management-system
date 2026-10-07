-- V3: Development seed data
-- This is demo/development data. Do NOT use in production.

-- ============================================================
-- USERS
-- Passwords are BCrypt hashes of 'password123'
-- ============================================================
INSERT INTO users (username, password, role, full_name) VALUES
  ('admin',    '$2a$12$mwuKaeFIbMBiSBgzOo4Aie9E6Gf4.v1R4.k1N7eYkTtWzAnj/dAji', 'ADMIN',   'System Administrator'),
  ('faculty1', '$2a$12$mwuKaeFIbMBiSBgzOo4Aie9E6Gf4.v1R4.k1N7eYkTtWzAnj/dAji', 'FACULTY', 'Dr. Ravi Kumar'),
  ('faculty2', '$2a$12$mwuKaeFIbMBiSBgzOo4Aie9E6Gf4.v1R4.k1N7eYkTtWzAnj/dAji', 'FACULTY', 'Prof. Anitha Reddy');

-- ============================================================
-- ACADEMIC TERM
-- ============================================================
INSERT INTO academic_terms (name, start_date, end_date, active) VALUES
  ('Fall 2026', '2026-07-01', '2026-11-30', TRUE);

-- ============================================================
-- DEPARTMENT
-- ============================================================
INSERT INTO departments (code, name) VALUES
  ('CSE', 'Computer Science and Engineering');

-- ============================================================
-- SECTION
-- ============================================================
INSERT INTO sections (name, department_id, term_id) VALUES
  ('CSE-A1', (SELECT id FROM departments WHERE code='CSE'), (SELECT id FROM academic_terms WHERE name='Fall 2026'));

-- ============================================================
-- SUBJECTS (THEORY + LAB pairs)
-- ============================================================
INSERT INTO subjects (code, name, type, credits) VALUES
  ('OS',       'Operating Systems',                    'THEORY', 3),
  ('OS-LAB',   'Operating Systems Lab',                'LAB',    1),
  ('OOPJ',     'Object Oriented Programming with Java','THEORY', 3),
  ('OOPJ-LAB', 'OOPJ Lab',                             'LAB',    1),
  ('DMS',      'Database Management Systems',          'THEORY', 3),
  ('COA',      'Computer Organization & Architecture', 'THEORY', 3),
  ('COA-LAB',  'Computer Organization Lab',            'LAB',    1),
  ('PSQT',     'Probability, Statistics & Queuing Theory','THEORY', 3);

-- ============================================================
-- FACULTY records
-- ============================================================
INSERT INTO faculty (employee_id, user_id, department_id) VALUES
  ('EMP001', (SELECT id FROM users WHERE username='faculty1'), (SELECT id FROM departments WHERE code='CSE')),
  ('EMP002', (SELECT id FROM users WHERE username='faculty2'), (SELECT id FROM departments WHERE code='CSE'));

-- ============================================================
-- STUDENTS (12 demo students with realistic registration numbers)
-- ============================================================
INSERT INTO students (registration_number, name, email, department_id) VALUES
  ('325106410001', 'RATISH SHARMA',          'ratish@example.edu',     (SELECT id FROM departments WHERE code='CSE')),
  ('325106410002', 'ABDUL REHMAN',           'abdul@example.edu',      (SELECT id FROM departments WHERE code='CSE')),
  ('325106410003', 'RISHITHA AKUMARTHI',     'rishitha@example.edu',   (SELECT id FROM departments WHERE code='CSE')),
  ('325106410004', 'SIREESHA BADANA',        'sireesha@example.edu',   (SELECT id FROM departments WHERE code='CSE')),
  ('325106410005', 'LAKSHMANA KUMAR BATHULA','lakshmana@example.edu',  (SELECT id FROM departments WHERE code='CSE')),
  ('325106410006', 'PADMA KUMARI CHIKKALA', 'padma@example.edu',      (SELECT id FROM departments WHERE code='CSE')),
  ('325106410007', 'RAKESH CHOPPA',          'rakesh@example.edu',     (SELECT id FROM departments WHERE code='CSE')),
  ('325106410008', 'SUBHASH DHULIPUDI',      'subhash@example.edu',    (SELECT id FROM departments WHERE code='CSE')),
  ('325106410009', 'SRAVANI DUMMU',          'sravani@example.edu',    (SELECT id FROM departments WHERE code='CSE')),
  ('325106410010', 'EESHAAN URLAM',          'eeshaan@example.edu',    (SELECT id FROM departments WHERE code='CSE')),
  ('325106410011', 'KAILASH REDDY GADI',     'kailash@example.edu',    (SELECT id FROM departments WHERE code='CSE')),
  ('325106410012', 'DEEPIKA GANTA',          'deepika@example.edu',    (SELECT id FROM departments WHERE code='CSE'));

-- ============================================================
-- LAB BATCHES for CSE-A1
-- ============================================================
INSERT INTO lab_batches (name, section_id) VALUES
  ('Batch 1', (SELECT id FROM sections WHERE name='CSE-A1')),
  ('Batch 2', (SELECT id FROM sections WHERE name='CSE-A1')),
  ('Batch 3', (SELECT id FROM sections WHERE name='CSE-A1'));

-- ============================================================
-- ENROLLMENTS
-- Students 1-4 → Batch 1
-- Students 5-8 → Batch 2
-- Students 9-12 → Batch 3
-- ============================================================
DO $$
DECLARE
  sec_id  BIGINT := (SELECT id FROM sections WHERE name='CSE-A1');
  b1_id   BIGINT := (SELECT id FROM lab_batches WHERE name='Batch 1' AND section_id=sec_id);
  b2_id   BIGINT := (SELECT id FROM lab_batches WHERE name='Batch 2' AND section_id=sec_id);
  b3_id   BIGINT := (SELECT id FROM lab_batches WHERE name='Batch 3' AND section_id=sec_id);
BEGIN
  INSERT INTO enrollments (student_id, section_id, lab_batch_id) VALUES
    ((SELECT id FROM students WHERE registration_number='325106410001'), sec_id, b1_id),
    ((SELECT id FROM students WHERE registration_number='325106410002'), sec_id, b1_id),
    ((SELECT id FROM students WHERE registration_number='325106410003'), sec_id, b1_id),
    ((SELECT id FROM students WHERE registration_number='325106410004'), sec_id, b1_id),
    ((SELECT id FROM students WHERE registration_number='325106410005'), sec_id, b2_id),
    ((SELECT id FROM students WHERE registration_number='325106410006'), sec_id, b2_id),
    ((SELECT id FROM students WHERE registration_number='325106410007'), sec_id, b2_id),
    ((SELECT id FROM students WHERE registration_number='325106410008'), sec_id, b2_id),
    ((SELECT id FROM students WHERE registration_number='325106410009'), sec_id, b3_id),
    ((SELECT id FROM students WHERE registration_number='325106410010'), sec_id, b3_id),
    ((SELECT id FROM students WHERE registration_number='325106410011'), sec_id, b3_id),
    ((SELECT id FROM students WHERE registration_number='325106410012'), sec_id, b3_id);
END $$;

-- ============================================================
-- TIMETABLE ENTRIES
-- faculty1 teaches OS, OOPJ-LAB (B1), COA
-- faculty2 teaches DMS, PSQT, OOPJ, OS-LAB (B2), COA-LAB (B3)
-- ============================================================
DO $$
DECLARE
  term_id    BIGINT := (SELECT id FROM academic_terms WHERE name='Fall 2026');
  sec_id     BIGINT := (SELECT id FROM sections WHERE name='CSE-A1');
  f1         BIGINT := (SELECT id FROM faculty WHERE employee_id='EMP001');
  f2         BIGINT := (SELECT id FROM faculty WHERE employee_id='EMP002');
  s_os       BIGINT := (SELECT id FROM subjects WHERE code='OS');
  s_os_lab   BIGINT := (SELECT id FROM subjects WHERE code='OS-LAB');
  s_oopj     BIGINT := (SELECT id FROM subjects WHERE code='OOPJ');
  s_oopj_lab BIGINT := (SELECT id FROM subjects WHERE code='OOPJ-LAB');
  s_dms      BIGINT := (SELECT id FROM subjects WHERE code='DMS');
  s_coa      BIGINT := (SELECT id FROM subjects WHERE code='COA');
  s_coa_lab  BIGINT := (SELECT id FROM subjects WHERE code='COA-LAB');
  s_psqt     BIGINT := (SELECT id FROM subjects WHERE code='PSQT');
  b1_id      BIGINT := (SELECT lb.id FROM lab_batches lb JOIN sections s ON lb.section_id=s.id WHERE lb.name='Batch 1' AND s.name='CSE-A1');
  b2_id      BIGINT := (SELECT lb.id FROM lab_batches lb JOIN sections s ON lb.section_id=s.id WHERE lb.name='Batch 2' AND s.name='CSE-A1');
  b3_id      BIGINT := (SELECT lb.id FROM lab_batches lb JOIN sections s ON lb.section_id=s.id WHERE lb.name='Batch 3' AND s.name='CSE-A1');
BEGIN
  -- Theory classes (no lab_batch = all students)
  INSERT INTO timetable_entries (term_id, section_id, subject_id, faculty_id, day_of_week, start_time, end_time, room) VALUES
    (term_id, sec_id, s_os,   f1, 'MONDAY',    '09:00', '10:00', 'Room 301'),
    (term_id, sec_id, s_oopj, f1, 'TUESDAY',   '09:00', '10:00', 'Room 301'),
    (term_id, sec_id, s_dms,  f2, 'WEDNESDAY', '09:00', '10:00', 'Room 302'),
    (term_id, sec_id, s_coa,  f1, 'THURSDAY',  '09:00', '10:00', 'Room 301'),
    (term_id, sec_id, s_psqt, f2, 'FRIDAY',    '09:00', '10:00', 'Room 302'),
    (term_id, sec_id, s_os,   f1, 'WEDNESDAY', '11:00', '12:00', 'Room 301'),
    (term_id, sec_id, s_oopj, f1, 'FRIDAY',    '11:00', '12:00', 'Room 301'),
    (term_id, sec_id, s_dms,  f2, 'TUESDAY',   '11:00', '12:00', 'Room 302'),
    (term_id, sec_id, s_coa,  f1, 'MONDAY',    '11:00', '12:00', 'Room 301');

  -- Lab classes — each batch gets its own timetable entry for the lab slot
  -- Thursday 14:00-16:40: OS-LAB Batch 1, OOPJ-LAB Batch 2, COA-LAB Batch 3
  INSERT INTO timetable_entries (term_id, section_id, subject_id, faculty_id, day_of_week, start_time, end_time, room, lab_batch_id) VALUES
    (term_id, sec_id, s_os_lab,   f1, 'THURSDAY', '14:00', '16:40', 'Lab-1', b1_id),
    (term_id, sec_id, s_oopj_lab, f1, 'THURSDAY', '14:00', '16:40', 'Lab-2', b2_id),
    (term_id, sec_id, s_coa_lab,  f2, 'THURSDAY', '14:00', '16:40', 'Lab-3', b3_id),
  -- Friday 14:00-16:40: OOPJ-LAB Batch 1, OS-LAB Batch 2, COA-LAB Batch 1... 
  -- Simplified: COA-LAB Batch 1, OS-LAB Batch 3
    (term_id, sec_id, s_coa_lab,  f2, 'FRIDAY',   '14:00', '16:40', 'Lab-1', b1_id),
    (term_id, sec_id, s_oopj_lab, f1, 'FRIDAY',   '14:00', '16:40', 'Lab-2', b3_id);
END $$;

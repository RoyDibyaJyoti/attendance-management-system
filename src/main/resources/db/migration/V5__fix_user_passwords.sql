-- V5: Fix BCrypt password hashes for seeded users so 'password123' authenticates successfully
UPDATE users SET password = '$2a$12$9f527yGJdyLjpm7Xo.h5S.ksbZDlBe/BKELiPc3izUUu/JbAiLeUK'
WHERE username IN ('admin', 'faculty1', 'faculty2');

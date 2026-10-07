-- ============================================================
-- V6: Holidays Table for Academic Calendar Management
-- ============================================================
CREATE TABLE IF NOT EXISTS holidays (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_holidays_date ON holidays(date);

-- Declared official holidays
INSERT INTO holidays (date, name) VALUES ('2026-10-02', 'Gandhi Jayanti') ON CONFLICT (date) DO NOTHING;

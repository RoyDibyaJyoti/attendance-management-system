-- ============================================================================
-- AMCS Migration: V2__optimistic_locking.sql
-- Adds optimistic locking version column to mutable aggregate roots.
-- Per Phase 3 Architecture Review:
--   - Added to 'sessions' (mutable aggregate root).
--   - NOT added to 'attendance_records' (immutable atomic facts).
-- ============================================================================

ALTER TABLE sessions
ADD COLUMN version INT NOT NULL DEFAULT 0;

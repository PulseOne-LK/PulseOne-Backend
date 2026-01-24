-- ========================================
-- MIGRATION: Dual-Mode Doctor Concept
-- ========================================
-- This migration implements the dual-mode doctor concept:
-- 1. CLINIC WORKFLOW (Physical): Managed by clinic admin, token-based, IN_PERSON
-- 2. DIRECT WORKFLOW (Virtual): Managed by doctor, time-slot based, VIRTUAL with pre-payment
--
-- Date: 2026-01-24
-- ========================================

-- Add creator tracking fields to sessions table
ALTER TABLE sessions 
ADD COLUMN creator_type VARCHAR(20),
ADD COLUMN creator_id VARCHAR(255);

-- Add meeting fields to appointments table for virtual consultations
ALTER TABLE appointments 
ADD COLUMN meeting_link VARCHAR(500),
ADD COLUMN meeting_id VARCHAR(100);

-- Update existing sessions to have a creator type
-- Default existing clinic-based sessions to CLINIC_ADMIN
UPDATE sessions 
SET creator_type = 'CLINIC_ADMIN',
    creator_id = 'LEGACY_MIGRATION'
WHERE clinic_id IS NOT NULL;

-- Default existing virtual sessions without clinic to DOCTOR
UPDATE sessions 
SET creator_type = 'DOCTOR',
    creator_id = doctor_user_id
WHERE clinic_id IS NULL AND service_type = 'VIRTUAL';

-- Add comments for documentation
COMMENT ON COLUMN sessions.creator_type IS 'Who manages this session: CLINIC_ADMIN (physical clinic) or DOCTOR (direct virtual)';
COMMENT ON COLUMN sessions.creator_id IS 'User ID of the creator (clinic admin or doctor)';
COMMENT ON COLUMN appointments.meeting_link IS 'AWS Chime meeting link for VIRTUAL appointments only';
COMMENT ON COLUMN appointments.meeting_id IS 'AWS Chime meeting ID for tracking';

-- Create index for faster queries on creator
CREATE INDEX idx_sessions_creator ON sessions(creator_type, creator_id);

-- Create index for meeting lookups
CREATE INDEX idx_appointments_meeting_id ON appointments(meeting_id) WHERE meeting_id IS NOT NULL;

-- ========================================
-- IMPORTANT NOTES FOR DEVELOPERS:
-- ========================================
-- 1. The ServiceType enum no longer has "BOTH" - sessions are strictly VIRTUAL or IN_PERSON
-- 2. CLINIC_ADMIN can only create IN_PERSON sessions with a clinic reference
-- 3. DOCTOR can only create VIRTUAL sessions without clinic reference  
-- 4. Payment must be verified BEFORE generating meeting links for VIRTUAL appointments
-- 5. Token/queue system is ONLY for IN_PERSON clinic sessions
-- 6. Time slots (not tokens) are used for VIRTUAL direct doctor sessions
-- ========================================

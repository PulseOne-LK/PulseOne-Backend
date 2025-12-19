-- Migration: Add denormalized profile service ID columns
-- This migration adds columns to store the actual profile service IDs alongside foreign keys
-- Reason: Allows easy access to profile service IDs without joining through appointments service auto-generated IDs

-- Add columns to sessions table if they don't exist
ALTER TABLE sessions
ADD COLUMN IF NOT EXISTS doctor_user_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS clinic_profile_id BIGINT;

-- Add constraints and indexes
ALTER TABLE sessions
ADD CONSTRAINT chk_doctor_user_id CHECK (doctor_user_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_sessions_doctor_user_id ON sessions(doctor_user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_clinic_profile_id ON sessions(clinic_profile_id);

-- Add columns to appointments table if they don't exist
ALTER TABLE appointments
ADD COLUMN IF NOT EXISTS clinic_profile_id BIGINT;

-- Add index
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_profile_id ON appointments(clinic_profile_id);

-- Populate doctor_user_id from existing doctor records
UPDATE sessions s
SET doctor_user_id = d.user_id
FROM doctors d
WHERE s.doctor_id = d.id AND s.doctor_user_id IS NULL;

-- Populate clinic_profile_id from existing clinic records
UPDATE sessions s
SET clinic_profile_id = c.profile_clinic_id
FROM clinics c
WHERE s.clinic_id = c.id AND s.clinic_profile_id IS NULL;

-- Populate appointment clinic_profile_id from session
UPDATE appointments a
SET clinic_profile_id = s.clinic_profile_id
FROM sessions s
WHERE a.session_id = s.id AND a.clinic_profile_id IS NULL;

-- Verify the migration
SELECT 
    (SELECT COUNT(*) FROM sessions WHERE doctor_user_id IS NULL) as sessions_missing_doctor_user_id,
    (SELECT COUNT(*) FROM sessions WHERE clinic_id IS NOT NULL AND clinic_profile_id IS NULL) as sessions_missing_clinic_profile_id,
    (SELECT COUNT(*) FROM appointments WHERE clinic_id IS NOT NULL AND clinic_profile_id IS NULL) as appointments_missing_clinic_profile_id;

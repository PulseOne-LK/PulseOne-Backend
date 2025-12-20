-- Migration script to update clinic_doctors table
-- Adds missing columns for clinic confirmation tracking

-- Add new columns to clinic_doctors table if they don't exist
ALTER TABLE clinic_doctors 
ADD COLUMN IF NOT EXISTS is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;

-- Add unique constraint to ensure unique clinic-doctor pairs
-- Note: The existing doctor_uuid column is used for doctor identification
ALTER TABLE clinic_doctors 
DROP CONSTRAINT IF EXISTS uk_clinic_doctor,
ADD CONSTRAINT uk_clinic_doctor UNIQUE (clinic_id, doctor_uuid);

-- Index for faster queries by doctor
CREATE INDEX IF NOT EXISTS idx_clinic_doctor_uuid ON clinic_doctors(doctor_uuid);
CREATE INDEX IF NOT EXISTS idx_clinic_id ON clinic_doctors(clinic_id);


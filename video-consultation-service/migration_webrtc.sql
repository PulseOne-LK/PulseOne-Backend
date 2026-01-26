-- Migration: Switch from AWS Chime to WebRTC
-- This migration updates the video_consultation_sessions and video_consultation_attendees tables
-- to replace AWS Chime fields with WebRTC fields

-- Step 1: Add new WebRTC fields to video_consultation_sessions
ALTER TABLE video_consultation_sessions
ADD COLUMN IF NOT EXISTS room_id VARCHAR(255) UNIQUE,
ADD COLUMN IF NOT EXISTS doctor_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS patient_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS signaling_server_url VARCHAR(500);

-- Step 2: Drop AWS Chime fields from video_consultation_sessions
ALTER TABLE video_consultation_sessions
DROP COLUMN IF EXISTS meeting_id,
DROP COLUMN IF EXISTS external_meeting_id,
DROP COLUMN IF EXISTS media_region,
DROP COLUMN IF EXISTS media_placement_audio_host_url,
DROP COLUMN IF EXISTS media_placement_audio_fallback_url,
DROP COLUMN IF EXISTS media_placement_signaling_url,
DROP COLUMN IF EXISTS media_placement_turn_control_url;

-- Step 3: Add new WebRTC fields to video_consultation_attendees
ALTER TABLE video_consultation_attendees
ADD COLUMN IF NOT EXISTS peer_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS access_token TEXT;

-- Step 4: Drop AWS Chime fields from video_consultation_attendees
ALTER TABLE video_consultation_attendees
DROP COLUMN IF EXISTS chime_attendee_id,
DROP COLUMN IF EXISTS external_user_id,
DROP COLUMN IF EXISTS join_token;

-- Step 5: Update video_consultation_metrics (rename attendee_minutes_used to session_duration_minutes)
ALTER TABLE video_consultation_metrics
ADD COLUMN IF NOT EXISTS session_duration_minutes INTEGER DEFAULT 0;

-- Copy data if exists (for backwards compatibility)
UPDATE video_consultation_metrics
SET session_duration_minutes = attendee_minutes_used
WHERE attendee_minutes_used IS NOT NULL AND session_duration_minutes = 0;

-- Drop old column
ALTER TABLE video_consultation_metrics
DROP COLUMN IF EXISTS attendee_minutes_used;

-- Step 6: Create index on room_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_room_id ON video_consultation_sessions(room_id);

-- Step 7: Create index on peer_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_peer_id ON video_consultation_attendees(peer_id);

-- Migration complete
-- Note: Existing sessions will need to be recreated with WebRTC
-- Old AWS Chime sessions will no longer have valid meeting data

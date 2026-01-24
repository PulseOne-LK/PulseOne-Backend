-- Video Consultation Service Database Schema
-- PostgreSQL 13+

-- Create database (run separately)
-- CREATE DATABASE videodb;

-- Connect to database
\c videodb;

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Video Consultation Sessions Table
CREATE TABLE IF NOT EXISTS video_consultation_sessions (
    session_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    
    -- Booking Information
    booking_type VARCHAR(20) NOT NULL CHECK (booking_type IN ('CLINIC_BASED', 'DIRECT_DOCTOR')),
    appointment_id VARCHAR(36),
    
    -- Participants
    doctor_id VARCHAR(100) NOT NULL,
    patient_id VARCHAR(100) NOT NULL,
    clinic_id INTEGER,
    
    -- AWS Chime Meeting Details
    meeting_id VARCHAR(255) UNIQUE,
    external_meeting_id VARCHAR(255) UNIQUE,
    media_region VARCHAR(50),
    media_placement_audio_host_url VARCHAR(500),
    media_placement_audio_fallback_url VARCHAR(500),
    media_placement_signaling_url VARCHAR(500),
    media_placement_turn_control_url VARCHAR(500),
    
    -- Session Status and Timing
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'WAITING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    scheduled_start_time TIMESTAMP NOT NULL,
    scheduled_end_time TIMESTAMP NOT NULL,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    
    -- Session Details
    consultation_duration_minutes INTEGER DEFAULT 30,
    chief_complaint TEXT,
    session_notes TEXT,
    
    -- Participant Join Info
    doctor_joined_at TIMESTAMP,
    patient_joined_at TIMESTAMP,
    doctor_left_at TIMESTAMP,
    patient_left_at TIMESTAMP,
    
    -- Recording and Quality
    is_recording_enabled BOOLEAN DEFAULT FALSE,
    recording_url VARCHAR(500),
    connection_quality_rating INTEGER CHECK (connection_quality_rating BETWEEN 1 AND 5),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    cancelled_by VARCHAR(100)
);

-- Video Consultation Attendees Table
CREATE TABLE IF NOT EXISTS video_consultation_attendees (
    attendee_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    session_id VARCHAR(36) NOT NULL REFERENCES video_consultation_sessions(session_id) ON DELETE CASCADE,
    
    -- User Information
    user_id VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('DOCTOR', 'PATIENT')),
    
    -- AWS Chime Attendee Details
    chime_attendee_id VARCHAR(255) UNIQUE,
    external_user_id VARCHAR(255),
    join_token TEXT,
    
    -- Join/Leave Tracking
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN DEFAULT FALSE,
    
    -- Technical Details
    device_type VARCHAR(50),
    browser_info VARCHAR(200),
    ip_address VARCHAR(50),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(session_id, user_id)
);

-- Video Consultation Events Table
CREATE TABLE IF NOT EXISTS video_consultation_events (
    event_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    session_id VARCHAR(36) NOT NULL REFERENCES video_consultation_sessions(session_id) ON DELETE CASCADE,
    
    event_type VARCHAR(100) NOT NULL,
    event_description TEXT,
    user_id VARCHAR(100),
    
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_metadata TEXT
);

-- Doctor Availability Table
CREATE TABLE IF NOT EXISTS doctor_availability (
    availability_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    doctor_id VARCHAR(100) NOT NULL,
    
    -- Time Slots
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time VARCHAR(10) NOT NULL,
    end_time VARCHAR(10) NOT NULL,
    
    -- Slot Configuration
    slot_duration_minutes INTEGER DEFAULT 30,
    is_available BOOLEAN DEFAULT TRUE,
    max_patients_per_slot INTEGER DEFAULT 1,
    
    -- Pricing
    consultation_fee DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Validity
    effective_from TIMESTAMP NOT NULL,
    effective_until TIMESTAMP,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Video Consultation Metrics Table
CREATE TABLE IF NOT EXISTS video_consultation_metrics (
    metric_id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    session_id VARCHAR(36),
    
    -- Usage Tracking
    attendee_minutes_used INTEGER DEFAULT 0,
    date TIMESTAMP NOT NULL,
    
    -- Quality Metrics
    average_latency_ms INTEGER,
    packet_loss_percentage DECIMAL(5, 2),
    audio_quality_score INTEGER CHECK (audio_quality_score BETWEEN 1 AND 5),
    video_quality_score INTEGER CHECK (video_quality_score BETWEEN 1 AND 5),
    
    -- Session Outcome
    session_completed BOOLEAN DEFAULT FALSE,
    technical_issues BOOLEAN DEFAULT FALSE,
    issue_description TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes
CREATE INDEX idx_sessions_doctor ON video_consultation_sessions(doctor_id);
CREATE INDEX idx_sessions_patient ON video_consultation_sessions(patient_id);
CREATE INDEX idx_sessions_appointment ON video_consultation_sessions(appointment_id);
CREATE INDEX idx_sessions_status ON video_consultation_sessions(status);
CREATE INDEX idx_sessions_scheduled_time ON video_consultation_sessions(scheduled_start_time);
CREATE INDEX idx_sessions_created ON video_consultation_sessions(created_at);

CREATE INDEX idx_attendees_session ON video_consultation_attendees(session_id);
CREATE INDEX idx_attendees_user ON video_consultation_attendees(user_id);
CREATE INDEX idx_attendees_chime ON video_consultation_attendees(chime_attendee_id);

CREATE INDEX idx_events_session ON video_consultation_events(session_id);
CREATE INDEX idx_events_type ON video_consultation_events(event_type);
CREATE INDEX idx_events_timestamp ON video_consultation_events(event_timestamp);

CREATE INDEX idx_availability_doctor ON doctor_availability(doctor_id);
CREATE INDEX idx_availability_day ON doctor_availability(day_of_week);

CREATE INDEX idx_metrics_session ON video_consultation_metrics(session_id);
CREATE INDEX idx_metrics_date ON video_consultation_metrics(date);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers
CREATE TRIGGER update_sessions_updated_at BEFORE UPDATE ON video_consultation_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_attendees_updated_at BEFORE UPDATE ON video_consultation_attendees
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_availability_updated_at BEFORE UPDATE ON doctor_availability
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions (adjust as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_user;

-- Print success message
\echo 'Video Consultation Service schema created successfully!'
\echo 'Total tables created: 5'
\echo '  - video_consultation_sessions'
\echo '  - video_consultation_attendees'
\echo '  - video_consultation_events'
\echo '  - doctor_availability'
\echo '  - video_consultation_metrics'

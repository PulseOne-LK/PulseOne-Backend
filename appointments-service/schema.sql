-- ========================================
-- PulseOne Appointments Service Database Schema
-- Part 1 of 3: Session Management System
-- ========================================

-- This script creates the database schema for the appointments service
-- session management system, including doctors, clinics, sessions, and overrides.

-- ========================================
-- Table: doctors
-- ========================================
-- Stores basic doctor information for appointment scheduling
-- Links to the auth service via user_id
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,          -- Foreign key to auth service
    name VARCHAR(255) NOT NULL,                    -- Doctor's display name
    specialization VARCHAR(255) NOT NULL,          -- Medical specialization
    is_active BOOLEAN DEFAULT TRUE,                -- Whether doctor is accepting appointments
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_doctors_user_id ON doctors(user_id);
CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON doctors(specialization);
CREATE INDEX IF NOT EXISTS idx_doctors_active ON doctors(is_active);

-- ========================================
-- Table: clinics
-- ========================================
-- Stores basic clinic information for in-person appointments
-- Full clinic details are maintained in the profile service
CREATE TABLE IF NOT EXISTS clinics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,                    -- Clinic display name
    address TEXT NOT NULL,                         -- Physical address
    is_active BOOLEAN DEFAULT TRUE,                -- Whether clinic is operational
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_clinics_name ON clinics(name);
CREATE INDEX IF NOT EXISTS idx_clinics_active ON clinics(is_active);

-- ========================================
-- Table: sessions
-- ========================================
-- Stores recurring weekly sessions where doctors are available
-- Each session defines when a doctor is available, location, and booking parameters
CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_id BIGINT REFERENCES clinics(id) ON DELETE SET NULL,  -- NULL for virtual sessions
    day_of_week VARCHAR(20) NOT NULL,              -- MONDAY, TUESDAY, etc.
    session_start_time TIME NOT NULL,              -- Start time (e.g., 09:00)
    session_end_time TIME NOT NULL,                -- End time (e.g., 12:00)
    service_type VARCHAR(20) NOT NULL,             -- VIRTUAL, IN_PERSON, BOTH
    max_queue_size INTEGER NOT NULL CHECK (max_queue_size > 0),  -- Max patients in queue
    estimated_consultation_minutes INTEGER NOT NULL CHECK (estimated_consultation_minutes > 0),
    effective_from DATE NOT NULL,                  -- When this schedule starts
    effective_until DATE,                          -- When this schedule ends (NULL = indefinite)
    is_active BOOLEAN DEFAULT TRUE,                -- Whether session is currently active
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_session_times CHECK (session_start_time < session_end_time),
    CONSTRAINT chk_effective_dates CHECK (effective_until IS NULL OR effective_from <= effective_until),
    CONSTRAINT chk_service_type CHECK (service_type IN ('VIRTUAL', 'IN_PERSON', 'BOTH')),
    CONSTRAINT chk_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_sessions_doctor ON sessions(doctor_id);
CREATE INDEX IF NOT EXISTS idx_sessions_clinic ON sessions(clinic_id);
CREATE INDEX IF NOT EXISTS idx_sessions_day ON sessions(day_of_week);
CREATE INDEX IF NOT EXISTS idx_sessions_active ON sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_sessions_effective_dates ON sessions(effective_from, effective_until);
CREATE INDEX IF NOT EXISTS idx_sessions_doctor_day ON sessions(doctor_id, day_of_week);

-- ========================================
-- Table: session_overrides
-- ========================================
-- Stores exceptions to regular session schedules
-- Used for holidays, special hours, or temporary changes
CREATE TABLE IF NOT EXISTS session_overrides (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    override_date DATE NOT NULL,                   -- Specific date for this override
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,  -- Whether session is cancelled
    override_start_time TIME,                      -- Override start time (if not cancelled)
    override_end_time TIME,                        -- Override end time (if not cancelled)
    override_max_queue_size INTEGER,               -- Override max queue size
    reason VARCHAR(500),                           -- Reason for override
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_override_times CHECK (
        is_cancelled = TRUE OR 
        override_start_time IS NULL OR 
        override_end_time IS NULL OR 
        override_start_time < override_end_time
    ),
    CONSTRAINT chk_override_queue_size CHECK (override_max_queue_size IS NULL OR override_max_queue_size > 0),
    
    -- Unique constraint: one override per session per date
    UNIQUE(session_id, override_date)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_session_overrides_session ON session_overrides(session_id);
CREATE INDEX IF NOT EXISTS idx_session_overrides_date ON session_overrides(override_date);
CREATE INDEX IF NOT EXISTS idx_session_overrides_session_date ON session_overrides(session_id, override_date);

-- ========================================
-- Sample Data (Optional)
-- ========================================
-- Uncomment to insert sample data for testing

/*
-- Sample doctors
INSERT INTO doctors (user_id, name, specialization) VALUES
('doc001', 'Dr. Sarah Johnson', 'Cardiology'),
('doc002', 'Dr. Michael Chen', 'General Practice'),
('doc003', 'Dr. Emily Davis', 'Pediatrics');

-- Sample clinics
INSERT INTO clinics (name, address) VALUES
('Central Medical Clinic', '123 Main Street, Colombo 03'),
('Westside Health Center', '456 Galle Road, Colombo 06'),
('Downtown Medical Plaza', '789 Union Place, Colombo 02');

-- Sample sessions
INSERT INTO sessions (doctor_id, clinic_id, day_of_week, session_start_time, session_end_time, service_type, max_queue_size, estimated_consultation_minutes, effective_from) VALUES
(1, 1, 'MONDAY', '09:00', '12:00', 'IN_PERSON', 15, 20, '2024-01-01'),
(1, NULL, 'TUESDAY', '14:00', '17:00', 'VIRTUAL', 20, 15, '2024-01-01'),
(2, 2, 'WEDNESDAY', '08:00', '11:00', 'BOTH', 12, 30, '2024-01-01'),
(3, 3, 'FRIDAY', '10:00', '15:00', 'IN_PERSON', 10, 25, '2024-01-01');

-- Sample session override (holiday)
INSERT INTO session_overrides (session_id, override_date, is_cancelled, reason) VALUES
(1, '2024-12-25', TRUE, 'Christmas Day Holiday');
*/

-- ========================================
-- Views for Common Queries (Optional)
-- ========================================

-- View: Active sessions with doctor and clinic details
CREATE OR REPLACE VIEW active_sessions_view AS
SELECT 
    s.id as session_id,
    s.day_of_week,
    s.session_start_time,
    s.session_end_time,
    s.service_type,
    s.max_queue_size,
    s.estimated_consultation_minutes,
    s.effective_from,
    s.effective_until,
    d.user_id as doctor_user_id,
    d.name as doctor_name,
    d.specialization as doctor_specialization,
    c.id as clinic_id,
    c.name as clinic_name,
    c.address as clinic_address
FROM sessions s
JOIN doctors d ON s.doctor_id = d.id
LEFT JOIN clinics c ON s.clinic_id = c.id
WHERE s.is_active = TRUE AND d.is_active = TRUE;

-- ========================================
-- Database Comments
-- ========================================
COMMENT ON TABLE doctors IS 'Basic doctor information for appointment scheduling';
COMMENT ON TABLE clinics IS 'Basic clinic information for in-person appointments';
COMMENT ON TABLE sessions IS 'Recurring weekly sessions defining doctor availability';
COMMENT ON TABLE session_overrides IS 'Exceptions to regular session schedules (holidays, special hours)';

COMMENT ON COLUMN sessions.day_of_week IS 'Day of week using Java DayOfWeek enum values';
COMMENT ON COLUMN sessions.service_type IS 'Type of service: VIRTUAL, IN_PERSON, or BOTH';
COMMENT ON COLUMN sessions.effective_from IS 'Date when this session schedule becomes active';
COMMENT ON COLUMN sessions.effective_until IS 'Date when this session schedule ends (NULL for indefinite)';
COMMENT ON COLUMN session_overrides.is_cancelled IS 'TRUE if session is cancelled on override_date';
COMMENT ON COLUMN session_overrides.reason IS 'Human-readable reason for the override';

-- ========================================
-- Database Functions (Optional)
-- ========================================

-- Function to check if a session is available on a specific date
CREATE OR REPLACE FUNCTION is_session_available(
    p_session_id BIGINT,
    p_check_date DATE
) RETURNS BOOLEAN AS $$
DECLARE
    session_record RECORD;
    override_record RECORD;
BEGIN
    -- Get session details
    SELECT * INTO session_record 
    FROM sessions 
    WHERE id = p_session_id AND is_active = TRUE;
    
    -- Return false if session doesn't exist or is inactive
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    -- Check if date is within effective range
    IF p_check_date < session_record.effective_from OR 
       (session_record.effective_until IS NOT NULL AND p_check_date > session_record.effective_until) THEN
        RETURN FALSE;
    END IF;
    
    -- Check for overrides
    SELECT * INTO override_record 
    FROM session_overrides 
    WHERE session_id = p_session_id AND override_date = p_check_date;
    
    -- If override exists and is cancelled, session is not available
    IF FOUND AND override_record.is_cancelled = TRUE THEN
        RETURN FALSE;
    END IF;
    
    -- Session is available
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- PART 2: APPOINTMENT BOOKING TABLES
-- ========================================

-- Table: appointments
-- ========================================
-- Stores individual appointment bookings with queue management
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id VARCHAR(255) NOT NULL,              -- Foreign key to auth service
    doctor_id VARCHAR(255) NOT NULL,               -- Foreign key to auth service  
    clinic_id BIGINT REFERENCES clinics(id) ON DELETE SET NULL,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    appointment_date DATE NOT NULL,                -- Date of the appointment
    queue_number INTEGER NOT NULL,                 -- Position in queue for that session/date
    appointment_type VARCHAR(20) NOT NULL,         -- VIRTUAL, IN_PERSON
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',  -- BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    chief_complaint TEXT,                          -- Patient's reason for visit
    consultation_fee DECIMAL(10,2),                -- Fee for this appointment
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PAID, FAILED, REFUNDED
    payment_id VARCHAR(255),                       -- Payment service reference
    doctor_notes TEXT,                             -- Notes from doctor
    estimated_start_time TIMESTAMP,                -- Calculated estimated start time
    actual_start_time TIMESTAMP,                   -- When consultation actually started
    actual_end_time TIMESTAMP,                     -- When consultation actually ended
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_appointment_type CHECK (appointment_type IN ('VIRTUAL', 'IN_PERSON')),
    CONSTRAINT chk_appointment_status CHECK (status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_queue_number CHECK (queue_number > 0),
    CONSTRAINT chk_consultation_fee CHECK (consultation_fee >= 0),
    CONSTRAINT chk_actual_times CHECK (actual_start_time IS NULL OR actual_end_time IS NULL OR actual_start_time <= actual_end_time),
    
    -- Unique constraint: one queue number per session per date
    UNIQUE(session_id, appointment_date, queue_number)
);

-- Indexes for appointments table
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_session ON appointments(session_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_session_date ON appointments(session_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_date ON appointments(patient_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_queue ON appointments(session_id, appointment_date, queue_number);

-- Table: appointment_history
-- ========================================
-- Tracks all status changes for appointments (audit trail)
CREATE TABLE IF NOT EXISTS appointment_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    previous_status VARCHAR(20),                   -- Previous status (NULL for initial creation)
    new_status VARCHAR(20) NOT NULL,               -- New status after change
    change_reason VARCHAR(500),                    -- Reason for status change
    changed_by VARCHAR(255),                       -- Who made the change (user ID)
    changed_by_type VARCHAR(20),                   -- Type of user (PATIENT, DOCTOR, SYSTEM, ADMIN)
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_history_previous_status CHECK (previous_status IS NULL OR previous_status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_history_new_status CHECK (new_status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_changed_by_type CHECK (changed_by_type IN ('PATIENT', 'DOCTOR', 'SYSTEM', 'ADMIN'))
);

-- Indexes for appointment_history table
CREATE INDEX IF NOT EXISTS idx_appointment_history_appointment ON appointment_history(appointment_id);
CREATE INDEX IF NOT EXISTS idx_appointment_history_changed_at ON appointment_history(changed_at);
CREATE INDEX IF NOT EXISTS idx_appointment_history_changed_by ON appointment_history(changed_by);

-- ========================================
-- Views for Common Queries - Part 2
-- ========================================

-- View: Current queue status for all active sessions
CREATE OR REPLACE VIEW current_queue_status AS
SELECT 
    s.id as session_id,
    s.day_of_week,
    s.session_start_time,
    s.session_end_time,
    d.name as doctor_name,
    d.specialization,
    c.name as clinic_name,
    a.appointment_date,
    COUNT(a.appointment_id) as total_appointments,
    COUNT(CASE WHEN a.status NOT IN ('CANCELLED', 'NO_SHOW') THEN 1 END) as active_appointments,
    s.max_queue_size,
    (s.max_queue_size - COUNT(CASE WHEN a.status NOT IN ('CANCELLED', 'NO_SHOW') THEN 1 END)) as available_slots,
    MAX(CASE WHEN a.status NOT IN ('CANCELLED', 'NO_SHOW') THEN a.queue_number ELSE 0 END) as last_queue_number
FROM sessions s
JOIN doctors d ON s.doctor_id = d.id
LEFT JOIN clinics c ON s.clinic_id = c.id
LEFT JOIN appointments a ON s.id = a.session_id AND a.appointment_date >= CURRENT_DATE
WHERE s.is_active = TRUE
GROUP BY s.id, s.day_of_week, s.session_start_time, s.session_end_time, 
         d.name, d.specialization, c.name, a.appointment_date
ORDER BY a.appointment_date, s.session_start_time;

-- View: Patient appointment summary
CREATE OR REPLACE VIEW patient_appointment_summary AS
SELECT 
    a.patient_id,
    COUNT(*) as total_appointments,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completed_appointments,
    COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) as cancelled_appointments,
    COUNT(CASE WHEN a.status = 'NO_SHOW' THEN 1 END) as no_show_appointments,
    COUNT(CASE WHEN a.appointment_date >= CURRENT_DATE AND a.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') THEN 1 END) as upcoming_appointments,
    MAX(a.appointment_date) as last_appointment_date,
    MIN(CASE WHEN a.appointment_date >= CURRENT_DATE AND a.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') THEN a.appointment_date END) as next_appointment_date
FROM appointments a
GROUP BY a.patient_id;

-- ========================================
-- Functions for Appointment Management
-- ========================================

-- Function to get next queue number for a session and date
CREATE OR REPLACE FUNCTION get_next_queue_number(
    p_session_id BIGINT,
    p_appointment_date DATE
) RETURNS INTEGER AS $$
DECLARE
    next_number INTEGER;
BEGIN
    SELECT COALESCE(MAX(queue_number), 0) + 1 
    INTO next_number
    FROM appointments 
    WHERE session_id = p_session_id 
    AND appointment_date = p_appointment_date 
    AND status != 'CANCELLED';
    
    RETURN next_number;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate estimated wait time
CREATE OR REPLACE FUNCTION calculate_estimated_wait_time(
    p_session_id BIGINT,
    p_appointment_date DATE,
    p_queue_number INTEGER
) RETURNS INTERVAL AS $$
DECLARE
    consultation_minutes INTEGER;
    wait_minutes INTEGER;
BEGIN
    -- Get estimated consultation time from session
    SELECT estimated_consultation_minutes 
    INTO consultation_minutes
    FROM sessions 
    WHERE id = p_session_id;
    
    -- Calculate wait time based on queue position
    wait_minutes := (p_queue_number - 1) * consultation_minutes;
    
    RETURN make_interval(mins => wait_minutes);
END;
$$ LANGUAGE plpgsql;

-- Function to check appointment capacity
CREATE OR REPLACE FUNCTION check_appointment_capacity(
    p_session_id BIGINT,
    p_appointment_date DATE
) RETURNS BOOLEAN AS $$
DECLARE
    max_capacity INTEGER;
    current_count INTEGER;
    override_capacity INTEGER;
BEGIN
    -- Get session max capacity
    SELECT max_queue_size INTO max_capacity
    FROM sessions WHERE id = p_session_id;
    
    -- Check for override capacity
    SELECT override_max_queue_size INTO override_capacity
    FROM session_overrides 
    WHERE session_id = p_session_id 
    AND override_date = p_appointment_date
    AND is_cancelled = FALSE;
    
    -- Use override capacity if exists
    IF override_capacity IS NOT NULL THEN
        max_capacity := override_capacity;
    END IF;
    
    -- Count current active appointments
    SELECT COUNT(*) INTO current_count
    FROM appointments 
    WHERE session_id = p_session_id 
    AND appointment_date = p_appointment_date 
    AND status NOT IN ('CANCELLED', 'NO_SHOW');
    
    -- Return true if capacity available
    RETURN current_count < max_capacity;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- Sample Data for Appointments (Optional)
-- ========================================

/*
-- Sample appointments (uncomment to insert)
INSERT INTO appointments (patient_id, doctor_id, clinic_id, session_id, appointment_date, queue_number, appointment_type, chief_complaint, consultation_fee) VALUES
('pat001', 'doc001', 1, 1, '2024-01-15', 1, 'IN_PERSON', 'Chest pain and shortness of breath', 75.00),
('pat002', 'doc001', 1, 1, '2024-01-15', 2, 'IN_PERSON', 'Follow-up for hypertension', 75.00),
('pat003', 'doc002', NULL, 2, '2024-01-16', 1, 'VIRTUAL', 'General health consultation', 50.00);

-- Sample appointment history
INSERT INTO appointment_history (appointment_id, previous_status, new_status, change_reason, changed_by, changed_by_type)
SELECT appointment_id, NULL, 'BOOKED', 'Initial booking', patient_id, 'PATIENT'
FROM appointments;
*/

-- ========================================
-- Database Comments - Part 2
-- ========================================
COMMENT ON TABLE appointments IS 'Individual appointment bookings with queue management and status tracking';
COMMENT ON TABLE appointment_history IS 'Audit trail for all appointment status changes';

COMMENT ON COLUMN appointments.queue_number IS 'Position in queue for the specific session and date';
COMMENT ON COLUMN appointments.appointment_type IS 'VIRTUAL or IN_PERSON consultation type';
COMMENT ON COLUMN appointments.status IS 'Current appointment status through lifecycle';
COMMENT ON COLUMN appointments.estimated_start_time IS 'Calculated estimated start time based on queue position';
COMMENT ON COLUMN appointment_history.changed_by_type IS 'Type of user who made the change: PATIENT, DOCTOR, SYSTEM, ADMIN';

-- ========================================
-- PART 3: QUEUE MANAGEMENT & WAITING ROOM
-- ========================================

-- Table: waiting_room
-- ========================================
-- Tracks patients who have checked in and are waiting for consultation
CREATE TABLE IF NOT EXISTS waiting_room (
    waiting_room_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    checked_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP,                        -- When patient was called for consultation
    called_by VARCHAR(255),                     -- Staff member who called the patient
    consultation_started_at TIMESTAMP,          -- When consultation actually began
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_called_after_checkin CHECK (called_at IS NULL OR called_at >= checked_in_at),
    CONSTRAINT chk_consultation_after_called CHECK (consultation_started_at IS NULL OR (called_at IS NOT NULL AND consultation_started_at >= called_at)),
    
    -- Unique constraint: one entry per appointment
    UNIQUE(appointment_id)
);

-- Indexes for waiting_room table
CREATE INDEX IF NOT EXISTS idx_waiting_room_appointment ON waiting_room(appointment_id);
CREATE INDEX IF NOT EXISTS idx_waiting_room_checked_in ON waiting_room(checked_in_at);
CREATE INDEX IF NOT EXISTS idx_waiting_room_called ON waiting_room(called_at);
CREATE INDEX IF NOT EXISTS idx_waiting_room_status ON waiting_room(called_at, consultation_started_at);

-- Table: consultation_notes
-- ========================================
-- Medical records and consultation notes for completed appointments
CREATE TABLE IF NOT EXISTS consultation_notes (
    note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    doctor_id VARCHAR(255) NOT NULL,            -- Foreign key to auth service
    patient_id VARCHAR(255) NOT NULL,           -- Foreign key to auth service
    chief_complaint TEXT,                       -- Patient's main concern/reason for visit
    diagnosis TEXT,                             -- Doctor's diagnosis
    treatment_plan TEXT,                        -- Prescribed treatment
    vital_signs JSONB,                          -- JSON: {"bp": "120/80", "pulse": 72, "temp": 98.6, "weight": 70, "height": 175}
    medications_prescribed TEXT,                -- List of prescribed medications
    follow_up_required BOOLEAN DEFAULT FALSE,   -- Whether follow-up is needed
    follow_up_in_days INTEGER,                  -- Number of days for follow-up
    follow_up_instructions TEXT,                -- Specific follow-up instructions
    consultation_duration_minutes INTEGER,      -- How long the consultation took
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_follow_up_days CHECK (follow_up_in_days IS NULL OR follow_up_in_days > 0),
    CONSTRAINT chk_consultation_duration CHECK (consultation_duration_minutes IS NULL OR consultation_duration_minutes > 0),
    
    -- Unique constraint: one note per appointment
    UNIQUE(appointment_id)
);

-- Indexes for consultation_notes table
CREATE INDEX IF NOT EXISTS idx_consultation_notes_appointment ON consultation_notes(appointment_id);
CREATE INDEX IF NOT EXISTS idx_consultation_notes_doctor ON consultation_notes(doctor_id);
CREATE INDEX IF NOT EXISTS idx_consultation_notes_patient ON consultation_notes(patient_id);
CREATE INDEX IF NOT EXISTS idx_consultation_notes_created ON consultation_notes(created_at);
CREATE INDEX IF NOT EXISTS idx_consultation_notes_follow_up ON consultation_notes(follow_up_required, follow_up_in_days);

-- ========================================
-- Views for Queue Management - Part 3
-- ========================================

-- View: Current waiting room status
CREATE OR REPLACE VIEW current_waiting_room AS
SELECT 
    a.appointment_id,
    a.patient_id,
    a.doctor_id,
    a.appointment_date,
    a.queue_number,
    a.status,
    a.chief_complaint,
    a.estimated_start_time,
    wr.checked_in_at,
    wr.called_at,
    wr.called_by,
    wr.consultation_started_at,
    s.session_start_time,
    s.estimated_consultation_minutes,
    d.name as doctor_name,
    c.name as clinic_name,
    -- Calculate wait time
    CASE 
        WHEN a.status = 'CHECKED_IN' THEN EXTRACT(EPOCH FROM (NOW() - wr.checked_in_at))/60
        WHEN a.status = 'IN_PROGRESS' THEN EXTRACT(EPOCH FROM (wr.called_at - wr.checked_in_at))/60
        WHEN a.status = 'COMPLETED' THEN EXTRACT(EPOCH FROM (wr.consultation_started_at - wr.checked_in_at))/60
        ELSE NULL
    END as actual_wait_minutes
FROM appointments a
JOIN sessions s ON a.session_id = s.id
JOIN doctors d ON a.doctor_id = d.id
LEFT JOIN clinics c ON a.clinic_id = c.id
LEFT JOIN waiting_room wr ON a.appointment_id = wr.appointment_id
WHERE a.appointment_date = CURRENT_DATE
AND a.status IN ('CHECKED_IN', 'IN_PROGRESS', 'COMPLETED')
ORDER BY a.queue_number;

-- View: Doctor queue dashboard
CREATE OR REPLACE VIEW doctor_queue_dashboard AS
SELECT 
    a.doctor_id,
    a.appointment_date,
    d.name as doctor_name,
    COUNT(*) as total_appointments,
    COUNT(CASE WHEN a.status = 'BOOKED' THEN 1 END) as pending_checkin,
    COUNT(CASE WHEN a.status = 'CHECKED_IN' THEN 1 END) as waiting_patients,
    COUNT(CASE WHEN a.status = 'IN_PROGRESS' THEN 1 END) as in_consultation,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completed,
    COUNT(CASE WHEN a.status = 'NO_SHOW' THEN 1 END) as no_shows,
    COUNT(CASE WHEN a.status = 'CANCELLED' THEN 1 END) as cancelled,
    -- Current patient being served
    MAX(CASE WHEN a.status = 'IN_PROGRESS' THEN a.queue_number END) as current_queue_number,
    -- Next patient to be called
    MIN(CASE WHEN a.status = 'CHECKED_IN' THEN a.queue_number END) as next_queue_number,
    -- Average wait time for completed appointments
    AVG(CASE 
        WHEN a.status = 'COMPLETED' AND wr.checked_in_at IS NOT NULL AND wr.called_at IS NOT NULL
        THEN EXTRACT(EPOCH FROM (wr.called_at - wr.checked_in_at))/60
    END) as avg_wait_minutes
FROM appointments a
JOIN doctors d ON a.doctor_id = d.id
LEFT JOIN waiting_room wr ON a.appointment_id = wr.appointment_id
WHERE a.appointment_date = CURRENT_DATE
GROUP BY a.doctor_id, a.appointment_date, d.name
ORDER BY a.doctor_id;

-- View: Patient consultation history
CREATE OR REPLACE VIEW patient_consultation_history AS
SELECT 
    cn.patient_id,
    cn.note_id,
    cn.appointment_id,
    a.appointment_date,
    cn.doctor_id,
    d.name as doctor_name,
    d.specialization,
    cn.chief_complaint,
    cn.diagnosis,
    cn.treatment_plan,
    cn.vital_signs,
    cn.medications_prescribed,
    cn.follow_up_required,
    cn.follow_up_in_days,
    cn.consultation_duration_minutes,
    cn.created_at,
    -- Calculate follow-up due date
    CASE 
        WHEN cn.follow_up_required AND cn.follow_up_in_days IS NOT NULL 
        THEN a.appointment_date + INTERVAL '1 day' * cn.follow_up_in_days
    END as follow_up_due_date
FROM consultation_notes cn
JOIN appointments a ON cn.appointment_id = a.appointment_id
JOIN doctors d ON cn.doctor_id = d.id
ORDER BY cn.created_at DESC;

-- ========================================
-- Functions for Queue Management - Part 3
-- ========================================

-- Function to calculate estimated wait time for a patient
CREATE OR REPLACE FUNCTION calculate_patient_wait_time(
    p_appointment_id UUID
) RETURNS TABLE (
    patients_ahead INTEGER,
    estimated_wait_minutes INTEGER,
    current_queue_number INTEGER
) AS $$
DECLARE
    patient_queue_number INTEGER;
    patient_doctor_id VARCHAR(255);
    patient_date DATE;
    consultation_minutes INTEGER;
    current_serving INTEGER;
BEGIN
    -- Get patient's appointment details
    SELECT a.queue_number, a.doctor_id, a.appointment_date, s.estimated_consultation_minutes
    INTO patient_queue_number, patient_doctor_id, patient_date, consultation_minutes
    FROM appointments a
    JOIN sessions s ON a.session_id = s.id
    WHERE a.appointment_id = p_appointment_id;
    
    -- Get current queue number being served
    SELECT MAX(a.queue_number)
    INTO current_serving
    FROM appointments a
    WHERE a.doctor_id = patient_doctor_id
    AND a.appointment_date = patient_date
    AND a.status = 'IN_PROGRESS';
    
    -- If no one is being served, check the last completed
    IF current_serving IS NULL THEN
        SELECT COALESCE(MAX(a.queue_number), 0)
        INTO current_serving
        FROM appointments a
        WHERE a.doctor_id = patient_doctor_id
        AND a.appointment_date = patient_date
        AND a.status = 'COMPLETED';
    END IF;
    
    -- Calculate patients ahead
    SELECT COUNT(*)
    INTO patients_ahead
    FROM appointments a
    WHERE a.doctor_id = patient_doctor_id
    AND a.appointment_date = patient_date
    AND a.queue_number < patient_queue_number
    AND a.status IN ('CHECKED_IN', 'IN_PROGRESS');
    
    -- Calculate estimated wait time
    estimated_wait_minutes := patients_ahead * consultation_minutes;
    
    RETURN QUERY SELECT patients_ahead, estimated_wait_minutes, COALESCE(current_serving, 0);
END;
$$ LANGUAGE plpgsql;

-- Function to get next patient to call
CREATE OR REPLACE FUNCTION get_next_patient_to_call(
    p_doctor_id VARCHAR(255),
    p_appointment_date DATE DEFAULT CURRENT_DATE
) RETURNS UUID AS $$
DECLARE
    next_appointment_id UUID;
BEGIN
    SELECT a.appointment_id
    INTO next_appointment_id
    FROM appointments a
    LEFT JOIN waiting_room wr ON a.appointment_id = wr.appointment_id
    WHERE a.doctor_id = p_doctor_id
    AND a.appointment_date = p_appointment_date
    AND a.status = 'CHECKED_IN'
    AND wr.called_at IS NULL
    ORDER BY a.queue_number
    LIMIT 1;
    
    RETURN next_appointment_id;
END;
$$ LANGUAGE plpgsql;

-- Function to validate status transitions
CREATE OR REPLACE FUNCTION validate_appointment_status_transition(
    p_current_status VARCHAR(20),
    p_new_status VARCHAR(20)
) RETURNS BOOLEAN AS $$
BEGIN
    -- Define valid transitions
    CASE p_current_status
        WHEN 'BOOKED' THEN
            RETURN p_new_status IN ('CHECKED_IN', 'CANCELLED', 'NO_SHOW');
        WHEN 'CHECKED_IN' THEN
            RETURN p_new_status IN ('IN_PROGRESS', 'CANCELLED', 'NO_SHOW');
        WHEN 'IN_PROGRESS' THEN
            RETURN p_new_status IN ('COMPLETED', 'CANCELLED');
        WHEN 'COMPLETED' THEN
            RETURN FALSE; -- No transitions allowed from completed
        WHEN 'CANCELLED' THEN
            RETURN FALSE; -- No transitions allowed from cancelled
        WHEN 'NO_SHOW' THEN
            RETURN FALSE; -- No transitions allowed from no-show
        ELSE
            RETURN FALSE;
    END CASE;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- Sample Data for Queue Management (Optional)
-- ========================================

/*
-- Sample waiting room entries (uncomment to insert)
INSERT INTO waiting_room (appointment_id, checked_in_at, called_by) 
SELECT appointment_id, NOW() - INTERVAL '30 minutes', 'Nurse Sarah'
FROM appointments 
WHERE status = 'CHECKED_IN' 
LIMIT 3;

-- Sample consultation notes (uncomment to insert)
INSERT INTO consultation_notes (
    appointment_id, doctor_id, patient_id, chief_complaint, diagnosis, 
    treatment_plan, vital_signs, follow_up_required, follow_up_in_days
) VALUES (
    (SELECT appointment_id FROM appointments WHERE status = 'COMPLETED' LIMIT 1),
    'doc001', 'pat001',
    'Chest pain and shortness of breath',
    'Mild hypertension and anxiety',
    'Prescribed Lisinopril 10mg daily, recommended stress management techniques',
    '{"bp": "140/90", "pulse": 85, "temp": 98.6, "weight": 75, "height": 175}',
    true, 14
);
*/

-- ========================================
-- Triggers for Automatic History Tracking - Part 3
-- ========================================

-- Trigger function to log appointment status changes to history
CREATE OR REPLACE FUNCTION log_appointment_status_change() 
RETURNS TRIGGER AS $$
BEGIN
    -- Only log if status actually changed
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO appointment_history (
            appointment_id, 
            previous_status, 
            new_status, 
            change_reason, 
            changed_by, 
            changed_by_type
        ) VALUES (
            NEW.appointment_id,
            OLD.status,
            NEW.status,
            CASE NEW.status
                WHEN 'CHECKED_IN' THEN 'Patient checked in for appointment'
                WHEN 'IN_PROGRESS' THEN 'Patient called for consultation'
                WHEN 'COMPLETED' THEN 'Consultation completed'
                WHEN 'NO_SHOW' THEN 'Patient did not show up'
                WHEN 'CANCELLED' THEN 'Appointment cancelled'
                ELSE 'Status updated'
            END,
            'SYSTEM', -- This should be updated to actual user when available
            'SYSTEM'
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS appointment_status_change_trigger ON appointments;
CREATE TRIGGER appointment_status_change_trigger
    AFTER UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION log_appointment_status_change();

-- ========================================
-- Database Comments - Part 3
-- ========================================
COMMENT ON TABLE waiting_room IS 'Tracks patients who have checked in and their waiting room journey';
COMMENT ON TABLE consultation_notes IS 'Medical records and consultation notes for completed appointments';

COMMENT ON COLUMN waiting_room.called_at IS 'Timestamp when patient was called for consultation';
COMMENT ON COLUMN waiting_room.called_by IS 'Name of staff member who called the patient';
COMMENT ON COLUMN consultation_notes.vital_signs IS 'JSON object containing vital signs measurements';
COMMENT ON COLUMN consultation_notes.follow_up_required IS 'Whether patient needs follow-up appointment';

-- ========================================
-- End of Schema Definition
-- ========================================
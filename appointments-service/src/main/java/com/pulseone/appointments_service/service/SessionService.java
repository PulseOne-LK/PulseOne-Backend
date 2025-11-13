package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.request.CreateSessionOverrideRequest;
import com.pulseone.appointments_service.dto.request.CreateSessionRequest;
import com.pulseone.appointments_service.dto.request.UpdateSessionRequest;
import com.pulseone.appointments_service.dto.response.SessionOverrideResponse;
import com.pulseone.appointments_service.dto.response.SessionResponse;
import com.pulseone.appointments_service.entity.Clinic;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.entity.Session;
import com.pulseone.appointments_service.entity.SessionOverride;
import com.pulseone.appointments_service.repository.ClinicRepository;
import com.pulseone.appointments_service.repository.DoctorRepository;
import com.pulseone.appointments_service.repository.SessionOverrideRepository;
import com.pulseone.appointments_service.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing doctor sessions and session overrides.
 * Handles business logic, validation, and data transformation.
 */
@Service
@Transactional
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private SessionOverrideRepository sessionOverrideRepository;

    /**
     * Create a new session with validation
     */
    public SessionResponse createSession(CreateSessionRequest request) {
        // Validate doctor exists
        Doctor doctor = doctorRepository.findByUserId(request.getDoctorUserId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with userId: " + request.getDoctorUserId()));

        // Validate clinic if provided
        Clinic clinic = null;
        if (request.getClinicId() != null) {
            clinic = clinicRepository.findByProfileClinicId(request.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found with profile clinic id: " + request.getClinicId()));
        }

        // Validate session times
        validateSessionTimes(request.getSessionStartTime(), request.getSessionEndTime());

        // Check for overlapping sessions
        List<Session> overlappingSessions = sessionRepository.findOverlappingSessions(
                doctor, request.getDayOfWeek(), request.getSessionStartTime(), request.getSessionEndTime());
        
        if (!overlappingSessions.isEmpty()) {
            throw new IllegalArgumentException("Session times overlap with existing session for this doctor on " + request.getDayOfWeek());
        }

        // Create and save session
        Session session = new Session();
        session.setDoctor(doctor);
        session.setClinic(clinic);
        session.setDayOfWeek(request.getDayOfWeek());
        session.setSessionStartTime(request.getSessionStartTime());
        session.setSessionEndTime(request.getSessionEndTime());
        session.setServiceType(request.getServiceType());
        session.setMaxQueueSize(request.getMaxQueueSize());
        session.setEstimatedConsultationMinutes(request.getEstimatedConsultationMinutes());
        session.setEffectiveFrom(request.getEffectiveFrom());
        session.setEffectiveUntil(request.getEffectiveUntil());
        session.setIsActive(true);

        Session savedSession = sessionRepository.save(session);
        return convertToSessionResponse(savedSession);
    }

    /**
     * Get all sessions for a specific doctor
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsByDoctorUserId(String doctorUserId) {
        List<Session> sessions = sessionRepository.findByDoctorUserIdAndIsActiveTrue(doctorUserId);
        return sessions.stream()
                .map(this::convertToSessionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing session
     */
    public SessionResponse updateSession(Long sessionId, UpdateSessionRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + sessionId));

        // Update clinic if provided
        if (request.getClinicId() != null) {
            Clinic clinic = clinicRepository.findByProfileClinicId(request.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found with profile clinic id: " + request.getClinicId()));
            session.setClinic(clinic);
        }

        // Validate and update times if provided
        if (request.getSessionStartTime() != null && request.getSessionEndTime() != null) {
            validateSessionTimes(request.getSessionStartTime(), request.getSessionEndTime());
            
            // Check for overlapping sessions (excluding current session)
            List<Session> overlappingSessions = sessionRepository.findOverlappingSessions(
                    session.getDoctor(), 
                    request.getDayOfWeek() != null ? request.getDayOfWeek() : session.getDayOfWeek(),
                    request.getSessionStartTime(), 
                    request.getSessionEndTime());
            
            overlappingSessions.removeIf(s -> s.getId().equals(sessionId));
            
            if (!overlappingSessions.isEmpty()) {
                throw new IllegalArgumentException("Updated session times overlap with existing session");
            }

            session.setSessionStartTime(request.getSessionStartTime());
            session.setSessionEndTime(request.getSessionEndTime());
        }

        // Update other fields if provided
        if (request.getDayOfWeek() != null) {
            session.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getServiceType() != null) {
            session.setServiceType(request.getServiceType());
        }
        if (request.getMaxQueueSize() != null) {
            if (request.getMaxQueueSize() <= 0) {
                throw new IllegalArgumentException("Max queue size must be positive");
            }
            session.setMaxQueueSize(request.getMaxQueueSize());
        }
        if (request.getEstimatedConsultationMinutes() != null) {
            if (request.getEstimatedConsultationMinutes() <= 0) {
                throw new IllegalArgumentException("Estimated consultation minutes must be positive");
            }
            session.setEstimatedConsultationMinutes(request.getEstimatedConsultationMinutes());
        }
        if (request.getEffectiveFrom() != null) {
            session.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveUntil() != null) {
            session.setEffectiveUntil(request.getEffectiveUntil());
        }
        if (request.getIsActive() != null) {
            session.setIsActive(request.getIsActive());
        }

        Session updatedSession = sessionRepository.save(session);
        return convertToSessionResponse(updatedSession);
    }

    /**
     * Soft delete a session (set isActive to false)
     */
    public void deleteSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + sessionId));
        
        session.setIsActive(false);
        sessionRepository.save(session);
    }

    /**
     * Create a session override
     */
    public SessionOverrideResponse createSessionOverride(CreateSessionOverrideRequest request) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + request.getSessionId()));

        // Check if override already exists for this date
        if (sessionOverrideRepository.existsBySessionAndOverrideDate(session, request.getOverrideDate())) {
            throw new IllegalArgumentException("Override already exists for session on " + request.getOverrideDate());
        }

        // Validate override times if not cancelled
        if (!request.getIsCancelled() && request.getOverrideStartTime() != null && request.getOverrideEndTime() != null) {
            validateSessionTimes(request.getOverrideStartTime(), request.getOverrideEndTime());
        }

        SessionOverride override = new SessionOverride();
        override.setSession(session);
        override.setOverrideDate(request.getOverrideDate());
        override.setIsCancelled(request.getIsCancelled());
        override.setOverrideStartTime(request.getOverrideStartTime());
        override.setOverrideEndTime(request.getOverrideEndTime());
        override.setOverrideMaxQueueSize(request.getOverrideMaxQueueSize());
        override.setReason(request.getReason());

        SessionOverride savedOverride = sessionOverrideRepository.save(override);
        return convertToSessionOverrideResponse(savedOverride);
    }

    /**
     * Get session by ID
     */
    @Transactional(readOnly = true)
    public Optional<SessionResponse> getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::convertToSessionResponse);
    }

    /**
     * Validate session times
     */
    private void validateSessionTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Session start time and end time are required");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Session start time must be before end time");
        }
    }

    /**
     * Convert Session entity to SessionResponse DTO
     */
    private SessionResponse convertToSessionResponse(Session session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setDayOfWeek(session.getDayOfWeek());
        response.setSessionStartTime(session.getSessionStartTime());
        response.setSessionEndTime(session.getSessionEndTime());
        response.setServiceType(session.getServiceType());
        response.setMaxQueueSize(session.getMaxQueueSize());
        response.setEstimatedConsultationMinutes(session.getEstimatedConsultationMinutes());
        response.setEffectiveFrom(session.getEffectiveFrom());
        response.setEffectiveUntil(session.getEffectiveUntil());
        response.setIsActive(session.getIsActive());

        // Set doctor information
        if (session.getDoctor() != null) {
            SessionResponse.DoctorResponse doctorResponse = new SessionResponse.DoctorResponse(
                    session.getDoctor().getId(),
                    session.getDoctor().getUserId(),
                    session.getDoctor().getName(),
                    session.getDoctor().getSpecialization()
            );
            response.setDoctor(doctorResponse);
        }

        // Set clinic information if available
        if (session.getClinic() != null) {
            SessionResponse.ClinicResponse clinicResponse = new SessionResponse.ClinicResponse(
                    session.getClinic().getProfileClinicId(),
                    session.getClinic().getName(),
                    session.getClinic().getAddress()
            );
            response.setClinic(clinicResponse);
        }

        return response;
    }

    /**
     * Convert SessionOverride entity to SessionOverrideResponse DTO
     */
    private SessionOverrideResponse convertToSessionOverrideResponse(SessionOverride override) {
        SessionOverrideResponse response = new SessionOverrideResponse();
        response.setId(override.getId());
        response.setSessionId(override.getSession().getId());
        response.setOverrideDate(override.getOverrideDate());
        response.setIsCancelled(override.getIsCancelled());
        response.setOverrideStartTime(override.getOverrideStartTime());
        response.setOverrideEndTime(override.getOverrideEndTime());
        response.setOverrideMaxQueueSize(override.getOverrideMaxQueueSize());
        response.setReason(override.getReason());
        return response;
    }
}
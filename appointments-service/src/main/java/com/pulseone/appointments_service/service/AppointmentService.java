package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.request.BookAppointmentRequest;
import com.pulseone.appointments_service.dto.response.AppointmentResponse;
import com.pulseone.appointments_service.dto.response.BookingResponse;
import com.pulseone.appointments_service.entity.*;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing appointments, bookings, and queue management.
 */
@Service
@Transactional
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionOverrideRepository sessionOverrideRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * Book a new appointment with comprehensive validation and queue management
     */
    public BookingResponse bookAppointment(BookAppointmentRequest request) {
        // Validate session exists and is active
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found with id: " + request.getSessionId()));

        if (!session.getIsActive()) {
            throw new IllegalArgumentException("Session is not active");
        }

        // Validate doctor matches session
        if (!session.getDoctor().getUserId().equals(request.getDoctorId())) {
            throw new IllegalArgumentException("Doctor ID does not match session doctor");
        }

        // Validate appointment date
        validateAppointmentDate(request.getAppointmentDate(), session);

        // Check if patient already has appointment with this doctor on same date
        Optional<Appointment> existingAppointment = appointmentRepository
                .findActiveAppointmentByPatientDoctorAndDate(
                        request.getPatientId(), request.getDoctorId(), request.getAppointmentDate());
        
        if (existingAppointment.isPresent()) {
            throw new IllegalArgumentException("Patient already has an appointment with this doctor on " + request.getAppointmentDate());
        }

        // Check session capacity considering overrides
        validateSessionCapacity(session, request.getAppointmentDate());

        // Validate appointment type is compatible with session service type
        validateAppointmentType(request.getAppointmentType(), session);

        // Calculate queue number
        Integer queueNumber = calculateQueueNumber(session, request.getAppointmentDate());

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(request.getPatientId());
        appointment.setDoctorId(request.getDoctorId());
        appointment.setSession(session);
        appointment.setClinic(session.getClinic());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setQueueNumber(queueNumber);
        appointment.setAppointmentType(request.getAppointmentType());
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setChiefComplaint(request.getChiefComplaint());

        // Set consultation fee (this could come from doctor profile service in future)
        appointment.setConsultationFee(BigDecimal.valueOf(50.00)); // Default fee for now

        // Calculate estimated start time
        LocalDateTime estimatedStartTime = calculateEstimatedStartTime(session, request.getAppointmentDate(), queueNumber);
        appointment.setEstimatedStartTime(estimatedStartTime);

        // Save appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create history record
        createAppointmentHistory(savedAppointment, null, AppointmentStatus.BOOKED, 
                "Appointment booked", request.getPatientId(), "PATIENT");

        // Calculate estimated wait time
        Integer estimatedWaitTime = (queueNumber - 1) * session.getEstimatedConsultationMinutes();

        return new BookingResponse(
                savedAppointment.getAppointmentId(),
                queueNumber,
                estimatedStartTime,
                estimatedWaitTime,
                savedAppointment.getConsultationFee(),
                "Appointment booked successfully. You are number " + queueNumber + " in the queue."
        );
    }

    /**
     * Get appointments for a specific patient
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(String patientId) {
        List<Appointment> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescCreatedAtDesc(patientId);
        
        return appointments.stream()
                .map(this::convertToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming appointments for a patient
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientUpcomingAppointments(String patientId) {
        List<Appointment> appointments = appointmentRepository
                .findUpcomingAppointmentsByPatientId(patientId, LocalDate.now());
        
        return appointments.stream()
                .map(this::convertToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get past appointments for a patient
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientPastAppointments(String patientId) {
        List<Appointment> appointments = appointmentRepository
                .findPastAppointmentsByPatientId(patientId, LocalDate.now());
        
        return appointments.stream()
                .map(this::convertToAppointmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancel an appointment
     */
    public AppointmentResponse cancelAppointment(UUID appointmentId, String cancelledBy, String cancelledByType, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel completed appointment");
        }

        AppointmentStatus previousStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.CANCELLED);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create history record
        createAppointmentHistory(savedAppointment, previousStatus, AppointmentStatus.CANCELLED, 
                reason != null ? reason : "Appointment cancelled", cancelledBy, cancelledByType);

        return convertToAppointmentResponse(savedAppointment);
    }

    /**
     * Get appointment by ID
     */
    @Transactional(readOnly = true)
    public Optional<AppointmentResponse> getAppointmentById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(this::convertToAppointmentResponse);
    }

    /**
     * Validate appointment date
     */
    private void validateAppointmentDate(LocalDate appointmentDate, Session session) {
        if (appointmentDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book appointment for past date");
        }

        // Check if date matches session day of week
        DayOfWeek appointmentDayOfWeek = appointmentDate.getDayOfWeek();
        if (!session.getDayOfWeek().equals(appointmentDayOfWeek)) {
            throw new IllegalArgumentException("Session is not available on " + appointmentDayOfWeek);
        }

        // Check if session is effective on this date
        if (appointmentDate.isBefore(session.getEffectiveFrom()) ||
            (session.getEffectiveUntil() != null && appointmentDate.isAfter(session.getEffectiveUntil()))) {
            throw new IllegalArgumentException("Session is not effective on " + appointmentDate);
        }

        // Check for session override
        Optional<SessionOverride> override = sessionOverrideRepository
                .findBySessionAndOverrideDate(session, appointmentDate);
        
        if (override.isPresent() && override.get().getIsCancelled()) {
            throw new IllegalArgumentException("Session is cancelled on " + appointmentDate + ": " + override.get().getReason());
        }
    }

    /**
     * Validate session capacity
     */
    private void validateSessionCapacity(Session session, LocalDate appointmentDate) {
        // Get override max queue size if exists
        Optional<SessionOverride> override = sessionOverrideRepository
                .findBySessionAndOverrideDate(session, appointmentDate);
        
        Integer maxSlots = override.map(SessionOverride::getOverrideMaxQueueSize)
                .orElse(session.getMaxQueueSize());

        // Count current active appointments
        Long currentCount = appointmentRepository.countActiveAppointmentsForSessionAndDate(session, appointmentDate);

        if (currentCount >= maxSlots) {
            throw new IllegalArgumentException("Session is fully booked on " + appointmentDate);
        }
    }

    /**
     * Validate appointment type compatibility
     */
    private void validateAppointmentType(com.pulseone.appointments_service.enums.AppointmentType appointmentType, Session session) {
        switch (session.getServiceType()) {
            case VIRTUAL:
                if (appointmentType != com.pulseone.appointments_service.enums.AppointmentType.VIRTUAL) {
                    throw new IllegalArgumentException("This session only supports virtual appointments");
                }
                break;
            case IN_PERSON:
                if (appointmentType != com.pulseone.appointments_service.enums.AppointmentType.IN_PERSON) {
                    throw new IllegalArgumentException("This session only supports in-person appointments");
                }
                break;
            case BOTH:
                // Both types are allowed
                break;
        }
    }

    /**
     * Calculate queue number for appointment
     */
    private Integer calculateQueueNumber(Session session, LocalDate appointmentDate) {
        Optional<Integer> maxQueueNumber = appointmentRepository.getMaxQueueNumberForSessionAndDate(session, appointmentDate);
        return maxQueueNumber.orElse(0) + 1;
    }

    /**
     * Calculate estimated start time for appointment
     */
    private LocalDateTime calculateEstimatedStartTime(Session session, LocalDate appointmentDate, Integer queueNumber) {
        // Get session start time, considering overrides
        Optional<SessionOverride> override = sessionOverrideRepository
                .findBySessionAndOverrideDate(session, appointmentDate);
        
        LocalTime sessionStartTime = override.map(SessionOverride::getOverrideStartTime)
                .orElse(session.getSessionStartTime());

        // Calculate estimated start time based on queue position
        LocalDateTime sessionDateTime = appointmentDate.atTime(sessionStartTime);
        int waitTimeMinutes = (queueNumber - 1) * session.getEstimatedConsultationMinutes();
        
        return sessionDateTime.plusMinutes(waitTimeMinutes);
    }

    /**
     * Create appointment history record
     */
    private void createAppointmentHistory(Appointment appointment, AppointmentStatus previousStatus, 
                                        AppointmentStatus newStatus, String reason, String changedBy, String changedByType) {
        AppointmentHistory history = new AppointmentHistory(
                appointment, previousStatus, newStatus, reason, changedBy, changedByType);
        appointmentHistoryRepository.save(history);
    }

    /**
     * Convert Appointment entity to AppointmentResponse DTO
     */
    private AppointmentResponse convertToAppointmentResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setAppointmentId(appointment.getAppointmentId());
        response.setPatientId(appointment.getPatientId());
        response.setDoctorId(appointment.getDoctorId());
        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setQueueNumber(appointment.getQueueNumber());
        response.setAppointmentType(appointment.getAppointmentType());
        response.setStatus(appointment.getStatus());
        response.setChiefComplaint(appointment.getChiefComplaint());
        response.setConsultationFee(appointment.getConsultationFee());
        response.setPaymentStatus(appointment.getPaymentStatus());
        response.setPaymentId(appointment.getPaymentId());
        response.setDoctorNotes(appointment.getDoctorNotes());
        response.setEstimatedStartTime(appointment.getEstimatedStartTime());
        response.setActualStartTime(appointment.getActualStartTime());
        response.setActualEndTime(appointment.getActualEndTime());
        response.setCreatedAt(appointment.getCreatedAt());
        response.setUpdatedAt(appointment.getUpdatedAt());

        // Set doctor information
        if (appointment.getSession() != null && appointment.getSession().getDoctor() != null) {
            Doctor doctor = appointment.getSession().getDoctor();
            response.setDoctor(new AppointmentResponse.DoctorSummary(
                    doctor.getUserId(), doctor.getName(), doctor.getSpecialization()));
        }

        // Set clinic information
        if (appointment.getClinic() != null) {
            response.setClinic(new AppointmentResponse.ClinicSummary(
                    appointment.getClinic().getId(),
                    appointment.getClinic().getName(),
                    appointment.getClinic().getAddress()));
        }

        // Set session information
        if (appointment.getSession() != null) {
            Session session = appointment.getSession();
            AppointmentResponse.SessionSummary sessionSummary = new AppointmentResponse.SessionSummary();
            sessionSummary.setId(session.getId());
            sessionSummary.setDayOfWeek(session.getDayOfWeek().toString());
            sessionSummary.setSessionStartTime(session.getSessionStartTime().toString());
            sessionSummary.setSessionEndTime(session.getSessionEndTime().toString());
            sessionSummary.setServiceType(session.getServiceType().toString());
            sessionSummary.setEstimatedConsultationMinutes(session.getEstimatedConsultationMinutes());
            response.setSession(sessionSummary);
        }

        return response;
    }
}
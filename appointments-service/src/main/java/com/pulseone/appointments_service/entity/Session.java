package com.pulseone.appointments_service.entity;

import com.pulseone.appointments_service.enums.ServiceType;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a recurring weekly session where a doctor is available for appointments.
 * Sessions define when doctors are available, at which clinic (if applicable), 
 * and the parameters for appointment booking.
 */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the doctor conducting this session
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * Reference to the clinic where the session takes place.
     * Nullable for virtual-only sessions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    /**
     * Day of the week when this session occurs (e.g., MONDAY, TUESDAY)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /**
     * Start time of the session (e.g., 09:00)
     */
    @Column(name = "session_start_time", nullable = false)
    private LocalTime sessionStartTime;

    /**
     * End time of the session (e.g., 12:00)
     */
    @Column(name = "session_end_time", nullable = false)
    private LocalTime sessionEndTime;

    /**
     * Type of service offered during this session
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    /**
     * Maximum number of patients that can be queued for this session
     */
    @Column(name = "max_queue_size", nullable = false)
    private Integer maxQueueSize;

    /**
     * Estimated consultation time per patient in minutes
     */
    @Column(name = "estimated_consultation_minutes", nullable = false)
    private Integer estimatedConsultationMinutes;

    /**
     * Date from which this session schedule becomes effective
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Date until which this session schedule is effective (nullable for indefinite)
     */
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    /**
     * Whether this session is currently active
     */
    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    // Constructors
    public Session() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Clinic getClinic() {
        return clinic;
    }

    public void setClinic(Clinic clinic) {
        this.clinic = clinic;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(LocalTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public LocalTime getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(LocalTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public Integer getEstimatedConsultationMinutes() {
        return estimatedConsultationMinutes;
    }

    public void setEstimatedConsultationMinutes(Integer estimatedConsultationMinutes) {
        this.estimatedConsultationMinutes = estimatedConsultationMinutes;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDate effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
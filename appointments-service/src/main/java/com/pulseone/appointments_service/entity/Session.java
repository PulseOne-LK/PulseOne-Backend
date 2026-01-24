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
     * Actual doctor user ID from auth service (denormalized for easy access)
     */
    @Column(name = "doctor_user_id", nullable = false)
    private String doctorUserId;

    /**
     * Reference to the clinic where the session takes place.
     * Nullable for virtual-only sessions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    /**
     * Actual clinic profile ID from profile service (denormalized for easy access)
     * Nullable for virtual sessions
     */
    @Column(name = "clinic_profile_id")
    private Long clinicProfileId;

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

    /**
     * Who created/manages this session.
     * - CLINIC_ADMIN: Session created by clinic admin for physical consultations
     * - DOCTOR: Session created by doctor for virtual direct consultations
     * 
     * This field enforces the dual-mode concept:
     * - CLINIC_ADMIN sessions must be IN_PERSON with clinic reference
     * - DOCTOR sessions must be VIRTUAL without clinic reference
     */
    @Column(name = "creator_type", length = 20)
    private String creatorType;

    /**
     * User ID of the creator (clinic admin ID or doctor ID)
     */
    @Column(name = "creator_id")
    private String creatorId;

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

    public String getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(String doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public Long getClinicProfileId() {
        return clinicProfileId;
    }

    public void setClinicProfileId(Long clinicProfileId) {
        this.clinicProfileId = clinicProfileId;
    }

    public String getCreatorType() {
        return creatorType;
    }

    public void setCreatorType(String creatorType) {
        this.creatorType = creatorType;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
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
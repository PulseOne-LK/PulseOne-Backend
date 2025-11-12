package com.pulseone.appointments_service.dto.request;

import com.pulseone.appointments_service.enums.ServiceType;
import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a new session
 */
public class CreateSessionRequest {

    @NotBlank(message = "Doctor user ID is required")
    private String doctorUserId;

    private Long clinicId; // Optional for virtual sessions

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Session start time is required")
    private LocalTime sessionStartTime;

    @NotNull(message = "Session end time is required")
    private LocalTime sessionEndTime;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Max queue size is required")
    @Min(value = 1, message = "Max queue size must be at least 1")
    @Max(value = 50, message = "Max queue size cannot exceed 50")
    private Integer maxQueueSize;

    @NotNull(message = "Estimated consultation minutes is required")
    @Min(value = 5, message = "Estimated consultation minutes must be at least 5")
    @Max(value = 180, message = "Estimated consultation minutes cannot exceed 180")
    private Integer estimatedConsultationMinutes;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveUntil; // Optional

    // Constructors
    public CreateSessionRequest() {
    }

    // Getters and Setters
    public String getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(String doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public Long getClinicId() {
        return clinicId;
    }

    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
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
}
package com.pulseone.appointments_service.dto.request;

import com.pulseone.appointments_service.enums.ServiceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for updating an existing session
 * Note: clinicId should be the profile_clinic_id from the profile service, not the appointments service clinic ID
 */
public class UpdateSessionRequest {

    /**
     * Clinic ID from profile service (profile_clinic_id in appointments DB)
     * Optional
     */
    private Long clinicId;

    private DayOfWeek dayOfWeek;

    private LocalTime sessionStartTime;

    private LocalTime sessionEndTime;

    private ServiceType serviceType;

    private Integer maxQueueSize;

    private Integer estimatedConsultationMinutes;

    private LocalDate effectiveFrom;

    private LocalDate effectiveUntil;

    private Boolean isActive;

    // Constructors
    public UpdateSessionRequest() {
    }

    // Getters and Setters
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
package com.pulseone.appointments_service.dto.response;

import com.pulseone.appointments_service.enums.ServiceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for session information
 */
public class SessionResponse {

    private Long id;
    private DoctorResponse doctor;
    private ClinicResponse clinic; // Can be null for virtual sessions
    private DayOfWeek dayOfWeek;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private ServiceType serviceType;
    private Integer maxQueueSize;
    private Integer estimatedConsultationMinutes;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Boolean isActive;
    private String creatorType; // CLINIC_ADMIN or DOCTOR
    private String creatorId; // User ID of creator

    // Constructors
    public SessionResponse() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DoctorResponse getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorResponse doctor) {
        this.doctor = doctor;
    }

    public ClinicResponse getClinic() {
        return clinic;
    }

    public void setClinic(ClinicResponse clinic) {
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

    /**
     * Nested DTO for doctor information in session response
     */
    public static class DoctorResponse {
        private Long id;
        private String userId;
        private String name;
        private String specialization;

        // Constructors
        public DoctorResponse() {
        }

        public DoctorResponse(Long id, String userId, String name, String specialization) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.specialization = specialization;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSpecialization() {
            return specialization;
        }

        public void setSpecialization(String specialization) {
            this.specialization = specialization;
        }
    }

    /**
     * Nested DTO for clinic information in session response
     */
    public static class ClinicResponse {
        private Long id;
        private String name;
        private String address;

        // Constructors
        public ClinicResponse() {
        }

        public ClinicResponse(Long id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
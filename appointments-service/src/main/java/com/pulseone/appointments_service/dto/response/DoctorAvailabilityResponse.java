package com.pulseone.appointments_service.dto.response;

import com.pulseone.appointments_service.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for doctor availability information
 */
public class DoctorAvailabilityResponse {

    private String doctorUserId;
    private String doctorName;
    private String specialization;
    private BigDecimal consultationFee;
    private List<AvailableSlot> availableSlots;

    // Constructors
    public DoctorAvailabilityResponse() {
    }

    // Getters and Setters
    public String getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(String doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public List<AvailableSlot> getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(List<AvailableSlot> availableSlots) {
        this.availableSlots = availableSlots;
    }

    /**
     * Nested class for available time slots
     */
    public static class AvailableSlot {
        private Long sessionId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private ServiceType serviceType;
        private Integer availableSlots;
        private Integer totalSlots;
        private String clinicName;
        private String clinicAddress;

        // Constructors
        public AvailableSlot() {
        }

        // Getters and Setters
        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public void setServiceType(ServiceType serviceType) {
            this.serviceType = serviceType;
        }

        public Integer getAvailableSlots() {
            return availableSlots;
        }

        public void setAvailableSlots(Integer availableSlots) {
            this.availableSlots = availableSlots;
        }

        public Integer getTotalSlots() {
            return totalSlots;
        }

        public void setTotalSlots(Integer totalSlots) {
            this.totalSlots = totalSlots;
        }

        public String getClinicName() {
            return clinicName;
        }

        public void setClinicName(String clinicName) {
            this.clinicName = clinicName;
        }

        public String getClinicAddress() {
            return clinicAddress;
        }

        public void setClinicAddress(String clinicAddress) {
            this.clinicAddress = clinicAddress;
        }
    }
}
package com.pulseone.appointments_service.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for doctor calendar availability (30-day view)
 */
public class DoctorCalendarResponse {

    private String doctorUserId;
    private String doctorName;
    private String specialization;
    private List<CalendarDay> calendar;

    // Constructors
    public DoctorCalendarResponse() {
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

    public List<CalendarDay> getCalendar() {
        return calendar;
    }

    public void setCalendar(List<CalendarDay> calendar) {
        this.calendar = calendar;
    }

    /**
     * Nested class for calendar day information
     */
    public static class CalendarDay {
        private LocalDate date;
        private String dayOfWeek;
        private Boolean isAvailable;
        private List<SessionSlot> sessions;
        private String unavailableReason; // e.g., "Holiday", "No sessions", "Fully booked"

        // Constructors
        public CalendarDay() {
        }

        // Getters and Setters
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public Boolean getIsAvailable() {
            return isAvailable;
        }

        public void setIsAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
        }

        public List<SessionSlot> getSessions() {
            return sessions;
        }

        public void setSessions(List<SessionSlot> sessions) {
            this.sessions = sessions;
        }

        public String getUnavailableReason() {
            return unavailableReason;
        }

        public void setUnavailableReason(String unavailableReason) {
            this.unavailableReason = unavailableReason;
        }
    }

    /**
     * Nested class for session slot information
     */
    public static class SessionSlot {
        private Long sessionId;
        private String startTime;
        private String endTime;
        private String serviceType;
        private Integer availableSlots;
        private Integer totalSlots;
        private String clinicName;

        // Constructors
        public SessionSlot() {
        }

        // Getters and Setters
        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
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
    }
}
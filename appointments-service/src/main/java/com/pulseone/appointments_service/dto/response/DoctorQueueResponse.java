package com.pulseone.appointments_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for doctor queue dashboard
 * Provides comprehensive overview of doctor's queue for the day
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorQueueResponse {
    
    private String doctorId;
    private String doctorName;
    private String specialization;
    private LocalDate appointmentDate;
    
    // Queue statistics
    private Integer totalAppointments;
    private Integer pendingCheckIn;      // BOOKED status
    private Integer waitingPatients;     // CHECKED_IN status
    private Integer inConsultation;      // IN_PROGRESS status
    private Integer completed;           // COMPLETED status
    private Integer noShows;            // NO_SHOW status
    private Integer cancelled;          // CANCELLED status
    
    // Current queue information
    private Integer currentQueueNumber;  // Currently being served
    private Integer nextQueueNumber;     // Next patient to be called
    private Double averageWaitMinutes;   // Average wait time today
    private Double averageConsultationMinutes; // Average consultation time
    
    // Session information
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private Integer estimatedConsultationMinutes;
    
    // Detailed queue list
    private List<QueueStatusResponse> queueList;
    
    // Constructors
    public DoctorQueueResponse() {}
    
    public DoctorQueueResponse(String doctorId, String doctorName, LocalDate appointmentDate) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
    }
    
    // Business methods
    
    /**
     * Calculate completion rate
     */
    public Double getCompletionRate() {
        if (totalAppointments == null || totalAppointments == 0) {
            return 0.0;
        }
        return (completed != null ? completed.doubleValue() : 0.0) / totalAppointments * 100;
    }
    
    /**
     * Calculate no-show rate
     */
    public Double getNoShowRate() {
        if (totalAppointments == null || totalAppointments == 0) {
            return 0.0;
        }
        return (noShows != null ? noShows.doubleValue() : 0.0) / totalAppointments * 100;
    }
    
    /**
     * Check if doctor is currently available
     */
    public boolean isDoctorAvailable() {
        return inConsultation != null && inConsultation == 0 && 
               waitingPatients != null && waitingPatients > 0;
    }
    
    /**
     * Get remaining appointments for the day
     */
    public Integer getRemainingAppointments() {
        if (totalAppointments == null) return 0;
        int processedAppointments = (completed != null ? completed : 0) + 
                                   (noShows != null ? noShows : 0) + 
                                   (cancelled != null ? cancelled : 0);
        return totalAppointments - processedAppointments;
    }
    
    // Getters and Setters
    public String getDoctorId() {
        return doctorId;
    }
    
    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
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
    
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public Integer getTotalAppointments() {
        return totalAppointments;
    }
    
    public void setTotalAppointments(Integer totalAppointments) {
        this.totalAppointments = totalAppointments;
    }
    
    public Integer getPendingCheckIn() {
        return pendingCheckIn;
    }
    
    public void setPendingCheckIn(Integer pendingCheckIn) {
        this.pendingCheckIn = pendingCheckIn;
    }
    
    public Integer getWaitingPatients() {
        return waitingPatients;
    }
    
    public void setWaitingPatients(Integer waitingPatients) {
        this.waitingPatients = waitingPatients;
    }
    
    public Integer getInConsultation() {
        return inConsultation;
    }
    
    public void setInConsultation(Integer inConsultation) {
        this.inConsultation = inConsultation;
    }
    
    public Integer getCompleted() {
        return completed;
    }
    
    public void setCompleted(Integer completed) {
        this.completed = completed;
    }
    
    public Integer getNoShows() {
        return noShows;
    }
    
    public void setNoShows(Integer noShows) {
        this.noShows = noShows;
    }
    
    public Integer getCancelled() {
        return cancelled;
    }
    
    public void setCancelled(Integer cancelled) {
        this.cancelled = cancelled;
    }
    
    public Integer getCurrentQueueNumber() {
        return currentQueueNumber;
    }
    
    public void setCurrentQueueNumber(Integer currentQueueNumber) {
        this.currentQueueNumber = currentQueueNumber;
    }
    
    public Integer getNextQueueNumber() {
        return nextQueueNumber;
    }
    
    public void setNextQueueNumber(Integer nextQueueNumber) {
        this.nextQueueNumber = nextQueueNumber;
    }
    
    public Double getAverageWaitMinutes() {
        return averageWaitMinutes;
    }
    
    public void setAverageWaitMinutes(Double averageWaitMinutes) {
        this.averageWaitMinutes = averageWaitMinutes;
    }
    
    public Double getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }
    
    public void setAverageConsultationMinutes(Double averageConsultationMinutes) {
        this.averageConsultationMinutes = averageConsultationMinutes;
    }
    
    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }
    
    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }
    
    public LocalDateTime getSessionEndTime() {
        return sessionEndTime;
    }
    
    public void setSessionEndTime(LocalDateTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }
    
    public Integer getEstimatedConsultationMinutes() {
        return estimatedConsultationMinutes;
    }
    
    public void setEstimatedConsultationMinutes(Integer estimatedConsultationMinutes) {
        this.estimatedConsultationMinutes = estimatedConsultationMinutes;
    }
    
    public List<QueueStatusResponse> getQueueList() {
        return queueList;
    }
    
    public void setQueueList(List<QueueStatusResponse> queueList) {
        this.queueList = queueList;
    }
    
    @Override
    public String toString() {
        return "DoctorQueueResponse{" +
                "doctorId='" + doctorId + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", totalAppointments=" + totalAppointments +
                ", waitingPatients=" + waitingPatients +
                ", inConsultation=" + inConsultation +
                ", completed=" + completed +
                ", currentQueueNumber=" + currentQueueNumber +
                ", nextQueueNumber=" + nextQueueNumber +
                ", averageWaitMinutes=" + averageWaitMinutes +
                '}';
    }
}
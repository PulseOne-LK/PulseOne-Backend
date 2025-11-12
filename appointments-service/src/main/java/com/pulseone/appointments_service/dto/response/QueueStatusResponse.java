package com.pulseone.appointments_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for queue status information
 * Provides comprehensive queue information for waiting room displays
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueStatusResponse {
    
    private UUID appointmentId;
    private String patientId;
    private String doctorId;
    private String doctorName;
    private String clinicName;
    private LocalDate appointmentDate;
    private Integer queueNumber;
    private AppointmentStatus status;
    private String chiefComplaint;
    
    // Waiting room information
    private LocalDateTime checkedInAt;
    private LocalDateTime calledAt;
    private String calledBy;
    private LocalDateTime consultationStartedAt;
    
    // Queue position information
    private Integer currentQueueNumber;      // Queue number currently being served
    private Integer patientsAhead;           // Number of patients ahead in queue
    private Integer estimatedWaitMinutes;    // Estimated wait time in minutes
    private Long actualWaitMinutes;          // Actual wait time so far
    
    // Session information
    private LocalDateTime sessionStartTime;
    private Integer estimatedConsultationMinutes;
    
    // Constructors
    public QueueStatusResponse() {}
    
    public QueueStatusResponse(UUID appointmentId, String patientId, Integer queueNumber, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.queueNumber = queueNumber;
        this.status = status;
    }
    
    // Business methods
    
    /**
     * Check if patient is currently waiting
     */
    public boolean isWaiting() {
        return status == AppointmentStatus.CHECKED_IN && calledAt == null;
    }
    
    /**
     * Check if patient is in consultation
     */
    public boolean isInConsultation() {
        return status == AppointmentStatus.IN_PROGRESS;
    }
    
    /**
     * Check if consultation is completed
     */
    public boolean isCompleted() {
        return status == AppointmentStatus.COMPLETED;
    }
    
    // Getters and Setters
    public UUID getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
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
    
    public String getClinicName() {
        return clinicName;
    }
    
    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }
    
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public Integer getQueueNumber() {
        return queueNumber;
    }
    
    public void setQueueNumber(Integer queueNumber) {
        this.queueNumber = queueNumber;
    }
    
    public AppointmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
    
    public String getChiefComplaint() {
        return chiefComplaint;
    }
    
    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }
    
    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
    
    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
    
    public LocalDateTime getCalledAt() {
        return calledAt;
    }
    
    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }
    
    public String getCalledBy() {
        return calledBy;
    }
    
    public void setCalledBy(String calledBy) {
        this.calledBy = calledBy;
    }
    
    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }
    
    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }
    
    public Integer getCurrentQueueNumber() {
        return currentQueueNumber;
    }
    
    public void setCurrentQueueNumber(Integer currentQueueNumber) {
        this.currentQueueNumber = currentQueueNumber;
    }
    
    public Integer getPatientsAhead() {
        return patientsAhead;
    }
    
    public void setPatientsAhead(Integer patientsAhead) {
        this.patientsAhead = patientsAhead;
    }
    
    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }
    
    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }
    
    public Long getActualWaitMinutes() {
        return actualWaitMinutes;
    }
    
    public void setActualWaitMinutes(Long actualWaitMinutes) {
        this.actualWaitMinutes = actualWaitMinutes;
    }
    
    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }
    
    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }
    
    public Integer getEstimatedConsultationMinutes() {
        return estimatedConsultationMinutes;
    }
    
    public void setEstimatedConsultationMinutes(Integer estimatedConsultationMinutes) {
        this.estimatedConsultationMinutes = estimatedConsultationMinutes;
    }
    
    @Override
    public String toString() {
        return "QueueStatusResponse{" +
                "appointmentId=" + appointmentId +
                ", queueNumber=" + queueNumber +
                ", status=" + status +
                ", currentQueueNumber=" + currentQueueNumber +
                ", patientsAhead=" + patientsAhead +
                ", estimatedWaitMinutes=" + estimatedWaitMinutes +
                ", actualWaitMinutes=" + actualWaitMinutes +
                '}';
    }
}
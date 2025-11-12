package com.pulseone.appointments_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for consultation notes
 * Returns medical record information after consultation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationNotesResponse {
    
    private UUID noteId;
    private UUID appointmentId;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String chiefComplaint;
    private String diagnosis;
    private String treatmentPlan;
    private Map<String, Object> vitalSigns;
    private String medicationsPrescribed;
    private Boolean followUpRequired;
    private Integer followUpInDays;
    private String followUpInstructions;
    private Integer consultationDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional computed fields
    private LocalDateTime followUpDueDate;
    private Boolean isFollowUpOverdue;
    
    // Constructors
    public ConsultationNotesResponse() {}
    
    public ConsultationNotesResponse(UUID noteId, UUID appointmentId, String doctorId, String patientId) {
        this.noteId = noteId;
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
    }
    
    // Business methods
    
    /**
     * Get specific vital sign value
     */
    public Object getVitalSign(String key) {
        return vitalSigns != null ? vitalSigns.get(key) : null;
    }
    
    /**
     * Check if consultation requires follow-up
     */
    public boolean requiresFollowUp() {
        return Boolean.TRUE.equals(followUpRequired);
    }
    
    // Getters and Setters
    public UUID getNoteId() {
        return noteId;
    }
    
    public void setNoteId(UUID noteId) {
        this.noteId = noteId;
    }
    
    public UUID getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
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
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public String getChiefComplaint() {
        return chiefComplaint;
    }
    
    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }
    
    public String getDiagnosis() {
        return diagnosis;
    }
    
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
    
    public String getTreatmentPlan() {
        return treatmentPlan;
    }
    
    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }
    
    public Map<String, Object> getVitalSigns() {
        return vitalSigns;
    }
    
    public void setVitalSigns(Map<String, Object> vitalSigns) {
        this.vitalSigns = vitalSigns;
    }
    
    public String getMedicationsPrescribed() {
        return medicationsPrescribed;
    }
    
    public void setMedicationsPrescribed(String medicationsPrescribed) {
        this.medicationsPrescribed = medicationsPrescribed;
    }
    
    public Boolean getFollowUpRequired() {
        return followUpRequired;
    }
    
    public void setFollowUpRequired(Boolean followUpRequired) {
        this.followUpRequired = followUpRequired;
    }
    
    public Integer getFollowUpInDays() {
        return followUpInDays;
    }
    
    public void setFollowUpInDays(Integer followUpInDays) {
        this.followUpInDays = followUpInDays;
    }
    
    public String getFollowUpInstructions() {
        return followUpInstructions;
    }
    
    public void setFollowUpInstructions(String followUpInstructions) {
        this.followUpInstructions = followUpInstructions;
    }
    
    public Integer getConsultationDurationMinutes() {
        return consultationDurationMinutes;
    }
    
    public void setConsultationDurationMinutes(Integer consultationDurationMinutes) {
        this.consultationDurationMinutes = consultationDurationMinutes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getFollowUpDueDate() {
        return followUpDueDate;
    }
    
    public void setFollowUpDueDate(LocalDateTime followUpDueDate) {
        this.followUpDueDate = followUpDueDate;
    }
    
    public Boolean getIsFollowUpOverdue() {
        return isFollowUpOverdue;
    }
    
    public void setIsFollowUpOverdue(Boolean isFollowUpOverdue) {
        this.isFollowUpOverdue = isFollowUpOverdue;
    }
    
    @Override
    public String toString() {
        return "ConsultationNotesResponse{" +
                "noteId=" + noteId +
                ", appointmentId=" + appointmentId +
                ", doctorId='" + doctorId + '\'' +
                ", patientId='" + patientId + '\'' +
                ", chiefComplaint='" + chiefComplaint + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                ", followUpRequired=" + followUpRequired +
                ", followUpInDays=" + followUpInDays +
                ", createdAt=" + createdAt +
                '}';
    }
}
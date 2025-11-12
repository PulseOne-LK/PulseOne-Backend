package com.pulseone.appointments_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating consultation notes
 * Used by doctors to record medical information after consultation
 */
public class ConsultationNotesRequest {
    
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;
    
    @NotBlank(message = "Chief complaint is required")
    private String chiefComplaint;
    
    private String diagnosis;
    
    private String treatmentPlan;
    
    private Map<String, Object> vitalSigns;
    
    private String medicationsPrescribed;
    
    private Boolean followUpRequired = false;
    
    @Positive(message = "Follow-up days must be positive")
    private Integer followUpInDays;
    
    private String followUpInstructions;
    
    @Positive(message = "Consultation duration must be positive")
    private Integer consultationDurationMinutes;
    
    // Constructors
    public ConsultationNotesRequest() {}
    
    public ConsultationNotesRequest(UUID appointmentId, String chiefComplaint) {
        this.appointmentId = appointmentId;
        this.chiefComplaint = chiefComplaint;
    }
    
    // Getters and Setters
    public UUID getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
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
    
    @Override
    public String toString() {
        return "ConsultationNotesRequest{" +
                "appointmentId=" + appointmentId +
                ", chiefComplaint='" + chiefComplaint + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                ", followUpRequired=" + followUpRequired +
                ", followUpInDays=" + followUpInDays +
                ", consultationDurationMinutes=" + consultationDurationMinutes +
                '}';
    }
}
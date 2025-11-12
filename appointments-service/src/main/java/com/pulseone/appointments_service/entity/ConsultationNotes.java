package com.pulseone.appointments_service.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing consultation notes and medical records
 * Stores detailed medical information for completed appointments
 */
@Entity
@Table(name = "consultation_notes")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationNotes {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "note_id", columnDefinition = "UUID")
    private UUID noteId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;
    
    @Column(name = "doctor_id", nullable = false)
    private String doctorId;
    
    @Column(name = "patient_id", nullable = false)
    private String patientId;
    
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;
    
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;
    
    @Type(JsonType.class)
    @Column(name = "vital_signs", columnDefinition = "jsonb")
    private Map<String, Object> vitalSigns;
    
    @Column(name = "medications_prescribed", columnDefinition = "TEXT")
    private String medicationsPrescribed;
    
    @Column(name = "follow_up_required")
    private Boolean followUpRequired = false;
    
    @Column(name = "follow_up_in_days")
    private Integer followUpInDays;
    
    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;
    
    @Column(name = "consultation_duration_minutes")
    private Integer consultationDurationMinutes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ConsultationNotes() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ConsultationNotes(Appointment appointment, String doctorId, String patientId) {
        this();
        this.appointment = appointment;
        this.doctorId = doctorId;
        this.patientId = patientId;
    }
    
    // Lifecycle callbacks
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    
    /**
     * Calculate follow-up due date based on appointment date and follow-up days
     */
    public LocalDateTime getFollowUpDueDate() {
        if (!Boolean.TRUE.equals(followUpRequired) || followUpInDays == null || appointment == null) {
            return null;
        }
        return appointment.getAppointmentDate().atStartOfDay().plusDays(followUpInDays);
    }
    
    /**
     * Check if follow-up is overdue
     */
    public boolean isFollowUpOverdue() {
        LocalDateTime dueDate = getFollowUpDueDate();
        return dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }
    
    /**
     * Set vital signs with validation
     */
    public void setVitalSignsWithDefaults(Map<String, Object> vitalSigns) {
        this.vitalSigns = vitalSigns;
        // Could add validation here for required vital signs
    }
    
    /**
     * Get specific vital sign value
     */
    public Object getVitalSign(String key) {
        return vitalSigns != null ? vitalSigns.get(key) : null;
    }
    
    /**
     * Add or update a vital sign
     */
    public void setVitalSign(String key, Object value) {
        if (this.vitalSigns == null) {
            this.vitalSigns = new java.util.HashMap<>();
        }
        this.vitalSigns.put(key, value);
    }
    
    // Getters and Setters
    public UUID getNoteId() {
        return noteId;
    }
    
    public void setNoteId(UUID noteId) {
        this.noteId = noteId;
    }
    
    public Appointment getAppointment() {
        return appointment;
    }
    
    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
    
    public String getDoctorId() {
        return doctorId;
    }
    
    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
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
    
    @Override
    public String toString() {
        return "ConsultationNotes{" +
                "noteId=" + noteId +
                ", appointmentId=" + (appointment != null ? appointment.getAppointmentId() : null) +
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
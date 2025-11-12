package com.pulseone.appointments_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for calling the next patient in queue
 * Used by doctors/staff to call patients for consultation
 */
public class CallNextPatientRequest {
    
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;
    
    @NotBlank(message = "Staff member name is required")
    private String calledBy;
    
    private String notes; // Optional notes about calling the patient
    
    // Constructors
    public CallNextPatientRequest() {}
    
    public CallNextPatientRequest(UUID appointmentId, String calledBy) {
        this.appointmentId = appointmentId;
        this.calledBy = calledBy;
    }
    
    // Getters and Setters
    public UUID getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }
    
    public String getCalledBy() {
        return calledBy;
    }
    
    public void setCalledBy(String calledBy) {
        this.calledBy = calledBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "CallNextPatientRequest{" +
                "appointmentId=" + appointmentId +
                ", calledBy='" + calledBy + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
package com.pulseone.appointments_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for patient check-in operations
 * Used when a patient arrives at the clinic and checks in for their appointment
 */
public class CheckInRequest {
    
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;
    
    @NotBlank(message = "Staff member name is required")
    private String checkedInBy;
    
    private String notes; // Optional notes about check-in
    
    // Constructors
    public CheckInRequest() {}
    
    public CheckInRequest(UUID appointmentId, String checkedInBy) {
        this.appointmentId = appointmentId;
        this.checkedInBy = checkedInBy;
    }
    
    // Getters and Setters
    public UUID getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }
    
    public String getCheckedInBy() {
        return checkedInBy;
    }
    
    public void setCheckedInBy(String checkedInBy) {
        this.checkedInBy = checkedInBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "CheckInRequest{" +
                "appointmentId=" + appointmentId +
                ", checkedInBy='" + checkedInBy + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
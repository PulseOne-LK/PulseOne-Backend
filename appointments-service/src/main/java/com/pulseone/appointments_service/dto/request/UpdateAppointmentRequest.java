package com.pulseone.appointments_service.dto.request;

import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.enums.AppointmentType;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing appointment
 * Only non-null fields will be updated
 */
public class UpdateAppointmentRequest {

    /**
     * Optional: Update appointment date (with validation)
     */
    private LocalDate appointmentDate;

    /**
     * Optional: Update appointment type (VIRTUAL or IN_PERSON)
     */
    private AppointmentType appointmentType;

    /**
     * Optional: Update chief complaint/reason for visit
     */
    @Size(max = 1000, message = "Chief complaint cannot exceed 1000 characters")
    private String chiefComplaint;

    /**
     * Optional: Update consultation fee
     */
    private BigDecimal consultationFee;

    /**
     * Optional: Update appointment status
     */
    private AppointmentStatus status;

    /**
     * Optional: Update doctor notes
     */
    @Size(max = 2000, message = "Doctor notes cannot exceed 2000 characters")
    private String doctorNotes;

    /**
     * Optional: Update actual start time of consultation
     */
    private LocalDateTime actualStartTime;

    /**
     * Optional: Update actual end time of consultation
     */
    private LocalDateTime actualEndTime;

    // Constructors
    public UpdateAppointmentRequest() {
    }

    // Getters and Setters
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(AppointmentType appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }
}

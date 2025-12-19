package com.pulseone.appointments_service.dto.request;

import com.pulseone.appointments_service.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for booking a new appointment
 * Note: doctorId should be the user_id from auth service, not the appointments service doctor ID
 */
public class BookAppointmentRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    /**
     * Doctor user ID from auth service (stored in doctor.user_id)
     */
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Appointment type is required")
    private AppointmentType appointmentType;

    @NotBlank(message = "Chief complaint is required")
    private String chiefComplaint;

    // Constructors
    public BookAppointmentRequest() {
    }

    // Getters and Setters
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

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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
}
package com.pulseone.appointments_service.dto.response;

import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.enums.AppointmentType;
import com.pulseone.appointments_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for appointment information
 */
public class AppointmentResponse {

    private UUID appointmentId;
    private String patientId;
    private String doctorId;
    private DoctorSummary doctor;
    private ClinicSummary clinic;
    private SessionSummary session;
    private LocalDate appointmentDate;
    private Integer queueNumber;
    private AppointmentType appointmentType;
    private AppointmentStatus status;
    private String chiefComplaint;
    private BigDecimal consultationFee;
    private PaymentStatus paymentStatus;
    private String paymentId;
    private String doctorNotes;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String meetingLink; // AWS Chime link for VIRTUAL appointments
    private String meetingId; // AWS Chime meeting ID

    // Constructors
    public AppointmentResponse() {
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

    public DoctorSummary getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorSummary doctor) {
        this.doctor = doctor;
    }

    public ClinicSummary getClinic() {
        return clinic;
    }

    public void setClinic(ClinicSummary clinic) {
        this.clinic = clinic;
    }

    public SessionSummary getSession() {
        return session;
    }

    public void setSession(SessionSummary session) {
        this.session = session;
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

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(AppointmentType appointmentType) {
        this.appointmentType = appointmentType;
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

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    public LocalDateTime getEstimatedStartTime() {
        return estimatedStartTime;
    }

    public void setEstimatedStartTime(LocalDateTime estimatedStartTime) {
        this.estimatedStartTime = estimatedStartTime;
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

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    // Nested DTOs
    public static class DoctorSummary {
        private String userId;
        private String name;
        private String specialization;

        public DoctorSummary() {}

        public DoctorSummary(String userId, String name, String specialization) {
            this.userId = userId;
            this.name = name;
            this.specialization = specialization;
        }

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }

    public static class ClinicSummary {
        private Long id;
        private String name;
        private String address;

        public ClinicSummary() {}

        public ClinicSummary(Long id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public static class SessionSummary {
        private Long id;
        private String dayOfWeek;
        private String sessionStartTime;
        private String sessionEndTime;
        private String serviceType;
        private Integer estimatedConsultationMinutes;

        public SessionSummary() {}

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public String getSessionStartTime() { return sessionStartTime; }
        public void setSessionStartTime(String sessionStartTime) { this.sessionStartTime = sessionStartTime; }
        public String getSessionEndTime() { return sessionEndTime; }
        public void setSessionEndTime(String sessionEndTime) { this.sessionEndTime = sessionEndTime; }
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        public Integer getEstimatedConsultationMinutes() { return estimatedConsultationMinutes; }
        public void setEstimatedConsultationMinutes(Integer estimatedConsultationMinutes) { this.estimatedConsultationMinutes = estimatedConsultationMinutes; }
    }
}
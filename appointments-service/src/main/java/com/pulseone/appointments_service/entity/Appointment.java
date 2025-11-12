package com.pulseone.appointments_service.entity;

import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.enums.AppointmentType;
import com.pulseone.appointments_service.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an appointment booked by a patient with a doctor.
 * Links to sessions and manages the queue position and status.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "appointment_id")
    private UUID appointmentId;

    /**
     * Patient ID from the auth service
     */
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    /**
     * Doctor ID from the auth service
     */
    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    /**
     * Reference to the clinic (nullable for virtual appointments)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    /**
     * Reference to the session this appointment belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /**
     * Date of the appointment
     */
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    /**
     * Queue number for this appointment within the session
     */
    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    /**
     * Type of appointment (virtual or in-person)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false)
    private AppointmentType appointmentType;

    /**
     * Current status of the appointment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    /**
     * Patient's chief complaint or reason for visit
     */
    @Column(name = "chief_complaint", length = 1000)
    private String chiefComplaint;

    /**
     * Consultation fee for this appointment
     */
    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    /**
     * Payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Payment ID from payment service
     */
    @Column(name = "payment_id")
    private String paymentId;

    /**
     * Notes added by doctor during/after consultation
     */
    @Column(name = "doctor_notes", length = 2000)
    private String doctorNotes;

    /**
     * Estimated start time based on queue position
     */
    @Column(name = "estimated_start_time")
    private LocalDateTime estimatedStartTime;

    /**
     * Actual consultation start time
     */
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    /**
     * Actual consultation end time
     */
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Appointment() {
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

    public Clinic getClinic() {
        return clinic;
    }

    public void setClinic(Clinic clinic) {
        this.clinic = clinic;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
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

    public Long getClinicId() {
        return clinic != null ? clinic.getId() : null;
    }

    public Long getSessionId() {
        return session != null ? session.getId() : null;
    }
}
package com.pulseone.appointments_service.entity;

import com.pulseone.appointments_service.enums.AppointmentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks all status changes for appointments to maintain audit trail.
 * This provides complete history of appointment lifecycle.
 */
@Entity
@Table(name = "appointment_history")
public class AppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    private UUID historyId;

    /**
     * Reference to the appointment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    /**
     * Previous status (null for initial creation)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private AppointmentStatus previousStatus;

    /**
     * New status after the change
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private AppointmentStatus newStatus;

    /**
     * Reason for status change
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * Who made the change (patient ID, doctor ID, or system)
     */
    @Column(name = "changed_by")
    private String changedBy;

    /**
     * Type of user who made the change (PATIENT, DOCTOR, SYSTEM, ADMIN)
     */
    @Column(name = "changed_by_type")
    private String changedByType;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    // Constructors
    public AppointmentHistory() {
    }

    public AppointmentHistory(Appointment appointment, AppointmentStatus previousStatus, 
                            AppointmentStatus newStatus, String changeReason, 
                            String changedBy, String changedByType) {
        this.appointment = appointment;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
        this.changedByType = changedByType;
    }

    // Getters and Setters
    public UUID getHistoryId() {
        return historyId;
    }

    public void setHistoryId(UUID historyId) {
        this.historyId = historyId;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public AppointmentStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(AppointmentStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public AppointmentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AppointmentStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangedByType() {
        return changedByType;
    }

    public void setChangedByType(String changedByType) {
        this.changedByType = changedByType;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
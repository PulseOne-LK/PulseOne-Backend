package com.pulseone.appointments_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing patients in the waiting room who have checked in
 * Tracks the journey from check-in through consultation start
 */
@Entity
@Table(name = "waiting_room")
public class WaitingRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "waiting_room_id", columnDefinition = "UUID")
    private UUID waitingRoomId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;
    
    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;
    
    @Column(name = "called_at")
    private LocalDateTime calledAt;
    
    @Column(name = "called_by")
    private String calledBy;
    
    @Column(name = "consultation_started_at")
    private LocalDateTime consultationStartedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WaitingRoom() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WaitingRoom(Appointment appointment) {
        this();
        this.appointment = appointment;
        this.checkedInAt = LocalDateTime.now();
    }
    
    public WaitingRoom(Appointment appointment, LocalDateTime checkedInAt) {
        this();
        this.appointment = appointment;
        this.checkedInAt = checkedInAt;
    }
    
    // Lifecycle callbacks
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    
    /**
     * Mark patient as called for consultation
     */
    public void callPatient(String staffName) {
        this.calledAt = LocalDateTime.now();
        this.calledBy = staffName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Mark consultation as started
     */
    public void startConsultation() {
        if (this.calledAt == null) {
            throw new IllegalStateException("Patient must be called before starting consultation");
        }
        this.consultationStartedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate total wait time from check-in to being called
     */
    public long getWaitTimeMinutes() {
        if (checkedInAt == null) return 0;
        
        LocalDateTime endTime = calledAt != null ? calledAt : LocalDateTime.now();
        return java.time.Duration.between(checkedInAt, endTime).toMinutes();
    }
    
    /**
     * Check if patient has been called
     */
    public boolean isCalled() {
        return calledAt != null;
    }
    
    /**
     * Check if consultation has started
     */
    public boolean isConsultationStarted() {
        return consultationStartedAt != null;
    }
    
    // Getters and Setters
    public UUID getWaitingRoomId() {
        return waitingRoomId;
    }
    
    public void setWaitingRoomId(UUID waitingRoomId) {
        this.waitingRoomId = waitingRoomId;
    }
    
    public Appointment getAppointment() {
        return appointment;
    }
    
    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
    
    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
    
    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
    
    public LocalDateTime getCalledAt() {
        return calledAt;
    }
    
    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }
    
    public String getCalledBy() {
        return calledBy;
    }
    
    public void setCalledBy(String calledBy) {
        this.calledBy = calledBy;
    }
    
    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }
    
    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
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
        return "WaitingRoom{" +
                "waitingRoomId=" + waitingRoomId +
                ", appointmentId=" + (appointment != null ? appointment.getAppointmentId() : null) +
                ", checkedInAt=" + checkedInAt +
                ", calledAt=" + calledAt +
                ", calledBy='" + calledBy + '\'' +
                ", consultationStartedAt=" + consultationStartedAt +
                '}';
    }
}
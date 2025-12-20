package com.pulseone.profile_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents the association between a clinic and a doctor.
 * This entity allows doctors to be added to clinics by clinic admins.
 * Doctors must confirm their association before being fully linked to the
 * clinic.
 */
@Entity
@Table(name = "clinic_doctors", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "clinic_id", "doctor_uuid" })
})
public class ClinicDoctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign Key to the Clinic entity.
     * Identifies which clinic the doctor is being added to.
     */
    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    /**
     * Doctor's User ID from the Auth Service.
     * Identifies the doctor being added to the clinic.
     */
    @Column(name = "doctor_uuid", nullable = false)
    private String doctorUserId;

    /**
     * Indicates whether the doctor has confirmed the clinic association.
     * Default is false when the clinic admin adds the doctor.
     * Changes to true when the doctor confirms on their dashboard.
     */
    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = Boolean.FALSE;

    /**
     * Timestamp when the clinic admin added the doctor.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the doctor confirmed the association.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // Constructors
    public ClinicDoctor() {
    }

    public ClinicDoctor(Long clinicId, String doctorUserId) {
        this.clinicId = clinicId;
        this.doctorUserId = doctorUserId;
        this.isConfirmed = Boolean.FALSE;
        this.createdAt = LocalDateTime.now();
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClinicId() {
        return clinicId;
    }

    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
    }

    public String getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(String doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public Boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setIsConfirmed(Boolean confirmed) {
        isConfirmed = confirmed;
        if (confirmed && this.confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}

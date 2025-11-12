package com.pulseone.appointments_service.entity;

import jakarta.persistence.*;

/**
 * Represents a simplified doctor entity for appointments service.
 * This contains basic information needed for session management.
 * Full doctor details are maintained in the profile-service.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign Key to the Auth Service User ID.
     * Links to the doctor's identity in the auth system.
     */
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    /**
     * Doctor's name for display purposes
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Medical specialization (e.g., "Cardiology", "General Practice")
     */
    @Column(name = "specialization", nullable = false)
    private String specialization;

    /**
     * Whether the doctor is currently active and accepting appointments
     */
    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    /**
     * Reference to the clinic where the doctor works (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    // Constructors
    public Doctor() {
    }

    public Doctor(String userId, String name, String specialization) {
        this.userId = userId;
        this.name = name;
        this.specialization = specialization;
        this.isActive = Boolean.TRUE;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Clinic getClinic() {
        return clinic;
    }

    public void setClinic(Clinic clinic) {
        this.clinic = clinic;
    }
}
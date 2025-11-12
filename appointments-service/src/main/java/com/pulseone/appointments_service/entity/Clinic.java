package com.pulseone.appointments_service.entity;

import jakarta.persistence.*;

/**
 * Represents a simplified clinic entity for appointments service.
 * This contains basic information needed for session management.
 * Full clinic details are maintained in the profile-service.
 */
@Entity
@Table(name = "clinics")
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Clinic name for display purposes
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Physical address of the clinic
     */
    @Column(name = "address", nullable = false)
    private String address;

    /**
     * Whether the clinic is currently operational
     */
    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    // Constructors
    public Clinic() {
    }

    public Clinic(String name, String address) {
        this.name = name;
        this.address = address;
        this.isActive = Boolean.TRUE;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
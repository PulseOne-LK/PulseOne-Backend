package com.pulseone.profile_service.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Stores non-clinical demographic and contact details for the PATIENT role.
 */
@Entity
@Table(name = "patient_profile")
public class PatientProfile {

    // Primary Key for the table itself
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CRITICAL: Foreign Key to the Auth Service User ID
    // This column MUST be unique, as one Auth user can only have one profile.
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    // --- Personal Details ---
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dob;

    // --- Billing & Emergency ---
    @Column(name = "insurance_provider")
    private String insuranceProvider;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    // --- Basic Health Info (Non-PHI) ---
    @Column(name = "known_allergies")
    private String knownAllergies;

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(String insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getKnownAllergies() {
        return knownAllergies;
    }

    public void setKnownAllergies(String knownAllergies) {
        this.knownAllergies = knownAllergies;
    }
}

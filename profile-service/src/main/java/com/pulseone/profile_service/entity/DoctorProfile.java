package com.pulseone.profile_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Stores professional details for the DOCTOR role.
 */
@Entity
@Table(name = "doctor_profile")
public class DoctorProfile {

    // Primary Key for the table itself
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CRITICAL: Foreign Key to the Auth Service User ID
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    // --- Professional Details ---
    @Column(name = "specialty", nullable = false)
    private String specialty;

    @Column(name = "consultation_fee", nullable = false)
    private BigDecimal consultationFee;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "bio", length = 500)
    private String bio;

    // --- Telemedicine & Compliance ---
    @Column(name = "telecom_url")
    private String telecomUrl; // Base URL for the virtual consultation room

    @Column(name = "license_photo_url")
    private String licensePhotoUrl; // Link to the stored document

    @Column(name = "is_virtual")
    private Boolean isVirtual;

    // --- Administrative ---
    @Column(name = "is_verified")
    private Boolean isVerified = Boolean.FALSE;

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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTelecomUrl() {
        return telecomUrl;
    }

    public void setTelecomUrl(String telecomUrl) {
        this.telecomUrl = telecomUrl;
    }

    public String getLicensePhotoUrl() {
        return licensePhotoUrl;
    }

    public void setLicensePhotoUrl(String licensePhotoUrl) {
        this.licensePhotoUrl = licensePhotoUrl;
    }

    public Boolean getVirtual() {
        return isVirtual;
    }

    public void setVirtual(Boolean virtual) {
        isVirtual = virtual;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }
}

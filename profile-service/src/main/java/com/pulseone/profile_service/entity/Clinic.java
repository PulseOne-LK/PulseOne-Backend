package com.pulseone.profile_service.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a clinic/chamber entity managed by a CLINIC_ADMIN.
 * Links to doctors and stores business details for the clinic.
 */
@Entity
@Table(name = "clinic")
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign Key to the CLINIC_ADMIN's ID from the Auth Service.
     * Links the clinic entity to its manager.
     */
    @Column(name = "admin_user_id", nullable = false)
    private String adminUserId;

    /**
     * Legal name of the clinic (e.g., "The Wellness Hub").
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The physical location of the chamber/clinic.
     * Essential for map integration and in-person bookings.
     */
    @Column(name = "physical_address", nullable = false)
    private String physicalAddress;

    /**
     * Clinic's main contact phone line.
     */
    @Column(name = "contact_phone")
    private String contactPhone;

    /**
     * Legal tax identifier for billing.
     * Critical for the Payments Service integration.
     */
    @Column(name = "tax_id")
    private String taxId;

    /**
     * General business hours (e.g., "M-F 9am-5pm").
     */
    @Column(name = "operating_hours")
    private String operatingHours;

    /**
     * Doctor associations are now managed through the ClinicDoctor entity.
     * This allows for tracking confirmation status and timestamps.
     * Previously stored as ElementCollection, now using dedicated entity.
     * This field is transient (not persisted) and populated from ClinicDoctor
     * table.
     */
    @Transient
    private List<String> doctorUuids = new ArrayList<>();

    // Constructors
    public Clinic() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(String physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    /**
     * Get all doctor associations for this clinic.
     * This list is populated from the ClinicDoctor table.
     */
    public List<String> getDoctorUuids() {
        return doctorUuids;
    }

    /**
     * Set the doctor associations for this clinic.
     * Used to populate doctors fetched from ClinicDoctor table.
     */
    public void setDoctorUuids(List<String> doctorUuids) {
        this.doctorUuids = doctorUuids;
    }
}

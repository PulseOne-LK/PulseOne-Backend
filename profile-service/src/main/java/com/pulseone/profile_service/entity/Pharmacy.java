package com.pulseone.profile_service.entity;

import jakarta.persistence.*;

/**
 * Stores the physical/legal location details for the Pharmacist role.
 */
@Entity
@Table(name = "pharmacy")
public class Pharmacy {

    // Primary Key for the table itself
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the legal dispensing business
    @Column(name = "name", nullable = false)
    private String name;

    // The official legal license number for the location (unique)
    @Column(name = "license_number", unique = true, nullable = false)
    private String licenseNumber;

    // CRITICAL: Foreign Key to the Pharmacist's Auth Service User ID
    @Column(name = "pharmacist_user_id", unique = true, nullable = false)
    private String pharmacistUserId;

    // --- Location & Logistics ---
    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "operating__hours")
    private String operatingHours;

    @Column(name = "fulfillment_radius_km")
    private Integer fulfillmentRadiusKm;

    /**
     * Latitude coordinate for pharmacy location (for geospatial queries and
     * mapping).
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Longitude coordinate for pharmacy location (for geospatial queries and
     * mapping).
     */
    @Column(name = "longitude")
    private Double longitude;

    // --- Administrative ---
    @Column(name = "is_verified")
    private Boolean isVerified = Boolean.FALSE;

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

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getPharmacistUserId() {
        return pharmacistUserId;
    }

    public void setPharmacistUserId(String pharmacistUserId) {
        this.pharmacistUserId = pharmacistUserId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    public Integer getFulfillmentRadiusKm() {
        return fulfillmentRadiusKm;
    }

    public void setFulfillmentRadiusKm(Integer fulfillmentRadiusKm) {
        this.fulfillmentRadiusKm = fulfillmentRadiusKm;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}

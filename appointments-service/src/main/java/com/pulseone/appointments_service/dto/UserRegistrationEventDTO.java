package com.pulseone.appointments_service.dto;

/**
 * DTO for user registration events from auth-service
 * Used to create appointment-related entities when users register
 */
public class UserRegistrationEventDTO {
    
    private String userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String timestamp;
    private String eventType;
    
    // Clinic-related fields for CLINIC_ADMIN users
    private String clinicName;
    private String clinicAddress;
    private String clinicPhone;
    private String clinicOperatingHours;

    // Constructors
    public UserRegistrationEventDTO() {
    }

    public UserRegistrationEventDTO(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getClinicAddress() {
        return clinicAddress;
    }

    public void setClinicAddress(String clinicAddress) {
        this.clinicAddress = clinicAddress;
    }

    public String getClinicPhone() {
        return clinicPhone;
    }

    public void setClinicPhone(String clinicPhone) {
        this.clinicPhone = clinicPhone;
    }

    public String getClinicOperatingHours() {
        return clinicOperatingHours;
    }

    public void setClinicOperatingHours(String clinicOperatingHours) {
        this.clinicOperatingHours = clinicOperatingHours;
    }

    /**
     * Get the full name of the user
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "Unknown User";
    }

    @Override
    public String toString() {
        return "UserRegistrationEventDTO{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
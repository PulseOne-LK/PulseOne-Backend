package com.pulseone.profile_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for user registration events received from Kafka
 */
public class UserRegistrationEventDTO {
    
    @JsonProperty("user_id")
    private String userId;
    
    private String email;
    
    private String role;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    private String timestamp;
    
    @JsonProperty("event_type")
    private String eventType;

    // Default constructor
    public UserRegistrationEventDTO() {}

    // Constructor with parameters
    public UserRegistrationEventDTO(String userId, String email, String role, 
                                    String firstName, String lastName, String timestamp, String eventType) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timestamp = timestamp;
        this.eventType = eventType;
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

    @Override
    public String toString() {
        return "UserRegistrationEventDTO{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
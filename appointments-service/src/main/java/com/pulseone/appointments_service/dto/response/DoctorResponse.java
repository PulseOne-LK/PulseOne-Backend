package com.pulseone.appointments_service.dto.response;

/**
 * Response DTO for doctor information
 */
public class DoctorResponse {
    
    private Long id;
    private String userId;
    private String name;
    private String specialization;
    private Boolean isActive;
    private Long clinicId;
    
    // Constructors
    public DoctorResponse() {
    }
    
    public DoctorResponse(Long id, String userId, String name, String specialization, Boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.specialization = specialization;
        this.isActive = isActive;
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
    
    public Long getClinicId() {
        return clinicId;
    }
    
    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
    }
    
    @Override
    public String toString() {
        return "DoctorResponse{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", specialization='" + specialization + '\'' +
                ", isActive=" + isActive +
                ", clinicId=" + clinicId +
                '}';
    }
}
package com.pulseone.profile_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores ratings and reviews submitted by patients for doctors.
 * Allows patients to rate their experience with doctors and provide feedback.
 */
@Entity
@Table(name = "doctor_rating")
public class DoctorRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CRITICAL: Foreign Key to the Doctor's Auth Service User ID
    @Column(name = "doctor_user_id", nullable = false)
    private String doctorUserId;

    // CRITICAL: Foreign Key to the Patient's Auth Service User ID
    @Column(name = "patient_user_id", nullable = false)
    private String patientUserId;

    // Rating value (1-5 stars)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    // Optional review text provided by the patient
    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    // Timestamp when the rating was created
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when the rating was last updated
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public DoctorRating() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DoctorRating(String doctorUserId, String patientUserId, Integer rating, String review) {
        this.doctorUserId = doctorUserId;
        this.patientUserId = patientUserId;
        this.rating = rating;
        this.review = review;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(String doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public String getPatientUserId() {
        return patientUserId;
    }

    public void setPatientUserId(String patientUserId) {
        this.patientUserId = patientUserId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.pulseone.profile_service.service;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Dedicated service for validating input data integrity and business rules.
 * This keeps the ProfileService clean and focused on orchestration.
 */
@Service
public class ValidationService {

    // --- General Validation ---

    public void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            throw new IllegalArgumentException("Phone number is required and must be valid.");
        }
        // In production: add regex check here for phone format
    }

    // --- Role-Specific Validation ---

    public void validatePatientProfile(PatientProfile profile) {
        validatePhoneNumber(profile.getPhoneNumber());
        if (profile.getDob() == null) {
            throw new IllegalArgumentException("Patient date of birth is required for scheduling.");
        }
        // Add checks for insurance format, address length, etc.
    }

    public void validateDoctorProfile(DoctorProfile profile) {
        if (profile.getSpecialty() == null || profile.getSpecialty().isEmpty()) {
            throw new IllegalArgumentException("Doctor specialty cannot be empty.");
        }
        if (profile.getConsultationFee() == null || profile.getConsultationFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Consultation fee must be set and greater than zero.");
        }
        // Add checks for license photo URL format, years of experience range, etc.
    }
}

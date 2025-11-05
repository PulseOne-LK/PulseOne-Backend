package com.pulseone.profile_service.service;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.repository.DoctorProfileRepository;
import com.pulseone.profile_service.repository.PatientProfileRepository;
import com.pulseone.profile_service.repository.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Central service for all Profile-related business logic.
 * This service layer applies validation and coordinates data access.
 */
@Service
public class ProfileService {

    private final PatientProfileRepository patientRepo;
    private final DoctorProfileRepository doctorRepo;
    private final PharmacyRepository pharmacyRepo;

    @Autowired
    public ProfileService(
            PatientProfileRepository patientRepo,
            DoctorProfileRepository doctorRepo,
            PharmacyRepository pharmacyRepo) {
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.pharmacyRepo = pharmacyRepo;
    }

    // -------------------------------------------------------------------
    // PATIENT PROFILE METHODS
    // -------------------------------------------------------------------

    /**
     * Saves or updates a patient profile.
     * @param profile The patient profile data.
     * @return The saved profile.
     */
    public PatientProfile savePatientProfile(PatientProfile profile) {
        // Business Rule: Ensure phone number is present before saving contact info
        if (profile.getPhoneNumber() == null || profile.getPhoneNumber().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is mandatory for patient profile.");
        }
        return patientRepo.save(profile);
    }

    /**
     * Retrieves a patient profile by the Auth Service User ID.
     * @param userId The ID from the JWT token.
     */
    public PatientProfile getPatientProfileByUserId(String userId) {
        return patientRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found."));
    }

    // -------------------------------------------------------------------
    // DOCTOR PROFILE METHODS
    // -------------------------------------------------------------------

    /**
     * Saves or updates a doctor profile.
     */
    public DoctorProfile saveDoctorProfile(DoctorProfile profile) {
        // Business Rule: Doctor's consultation fee must be set.
        if (profile.getConsultationFee() == null || profile.getConsultationFee().doubleValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation fee must be greater than zero.");
        }
        return doctorRepo.save(profile);
    }

    /**
     * Retrieves a doctor profile by the Auth Service User ID.
     */
    public DoctorProfile getDoctorProfileByUserId(String userId) {
        return doctorRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found."));
    }

    /**
     * Retrieves a list of all verified doctors (for the Patient facing directory).
     * NOTE: Verification status is primarily in the Auth Service, but we assume
     * only profiles with specialty data are ready to be listed.
     */
    public List<DoctorProfile> getAllDoctors() {
        return doctorRepo.findAll();
    }

    // -------------------------------------------------------------------
    // PHARMACY METHODS
    // -------------------------------------------------------------------

    /**
     * Saves or updates a Pharmacy entity (called by the Pharmacist user).
     */
    public Pharmacy savePharmacy(Pharmacy pharmacy) {
        // Business Rule: Pharmacy must have a legal license number set.
        if (pharmacy.getLicenseNumber() == null || pharmacy.getLicenseNumber().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pharmacy license number is required for registration.");
        }
        return pharmacyRepo.save(pharmacy);
    }

    /**
     * Retrieves a Pharmacy entity by the Pharmacist's Auth Service User ID.
     */
    public Pharmacy getPharmacyByPharmacistUserId(String pharmacistUserId) {
        return pharmacyRepo.findByPharmacistUserId(pharmacistUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacy not found for this user."));
    }
}

package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for managing all Profile entities (Patient, Doctor, Pharmacy).
 * Relies on JWT validation from the API Gateway which populates the custom headers.
 */
@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Utility to check if the caller's role matches the required role
    private void checkRole(String requiredRole, String actualRole) {
        if (!requiredRole.equals(actualRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Required role is " + requiredRole);
        }
    }

    // --- 1. PATIENT PROFILE ENDPOINTS ---

    /**
     * POST /profiles/patient - Creates the patient profile after registration.
     * The patient's Auth ID is taken from the header, not the request body.
     */
    @PostMapping("/patient")
    public ResponseEntity<PatientProfile> createPatientProfile(
            @RequestHeader("X-User-ID") String authUserId,
            @RequestHeader("X-User-Role") String authUserRole,
            @RequestBody PatientProfile profile) {

        checkRole("PATIENT", authUserRole);

        // Ensure the profile is created only for the authenticated user
        profile.setUserId(authUserId);

        // Check if profile already exists before saving (or use a PUT for update)
        try {
            profileService.getPatientProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e;
        }

        PatientProfile newProfile = profileService.savePatientProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * GET /profiles/patient/me - Retrieves the patient's own profile.
     */
    @GetMapping("/patient/me")
    public PatientProfile getPatientProfile(
            @RequestHeader("X-User-ID") String authUserId,
            @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PATIENT", authUserRole);

        // Lookup profile using the ID passed by the Gateway
        return profileService.getPatientProfileByUserId(authUserId);
    }

    // --- 2. DOCTOR PROFILE ENDPOINTS ---

    /**
     * POST /profiles/doctor - Creates the doctor's professional profile.
     * Accessible by DOCTOR role only.
     */
    @PostMapping("/doctor")
    public ResponseEntity<DoctorProfile> createDoctorProfile(
            @RequestHeader("X-User-ID") String authUserId,
            @RequestHeader("X-User-Role") String authUserRole,
            @RequestBody DoctorProfile profile) {

        checkRole("DOCTOR", authUserRole);
        profile.setUserId(authUserId);

        // Logic to prevent duplicate creation remains
        try {
            profileService.getDoctorProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e;
        }

        DoctorProfile newProfile = profileService.saveDoctorProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * GET /profiles/doctor/{userId} - Public endpoint for patients to view a doctor's profile.
     */
    @GetMapping("/doctor/{userId}")
    public DoctorProfile getDoctorProfilePublic(@PathVariable String userId) {
        // Any PATIENT can view any DOCTOR profile (read-only directory view)
        return profileService.getDoctorProfileByUserId(userId);
    }

    /**
     * GET /profiles/doctors - Retrieves the list of all doctors (for the directory).
     */
    @GetMapping("/doctors")
    public List<DoctorProfile> getAllDoctorsPublic() {
        return profileService.getAllDoctors();
    }


    // --- 3. PHARMACY ENDPOINTS ---

    /**
     * POST /profiles/pharmacy - Creates a Pharmacy location/business entry.
     * Accessible by PHARMACIST role only.
     */
    @PostMapping("/pharmacy")
    public ResponseEntity<Pharmacy> createPharmacy(
            @RequestHeader("X-User-ID") String authUserId,
            @RequestHeader("X-User-Role") String authUserRole,
            @RequestBody Pharmacy pharmacy) {

        checkRole("PHARMACIST", authUserRole);
        // Link the pharmacist user ID to the pharmacy business entity
        pharmacy.setPharmacistUserId(authUserId);

        // Check if pharmacy is already linked to the pharmacist
        try {
            profileService.getPharmacyByPharmacistUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pharmacist is already linked to a pharmacy entity.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e;
        }

        Pharmacy newPharmacy = profileService.savePharmacy(pharmacy);
        return new ResponseEntity<>(newPharmacy, HttpStatus.CREATED);
    }

    /**
     * GET /profiles/pharmacy/me - Retrieves the Pharmacy entity linked to the logged-in Pharmacist.
     */
    @GetMapping("/pharmacy/me")
    public Pharmacy getPharmacyByPharmacist(
            @RequestHeader("X-User-ID") String authUserId,
            @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PHARMACIST", authUserRole);
        return profileService.getPharmacyByPharmacistUserId(authUserId);
    }
}

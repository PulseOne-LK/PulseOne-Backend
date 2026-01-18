package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.service.ProfileService;
import com.pulseone.profile_service.service.DoctorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for managing all Profile entities (Patient, Doctor,
 * Pharmacy).
 * Relies on JWT validation from the API Gateway which populates the custom
 * headers.
 */
@RestController
@RequestMapping("/")
@Tag(name = "Profile Management", description = "API for managing user profiles")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final DoctorDashboardService doctorDashboardService;

    @Autowired
    public ProfileController(ProfileService profileService, DoctorDashboardService doctorDashboardService) {
        this.profileService = profileService;
        this.doctorDashboardService = doctorDashboardService;
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
    @Operation(summary = "Create patient profile", description = "Create a new patient profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile created successfully", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/patient")
    public ResponseEntity<PatientProfile> createPatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Patient profile data", required = true) @RequestBody PatientProfile profile) {

        checkRole("PATIENT", authUserRole);

        // Ensure the profile is created only for the authenticated user
        profile.setUserId(authUserId);

        // Check if profile already exists before saving (or use a PUT for update)
        try {
            profileService.getPatientProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Patient profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        PatientProfile newProfile = profileService.savePatientProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/patient - Updates the patient's profile.
     */
    @Operation(summary = "Update patient profile", description = "Update the authenticated patient's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile updated successfully", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/patient")
    public PatientProfile updatePatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Patient profile update data", required = true) @RequestBody PatientProfile updates) {
        checkRole("PATIENT", authUserRole);
        return profileService.updatePatientProfile(authUserId, updates);
    }

    /**
     * GET /profiles/patient/me - Retrieves the patient's own profile.
     */
    @Operation(summary = "Get own patient profile", description = "Retrieve the authenticated patient's own profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile found", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/patient/me")
    public PatientProfile getPatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PATIENT", authUserRole);

        // Lookup profile using the ID passed by the Gateway
        return profileService.getPatientProfileByUserId(authUserId);
    }

    // --- 2. DOCTOR PROFILE ENDPOINTS ---

    /**
     * GET /doctors?clinicId=4 - Retrieves doctors by clinic ID.
     */
    @Operation(summary = "Get doctors by clinic ID", description = "Retrieve list of doctors for a given clinic ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of doctors retrieved successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class)))
    })
    @GetMapping("/doctors")
    public List<DoctorProfile> getDoctorsByClinicId(@RequestParam(value = "clinicId", required = false) Long clinicId) {
        if (clinicId != null) {
            return profileService.getDoctorsByClinicId(clinicId);
        } else {
            return profileService.getAllDoctors();
        }
    }

    /**
     * POST /profiles/doctor - Creates the doctor's professional profile.
     * Accessible by DOCTOR role only.
     */
    @Operation(summary = "Create doctor profile", description = "Create a new doctor profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor profile created successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "409", description = "Doctor profile already exists"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/doctor")
    public ResponseEntity<DoctorProfile> createDoctorProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Doctor profile data", required = true) @RequestBody DoctorProfile profile) {

        checkRole("DOCTOR", authUserRole);
        profile.setUserId(authUserId);

        // Logic to prevent duplicate creation remains
        try {
            profileService.getDoctorProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        DoctorProfile newProfile = profileService.saveDoctorProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/doctor - Updates the doctor's profile.
     */
    @Operation(summary = "Update doctor profile", description = "Update the authenticated doctor's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile updated successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/doctor")
    public DoctorProfile updateDoctorProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Doctor profile update data", required = true) @RequestBody DoctorProfile updates) {
        checkRole("DOCTOR", authUserRole);
        return profileService.updateDoctorProfile(authUserId, updates);
    }

    /**
     * GET /profiles/doctor/{userId} - Public endpoint for patients to view a
     * doctor's profile.
     */
    @Operation(summary = "Get doctor profile by user ID", description = "Retrieve doctor profile information for public viewing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile found", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found")
    })
    @GetMapping("/doctor/{userId}")
    public DoctorProfile getDoctorProfilePublic(
            @Parameter(description = "User ID of the doctor", required = true) @PathVariable String userId) {
        // Any PATIENT can view any DOCTOR profile (read-only directory view)
        return profileService.getDoctorProfileByUserId(userId);
    }

    /**
     * GET /doctor/me - Retrieves the doctor's own profile.
     */
    @Operation(summary = "Get own doctor profile", description = "Retrieve the authenticated doctor's own profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile found", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/doctor/me")
    public DoctorProfile getDoctorProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("DOCTOR", authUserRole);
        return profileService.getDoctorProfileByUserId(authUserId);
    }

    // --- 2.5 DOCTOR DASHBOARD & CLINIC CONFIRMATION ENDPOINTS ---

    /**
     * GET /doctor/dashboard/pending-clinics - Retrieves all pending clinic
     * confirmations for a doctor.
     * Used when doctor logs into their dashboard to see which clinics have added
     * them.
     */
    @Operation(summary = "Get pending clinic confirmations", description = "Retrieve list of clinics that have added the authenticated doctor, awaiting confirmation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending clinics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping("/doctor/dashboard/pending-clinics")
    public List<DoctorDashboardService.PendingClinicDTO> getPendingClinics(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("DOCTOR", authUserRole);
        return doctorDashboardService.getPendingClinicConfirmations(authUserId);
    }

    /**
     * POST /doctor/dashboard/confirm-clinic/{clinicDoctorId} - Doctor confirms
     * clinic association.
     * When the doctor confirms, the clinic ID is saved to their doctor profile.
     */
    @Operation(summary = "Confirm clinic association", description = "Confirm and associate the clinic with the authenticated doctor's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinic association confirmed successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Clinic association not found")
    })
    @PostMapping("/doctor/dashboard/confirm-clinic/{clinicDoctorId}")
    public DoctorProfile confirmClinicAssociation(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Clinic-doctor association ID", required = true) @PathVariable Long clinicDoctorId) {

        checkRole("DOCTOR", authUserRole);
        return doctorDashboardService.confirmClinicAssociation(authUserId, clinicDoctorId);
    }

    /**
     * DELETE /doctor/dashboard/reject-clinic/{clinicDoctorId} - Doctor rejects
     * clinic association.
     * Removes the clinic from the pending list without saving clinic ID to profile.
     */
    @Operation(summary = "Reject clinic association", description = "Reject and remove clinic association for the authenticated doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Clinic association rejected successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Clinic association not found")
    })
    @DeleteMapping("/doctor/dashboard/reject-clinic/{clinicDoctorId}")
    public ResponseEntity<Void> rejectClinicAssociation(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Clinic-doctor association ID", required = true) @PathVariable Long clinicDoctorId) {

        checkRole("DOCTOR", authUserRole);
        doctorDashboardService.rejectClinicAssociation(authUserId, clinicDoctorId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // --- 3. PHARMACY ENDPOINTS ---

    /**
     * POST /profiles/pharmacy - Creates a Pharmacy location/business entry.
     * Accessible by PHARMACIST role only.
     */
    @Operation(summary = "Create pharmacy", description = "Create a new pharmacy for the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pharmacy created successfully", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "409", description = "Pharmacy already exists"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/pharmacy")
    public ResponseEntity<Pharmacy> createPharmacy(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Pharmacy data", required = true) @RequestBody Pharmacy pharmacy) {

        checkRole("PHARMACIST", authUserRole);
        // Link the pharmacist user ID to the pharmacy business entity
        pharmacy.setPharmacistUserId(authUserId);

        // Check if pharmacy is already linked to the pharmacist
        try {
            profileService.getPharmacyByPharmacistUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This pharmacist is already linked to a pharmacy entity.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        Pharmacy newPharmacy = profileService.savePharmacy(pharmacy);
        return new ResponseEntity<>(newPharmacy, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/pharmacy - Updates the Pharmacy linked to the logged-in
     * Pharmacist.
     */
    @Operation(summary = "Update pharmacy", description = "Update the pharmacy linked to the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy updated successfully", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/pharmacy")
    public Pharmacy updatePharmacy(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Pharmacy update data", required = true) @RequestBody Pharmacy updates) {
        checkRole("PHARMACIST", authUserRole);
        return profileService.updatePharmacy(authUserId, updates);
    }

    /**
     * GET /profiles/pharmacy/me - Retrieves the Pharmacy entity linked to the
     * logged-in Pharmacist.
     */
    @Operation(summary = "Get own pharmacy", description = "Retrieve the pharmacy linked to the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy found", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Pharmacy not found")
    })
    @GetMapping("/pharmacy/me")
    public Pharmacy getPharmacyByPharmacist(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PHARMACIST", authUserRole);
        return profileService.getPharmacyByPharmacistUserId(authUserId);
    }

    /**
     * GET /profiles/pharmacy/{pharmacyId} - Get Pharmacy by its ID (public or admin
     * use).
     */
    @Operation(summary = "Get pharmacy by ID", description = "Retrieve pharmacy details by pharmacy ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy found", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "404", description = "Pharmacy not found")
    })
    @GetMapping("/pharmacy/{pharmacyId}")
    public Pharmacy getPharmacyById(
            @Parameter(description = "Pharmacy ID", required = true) @PathVariable Long pharmacyId) {
        return profileService.getPharmacyById(pharmacyId);
    }

    // --- 4. CLINIC DISCOVERY ENDPOINTS (For Patients) ---

    /**
     * GET /clinics - Get all available clinics for patient discovery
     * Public endpoint for patients to browse all clinics
     */
    @Operation(summary = "Get all clinics", description = "Retrieve list of all available clinics for patient discovery")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of clinics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/clinics")
    public List<Clinic> getAllClinics() {
        return profileService.getAllClinics();
    }

    /**
     * GET /clinics/search?query={searchTerm} - Search clinics by name, address, or
     * specialty
     * Allows patients to search for specific clinics
     */
    @Operation(summary = "Search clinics", description = "Search for clinics by name, address, or specialty")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinics matching search criteria retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search query")
    })
    @GetMapping("/clinics/search")
    public List<Clinic> searchClinics(
            @Parameter(description = "Search query (clinic name, address, or specialty)", required = true) @RequestParam String query) {
        return profileService.searchClinics(query);
    }

    /**
     * GET /clinics/nearby?latitude={lat}&longitude={lng}&radius={km} - Get nearby
     * clinics
     * Returns clinics within specified radius from given coordinates
     */
    @Operation(summary = "Get nearby clinics", description = "Retrieve clinics within a specified radius from given coordinates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nearby clinics retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius")
    })
    @GetMapping("/clinics/nearby")
    public List<Clinic> getNearByClinics(
            @Parameter(description = "Latitude coordinate", required = true) @RequestParam Double latitude,
            @Parameter(description = "Longitude coordinate", required = true) @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers (default: 5)", required = false) @RequestParam(defaultValue = "5") Double radiusKm) {
        return profileService.getNearByClinics(latitude, longitude, radiusKm);
    }

    // --- 5. PHARMACY DISCOVERY ENDPOINTS (For Patients) ---

    /**
     * GET /pharmacies - Get all available pharmacies for patient discovery
     * Public endpoint for patients to browse all pharmacies
     */
    @Operation(summary = "Get all pharmacies", description = "Retrieve list of all available pharmacies for patient discovery")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pharmacies retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pharmacies")
    public List<Pharmacy> getAllPharmacies() {
        return profileService.getAllPharmacies();
    }

    /**
     * GET /pharmacies/search?query={searchTerm} - Search pharmacies by name or
     * address
     * Allows patients to search for specific pharmacies
     */
    @Operation(summary = "Search pharmacies", description = "Search for pharmacies by name or address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacies matching search criteria retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search query")
    })
    @GetMapping("/pharmacies/search")
    public List<Pharmacy> searchPharmacies(
            @Parameter(description = "Search query (pharmacy name or address)", required = true) @RequestParam String query) {
        return profileService.searchPharmacies(query);
    }

    /**
     * GET /pharmacies/nearby?latitude={lat}&longitude={lng}&radius={km} - Get
     * nearby pharmacies
     * Returns pharmacies within specified radius from given coordinates
     */
    @Operation(summary = "Get nearby pharmacies", description = "Retrieve pharmacies within a specified radius from given coordinates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nearby pharmacies retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius")
    })
    @GetMapping("/pharmacies/nearby")
    public List<Pharmacy> getNearByPharmacies(
            @Parameter(description = "Latitude coordinate", required = true) @RequestParam Double latitude,
            @Parameter(description = "Longitude coordinate", required = true) @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers (default: 5)", required = false) @RequestParam(defaultValue = "5") Double radiusKm) {
        return profileService.getNearByPharmacies(latitude, longitude, radiusKm);
    }

    // --- 6. DOCTOR RATINGS (Public Endpoints) ---

    /**
     * GET /doctor/{userId}/ratings - Get all ratings for a doctor
     * Public endpoint for patients to view doctor ratings and reviews
     */
    @Operation(summary = "Get doctor ratings", description = "Retrieve all ratings and reviews for a specific doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor ratings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping("/doctor/{userId}/ratings")
    public List<com.pulseone.profile_service.entity.DoctorRating> getDoctorRatings(
            @Parameter(description = "User ID of the doctor", required = true) @PathVariable String userId) {
        return profileService.getDoctorRatings(userId);
    }

    /**
     * POST /doctor/{userId}/ratings - Submit a rating for a doctor
     * Authenticated patients can submit ratings and reviews for doctors they've
     * visited
     */
    @Operation(summary = "Submit doctor rating", description = "Submit a rating and review for a doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rating submitted successfully", content = @Content(schema = @Schema(implementation = com.pulseone.profile_service.entity.DoctorRating.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rating data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PostMapping("/doctor/{userId}/ratings")
    public ResponseEntity<com.pulseone.profile_service.entity.DoctorRating> submitDoctorRating(
            @Parameter(description = "User ID of the doctor", required = true) @PathVariable String userId,
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String patientUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String userRole,
            @Parameter(description = "Rating data", required = true) @RequestBody com.pulseone.profile_service.entity.DoctorRating rating) {

        checkRole("PATIENT", userRole);
        return new ResponseEntity<>(profileService.submitDoctorRating(userId, patientUserId, rating),
                HttpStatus.CREATED);
    }
}

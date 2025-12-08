package com.pulseone.profile_service.service;

import com.pulseone.profile_service.client.AppointmentsServiceClient;
import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.repository.ClinicRepository;
import com.pulseone.profile_service.repository.DoctorProfileRepository;
import com.pulseone.profile_service.repository.PatientProfileRepository;
import com.pulseone.profile_service.repository.PharmacyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final PatientProfileRepository patientRepo;
    private final DoctorProfileRepository doctorRepo;
    private final PharmacyRepository pharmacyRepo;
    private final ClinicRepository clinicRepo;
    private final AppointmentsServiceClient appointmentsServiceClient;

    public ProfileService(
            PatientProfileRepository patientRepo,
            DoctorProfileRepository doctorRepo,
            PharmacyRepository pharmacyRepo,
            ClinicRepository clinicRepo,
            AppointmentsServiceClient appointmentsServiceClient) {
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.clinicRepo = clinicRepo;
        this.appointmentsServiceClient = appointmentsServiceClient;
    }

    // -------------------------------------------------------------------
    // PATIENT PROFILE METHODS
    // -------------------------------------------------------------------

    /**
     * Saves or updates a patient profile.
     * 
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
     * Updates the existing patient profile for the given user ID.
     */
    public PatientProfile updatePatientProfile(String userId, PatientProfile updates) {
        PatientProfile existing = getPatientProfileByUserId(userId);
        existing.setPhoneNumber(updates.getPhoneNumber());
        existing.setAddress(updates.getAddress());
        existing.setDob(updates.getDob());
        existing.setInsuranceProvider(updates.getInsuranceProvider());
        existing.setEmergencyContact(updates.getEmergencyContact());
        existing.setKnownAllergies(updates.getKnownAllergies());
        return savePatientProfile(existing);
    }

    /**
     * Retrieves a patient profile by the Auth Service User ID.
     * 
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
     * Updates the existing doctor profile for the given user ID.
     */
    public DoctorProfile updateDoctorProfile(String userId, DoctorProfile updates) {
        DoctorProfile existing = getDoctorProfileByUserId(userId);
        existing.setSpecialty(updates.getSpecialty());
        existing.setConsultationFee(updates.getConsultationFee());
        existing.setYearsOfExperience(updates.getYearsOfExperience());
        existing.setBio(updates.getBio());
        existing.setTelecomUrl(updates.getTelecomUrl());
        existing.setLicensePhotoUrl(updates.getLicensePhotoUrl());
        existing.setVirtual(updates.getVirtual());
        return saveDoctorProfile(existing);
    }

    /**
     * Retrieves a doctor profile by the Auth Service User ID.
     */
    public DoctorProfile getDoctorProfileByUserId(String userId) {
        return doctorRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found."));
    }

    /**
     * Retrieves a list of all doctors (admin and public directory).
     */
    public List<DoctorProfile> getAllDoctors() {
        return doctorRepo.findAll();
    }

    /**
     * Updates verification status of a doctor by user ID.
     */
    public DoctorProfile setDoctorVerification(String userId, boolean verified) {
        DoctorProfile doc = getDoctorProfileByUserId(userId);
        doc.setVerified(verified);
        return doctorRepo.save(doc);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pharmacy license number is required for registration.");
        }
        return pharmacyRepo.save(pharmacy);
    }

    /**
     * Updates the existing pharmacy for the pharmacist user ID.
     */
    public Pharmacy updatePharmacy(String pharmacistUserId, Pharmacy updates) {
        Pharmacy existing = getPharmacyByPharmacistUserId(pharmacistUserId);
        existing.setName(updates.getName());
        existing.setLicenseNumber(updates.getLicenseNumber());
        existing.setAddress(updates.getAddress());
        existing.setContactPhone(updates.getContactPhone());
        existing.setOperatingHours(updates.getOperatingHours());
        existing.setFulfillmentRadiusKm(updates.getFulfillmentRadiusKm());
        return savePharmacy(existing);
    }

    /**
     * Retrieves a Pharmacy entity by the Pharmacist's Auth Service User ID.
     */
    public Pharmacy getPharmacyByPharmacistUserId(String pharmacistUserId) {
        return pharmacyRepo.findByPharmacistUserId(pharmacistUserId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacy not found for this user."));
    }

    /**
     * Retrieves Pharmacy by its ID.
     */
    public Pharmacy getPharmacyById(Long id) {
        return pharmacyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacy not found."));
    }

    /**
     * Updates verification status of a pharmacy (by pharmacist user ID).
     */
    public Pharmacy setPharmacistVerification(String pharmacistUserId, boolean verified) {
        Pharmacy ph = getPharmacyByPharmacistUserId(pharmacistUserId);
        ph.setVerified(verified);
        return pharmacyRepo.save(ph);
    }

    // -------------------------------------------------------------------
    // CLINIC METHODS
    // -------------------------------------------------------------------

    /**
     * Creates a clinic for a clinic admin.
     */
    public Clinic createClinic(Clinic clinic) {
        return clinicRepo.save(clinic);
    }

    /**
     * Updates clinic fields by admin user id.
     */
    public Clinic updateClinicByAdmin(String adminUserId, Clinic updates) {
        Clinic existing = clinicRepo.findByAdminUserId(adminUserId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinic not found for this admin."));

        existing.setName(updates.getName());
        existing.setPhysicalAddress(updates.getPhysicalAddress());
        existing.setContactPhone(updates.getContactPhone());
        existing.setTaxId(updates.getTaxId());
        existing.setOperatingHours(updates.getOperatingHours());
        existing.setDoctorUuids(updates.getDoctorUuids());

        Clinic savedClinic = clinicRepo.save(existing);

        // Notify appointments service of clinic update
        try {
            appointmentsServiceClient.notifyClinicUpdated(
                    savedClinic.getId(),
                    savedClinic.getName(),
                    savedClinic.getPhysicalAddress(),
                    savedClinic.getContactPhone(),
                    savedClinic.getOperatingHours(),
                    true // isActive
            );
            logger.info("Successfully notified appointments service of clinic update for clinic ID: {}",
                    savedClinic.getId());
        } catch (Exception e) {
            logger.error("Failed to notify appointments service of clinic update: {}", e.getMessage(), e);
            // Don't fail the update if notification fails
        }

        return savedClinic;
    }

    public Clinic getClinicById(Long clinicId) {
        return clinicRepo.findById(clinicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinic not found."));
    }

    // -------------------------------------------------------------------
    // EVENT HANDLING METHODS (RabbitMQ)
    // -------------------------------------------------------------------

    /**
     * Create patient profile from user registration event
     */
    public void createPatientProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing patient registration event for user: {} (email: {})",
                    event.getUserId(), event.getEmail());
            // Profile service creates patient profile
            // TODO: Implement based on entity structure
        } catch (Exception e) {
            logger.error("Failed to create patient profile from event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create doctor profile from user registration event
     */
    public void createDoctorProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing doctor registration event for user: {} (email: {})",
                    event.getUserId(), event.getEmail());
            // Profile service creates doctor profile
            // TODO: Implement based on entity structure
        } catch (Exception e) {
            logger.error("Failed to create doctor profile from event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create pharmacist profile from user registration event
     */
    public void createPharmacistProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing pharmacist registration event for user: {} (email: {})",
                    event.getUserId(), event.getEmail());
            // Profile service creates pharmacist/pharmacy profile
            // TODO: Implement based on entity structure
        } catch (Exception e) {
            logger.error("Failed to create pharmacist profile from event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create clinic admin profile from user registration event
     */
    public void createClinicAdminProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing clinic admin registration event for clinic: {} (email: {})",
                    event.hasClinicData() ? event.getClinicData().getName() : "N/A", event.getEmail());
            // Profile service creates clinic profile
            // TODO: Implement based on entity structure
        } catch (Exception e) {
            logger.error("Failed to create clinic profile from event: {}", e.getMessage(), e);
        }
    }
}

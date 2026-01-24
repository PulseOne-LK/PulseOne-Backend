package com.pulseone.profile_service.service;

import com.pulseone.profile_service.client.AppointmentsServiceClient;
import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.entity.ClinicDoctor;
import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.DoctorRating;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.messaging.RabbitMQPublisher;
import com.pulseone.profile_service.repository.ClinicDoctorRepository;
import com.pulseone.profile_service.repository.ClinicRepository;
import com.pulseone.profile_service.repository.DoctorProfileRepository;
import com.pulseone.profile_service.repository.DoctorRatingRepository;
import com.pulseone.profile_service.repository.PatientProfileRepository;
import com.pulseone.profile_service.repository.PharmacyRepository;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    /**
     * Retrieves doctors by clinic ID.
     */
    public List<DoctorProfile> getDoctorsByClinicId(Long clinicId) {
        // Get all confirmed doctor associations for this clinic
        List<ClinicDoctor> clinicDoctors = clinicDoctorRepo.findByClinicIdAndIsConfirmedTrue(clinicId);

        if (clinicDoctors == null || clinicDoctors.isEmpty()) {
            return List.of();
        }

        return clinicDoctors.stream()
                .map(cd -> doctorRepo.findByUserId(cd.getDoctorUserId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Populates doctorUuids in clinic by fetching from ClinicDoctor table.
     * Includes both confirmed and unconfirmed associations.
     */
    private void populateClinicDoctors(Clinic clinic) {
        if (clinic == null) {
            return;
        }

        List<ClinicDoctor> allDoctors = clinicDoctorRepo.findByClinicId(clinic.getId());
        List<String> doctorUuids = allDoctors.stream()
                .map(ClinicDoctor::getDoctorUserId)
                .toList();

        clinic.setDoctorUuids(new java.util.ArrayList<>(doctorUuids));
    }

    /**
     * Synchronizes clinic-doctor associations when clinic is updated.
     * Creates ClinicDoctor entries for new doctors and removes entries for removed
     * doctors.
     * 
     * @param clinicId       The clinic ID
     * @param newDoctorUuids List of doctor user IDs that should be associated with
     *                       the clinic
     */
    private void syncClinicDoctorAssociations(Long clinicId, List<String> newDoctorUuids) {
        // Get existing associations
        List<ClinicDoctor> existingAssociations = clinicDoctorRepo.findByClinicId(clinicId);
        java.util.Set<String> existingDoctorIds = existingAssociations.stream()
                .map(ClinicDoctor::getDoctorUserId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> newDoctorIdSet = new java.util.HashSet<>(newDoctorUuids);

        // Find doctors to add (in new list but not in existing)
        java.util.Set<String> doctorsToAdd = new java.util.HashSet<>(newDoctorIdSet);
        doctorsToAdd.removeAll(existingDoctorIds);

        // Find doctors to remove (in existing but not in new list)
        java.util.Set<String> doctorsToRemove = new java.util.HashSet<>(existingDoctorIds);
        doctorsToRemove.removeAll(newDoctorIdSet);

        // Add new doctor associations
        for (String doctorUserId : doctorsToAdd) {
            ClinicDoctor newAssociation = new ClinicDoctor(clinicId, doctorUserId);
            clinicDoctorRepo.save(newAssociation);
            logger.info("Created pending clinic association for clinic {} and doctor {}", clinicId, doctorUserId);
        }

        // Remove doctor associations that are no longer in the list
        for (ClinicDoctor association : existingAssociations) {
            if (doctorsToRemove.contains(association.getDoctorUserId())) {
                clinicDoctorRepo.delete(association);
                logger.info("Removed clinic association for clinic {} and doctor {}", clinicId,
                        association.getDoctorUserId());
            }
        }

        logger.info("Synced clinic-doctor associations for clinic {}: added {}, removed {}",
                clinicId, doctorsToAdd.size(), doctorsToRemove.size());
    }

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final PatientProfileRepository patientRepo;
    private final DoctorProfileRepository doctorRepo;
    private final PharmacyRepository pharmacyRepo;
    private final ClinicRepository clinicRepo;
    private final ClinicDoctorRepository clinicDoctorRepo;
    private final DoctorRatingRepository doctorRatingRepo;
    private final AppointmentsServiceClient appointmentsServiceClient;

    @Autowired(required = false)
    private RabbitMQPublisher rabbitMQPublisher;

    public ProfileService(
            PatientProfileRepository patientRepo,
            DoctorProfileRepository doctorRepo,
            PharmacyRepository pharmacyRepo,
            ClinicRepository clinicRepo,
            ClinicDoctorRepository clinicDoctorRepo,
            DoctorRatingRepository doctorRatingRepo,
            AppointmentsServiceClient appointmentsServiceClient) {
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.clinicRepo = clinicRepo;
        this.clinicDoctorRepo = clinicDoctorRepo;
        this.doctorRatingRepo = doctorRatingRepo;
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
        if (updates.getFirstName() != null) {
            existing.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            existing.setLastName(updates.getLastName());
        }
        if (updates.getPhoneNumber() != null) {
            existing.setPhoneNumber(updates.getPhoneNumber());
        }
        if (updates.getAddress() != null) {
            existing.setAddress(updates.getAddress());
        }
        if (updates.getDob() != null) {
            existing.setDob(updates.getDob());
        }
        if (updates.getInsuranceProvider() != null) {
            existing.setInsuranceProvider(updates.getInsuranceProvider());
        }
        if (updates.getEmergencyContact() != null) {
            existing.setEmergencyContact(updates.getEmergencyContact());
        }
        if (updates.getKnownAllergies() != null) {
            existing.setKnownAllergies(updates.getKnownAllergies());
        }
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
        if (updates.getFirstName() != null) {
            existing.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            existing.setLastName(updates.getLastName());
        }
        if (updates.getSpecialty() != null) {
            existing.setSpecialty(updates.getSpecialty());
        }
        if (updates.getConsultationFee() != null) {
            existing.setConsultationFee(updates.getConsultationFee());
        }
        if (updates.getYearsOfExperience() != null) {
            existing.setYearsOfExperience(updates.getYearsOfExperience());
        }
        if (updates.getBio() != null) {
            existing.setBio(updates.getBio());
        }
        if (updates.getTelecomUrl() != null) {
            existing.setTelecomUrl(updates.getTelecomUrl());
        }
        if (updates.getLicensePhotoUrl() != null) {
            existing.setLicensePhotoUrl(updates.getLicensePhotoUrl());
        }
        if (updates.getVirtual() != null) {
            existing.setVirtual(updates.getVirtual());
        }
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
        Clinic saved = clinicRepo.save(clinic);
        populateClinicDoctors(saved);
        return saved;
    }

    /**
     * Creates a pharmacy for a pharmacist.
     */
    public Pharmacy createPharmacy(Pharmacy pharmacy) {
        return pharmacyRepo.save(pharmacy);
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

        Clinic savedClinic = clinicRepo.save(existing);

        // Process doctor associations if doctorUuids is provided
        if (updates.getDoctorUuids() != null && !updates.getDoctorUuids().isEmpty()) {
            syncClinicDoctorAssociations(savedClinic.getId(), updates.getDoctorUuids());
        }

        populateClinicDoctors(savedClinic);

        // Notify appointments service of clinic update via RabbitMQ
        try {
            if (rabbitMQPublisher != null) {
                // Create protobuf clinic update event
                UserEvents.ClinicUpdateEvent clinicEvent = UserEvents.ClinicUpdateEvent.newBuilder()
                        .setClinicId(savedClinic.getId())
                        .setName(savedClinic.getName())
                        .setAddress(savedClinic.getPhysicalAddress())
                        .setContactPhone(savedClinic.getContactPhone() != null ? savedClinic.getContactPhone() : "")
                        .setOperatingHours(
                                savedClinic.getOperatingHours() != null ? savedClinic.getOperatingHours() : "")
                        .setIsActive(true)
                        .setTimestamp(System.currentTimeMillis() / 1000)
                        .setEventType("CLINIC_UPDATED")
                        .build();

                // Publish to RabbitMQ
                rabbitMQPublisher.publishClinicUpdateEvent(clinicEvent);
                logger.info("Successfully published clinic update event to RabbitMQ for clinic ID: {}",
                        savedClinic.getId());
            } else {
                logger.warn("RabbitMQPublisher not available, cannot notify appointments service");
            }
        } catch (Exception e) {
            logger.error("Failed to publish clinic update event to RabbitMQ: {}", e.getMessage(), e);
            // Don't fail the update if notification fails
        }

        return savedClinic;
    }

    public Clinic getClinicById(Long clinicId) {
        Clinic clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinic not found."));
        populateClinicDoctors(clinic);
        return clinic;
    }

    /**
     * Retrieves a clinic by the admin user ID.
     */
    public Clinic getClinicByAdminUserId(String adminUserId) {
        Clinic clinic = clinicRepo.findByAdminUserId(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Clinic not found for this admin user."));
        populateClinicDoctors(clinic);
        return clinic;
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

            // Create a new patient profile with the user's basic info
            PatientProfile patientProfile = new PatientProfile();
            patientProfile.setUserId(event.getUserId());

            // Set phone number if available (required for savePatientProfile)
            if (!event.getPhoneNumber().isEmpty()) {
                patientProfile.setPhoneNumber(event.getPhoneNumber());
            } else {
                // Default placeholder if phone number is not provided
                patientProfile.setPhoneNumber("Not provided");
            }

            // Save the patient profile
            PatientProfile savedProfile = patientRepo.save(patientProfile);
            logger.info("Successfully created patient profile with ID: {} for user: {}",
                    savedProfile.getId(), event.getUserId());
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

            // Create a new doctor profile with the user's basic info
            DoctorProfile doctorProfile = new DoctorProfile();
            doctorProfile.setUserId(event.getUserId());

            // Set default specialty (doctor can update later)
            doctorProfile.setSpecialty("General");

            // Set default consultation fee (can be updated by doctor later)
            doctorProfile.setConsultationFee(java.math.BigDecimal.ZERO);
            doctorProfile.setVerified(false);

            // Save the doctor profile
            DoctorProfile savedProfile = doctorRepo.save(doctorProfile);
            logger.info("Successfully created doctor profile with ID: {} for user: {}",
                    savedProfile.getId(), event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to create doctor profile from event: {}", e.getMessage(), e);
        }
    }

    /**
     * Create pharmacist profile from user registration event
     */
    public void createPharmacistProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing pharmacist registration event for pharmacy: {} (email: {})",
                    event.hasPharmacyData() ? event.getPharmacyData().getName() : "N/A", event.getEmail());

            // Check if pharmacy already exists for this pharmacist
            if (pharmacyRepo.findByPharmacistUserId(event.getUserId()).isPresent()) {
                logger.info("Pharmacy already exists for pharmacist user: {}", event.getUserId());
                return;
            }

            // Create pharmacy from the event's pharmacy data (similar to clinic creation)
            if (event.hasPharmacyData()) {
                events.v1.UserEvents.PharmacyData pharmacyData = event.getPharmacyData();

                Pharmacy pharmacy = new Pharmacy();
                pharmacy.setPharmacistUserId(event.getUserId());
                pharmacy.setName(pharmacyData.getName());
                pharmacy.setAddress(pharmacyData.getAddress());

                // Generate unique license number: use event user ID with "PENDING" prefix to
                // ensure uniqueness
                String licenseNumber = pharmacyData.getLicenseNumber();
                if (licenseNumber.isEmpty() || licenseNumber.equals("PENDING")) {
                    licenseNumber = "PENDING-" + event.getUserId();
                }
                pharmacy.setLicenseNumber(licenseNumber);

                pharmacy.setContactPhone(pharmacyData.getContactPhone());
                pharmacy.setOperatingHours(pharmacyData.getOperatingHours());
                if (pharmacyData.getFulfillmentRadiusKm() > 0) {
                    pharmacy.setFulfillmentRadiusKm((int) pharmacyData.getFulfillmentRadiusKm());
                }
                pharmacy.setVerified(false);

                Pharmacy savedPharmacy = createPharmacy(pharmacy);
                logger.info("Successfully created pharmacy with ID: {} for pharmacist user: {}",
                        savedPharmacy.getId(), event.getUserId());

                // Optionally publish RabbitMQ event for other services
                // (can be implemented for future integrations like inventory service)
            } else {
                logger.warn("Pharmacy data not provided in registration event for user: {}", event.getUserId());
                // Fall back to creating a basic pharmacy profile with minimal data
                Pharmacy pharmacy = new Pharmacy();
                pharmacy.setPharmacistUserId(event.getUserId());
                pharmacy.setName(event.getFirstName().isEmpty() ? "Pharmacy" : event.getFirstName() + " Pharmacy");
                pharmacy.setAddress("Address to be provided");
                // Generate unique license number: use user ID to ensure uniqueness
                pharmacy.setLicenseNumber("PENDING-" + event.getUserId());
                pharmacy.setContactPhone(event.getPhoneNumber().isEmpty() ? "" : event.getPhoneNumber());
                pharmacy.setVerified(false);

                Pharmacy savedPharmacy = createPharmacy(pharmacy);
                logger.info("Successfully created basic pharmacy profile with ID: {} for pharmacist user: {}",
                        savedPharmacy.getId(), event.getUserId());
            }
        } catch (Exception e) {
            logger.error("Failed to create pharmacist profile from event: {}", e.getMessage(), e);
            // Swallow the exception to prevent message redelivery loop from RabbitMQ
        }
    }

    /**
     * Create clinic admin profile from user registration event
     */
    public void createClinicAdminProfileFromEvent(events.v1.UserEvents.UserRegistrationEvent event) {
        try {
            logger.info("Processing clinic admin registration event for clinic: {} (email: {})",
                    event.hasClinicData() ? event.getClinicData().getName() : "N/A", event.getEmail());

            // Create clinic from the event's clinic data
            if (event.hasClinicData()) {
                events.v1.UserEvents.ClinicData clinicData = event.getClinicData();

                Clinic clinic = new Clinic();
                clinic.setAdminUserId(event.getUserId());
                clinic.setName(clinicData.getName());
                clinic.setPhysicalAddress(clinicData.getPhysicalAddress());
                clinic.setContactPhone(clinicData.getContactPhone());
                clinic.setOperatingHours(clinicData.getOperatingHours());

                Clinic savedClinic = createClinic(clinic);
                logger.info("Successfully created clinic with ID: {} for admin user: {}",
                        savedClinic.getId(), event.getUserId());

                // Notify appointments service of clinic creation via RabbitMQ
                try {
                    if (rabbitMQPublisher != null) {
                        // Create protobuf clinic update event
                        UserEvents.ClinicUpdateEvent clinicEvent = UserEvents.ClinicUpdateEvent.newBuilder()
                                .setClinicId(savedClinic.getId())
                                .setName(savedClinic.getName())
                                .setAddress(savedClinic.getPhysicalAddress())
                                .setContactPhone(
                                        savedClinic.getContactPhone() != null ? savedClinic.getContactPhone() : "")
                                .setOperatingHours(
                                        savedClinic.getOperatingHours() != null ? savedClinic.getOperatingHours() : "")
                                .setIsActive(true)
                                .setTimestamp(System.currentTimeMillis() / 1000)
                                .setEventType("CLINIC_CREATED")
                                .build();

                        // Publish to RabbitMQ
                        rabbitMQPublisher.publishClinicUpdateEvent(clinicEvent);
                        logger.info("Successfully published clinic creation event to RabbitMQ for clinic ID: {}",
                                savedClinic.getId());
                    } else {
                        logger.warn("RabbitMQPublisher not available, cannot notify appointments service");
                    }
                } catch (Exception notificationError) {
                    logger.error("Failed to publish clinic creation event to RabbitMQ for clinic ID {}: {}",
                            savedClinic.getId(), notificationError.getMessage(), notificationError);
                    // Don't fail the clinic creation if notification fails
                }
            } else {
                logger.warn("Clinic data not provided in registration event for user: {}", event.getUserId());
            }
        } catch (Exception e) {
            logger.error("Failed to create clinic profile from event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------
    // CLINIC & PHARMACY DISCOVERY METHODS (For Patients)
    // -------------------------------------------------------------------

    /**
     * Get all available clinics for patient discovery
     */
    public List<Clinic> getAllClinics() {
        List<Clinic> clinics = clinicRepo.findAll();
        clinics.forEach(this::populateClinicDoctors);
        return clinics;
    }

    /**
     * Search clinics by name, address, or operating hours
     */
    public List<Clinic> searchClinics(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllClinics();
        }

        String searchTerm = "%" + query.toLowerCase() + "%";
        List<Clinic> clinics = clinicRepo.findAll().stream()
                .filter(clinic -> (clinic.getName() != null
                        && clinic.getName().toLowerCase().contains(query.toLowerCase())) ||
                        (clinic.getPhysicalAddress() != null
                                && clinic.getPhysicalAddress().toLowerCase().contains(query.toLowerCase()))
                        ||
                        (clinic.getOperatingHours() != null
                                && clinic.getOperatingHours().toLowerCase().contains(query.toLowerCase())))
                .toList();

        clinics.forEach(this::populateClinicDoctors);
        return clinics;
    }

    /**
     * Get nearby clinics within specified radius (in kilometers)
     * Note: This is a simplified implementation. For production, use actual
     * geospatial queries
     * Requires Clinic entity to have latitude and longitude fields
     */
    public List<Clinic> getNearByClinics(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null || radiusKm == null || radiusKm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid latitude, longitude, and radius are required.");
        }

        List<Clinic> allClinics = getAllClinics();

        // Filter clinics within the radius
        // Simplified distance calculation (Haversine formula would be more accurate)
        List<Clinic> nearbyClinics = allClinics.stream()
                .filter(clinic -> {
                    // Check if clinic has location info
                    if (clinic.getLatitude() == null || clinic.getLongitude() == null) {
                        return false;
                    }

                    // Simple distance calculation (in real app, use Haversine formula)
                    double distance = Math.sqrt(
                            Math.pow((clinic.getLatitude() - latitude) * 111.32, 2) +
                                    Math.pow((clinic.getLongitude() - longitude) * 111.32
                                            * Math.cos(Math.toRadians(latitude)), 2));

                    return distance <= radiusKm;
                })
                .toList();

        return nearbyClinics;
    }

    /**
     * Get all available pharmacies for patient discovery
     */
    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepo.findAll();
    }

    /**
     * Search pharmacies by name or address
     */
    public List<Pharmacy> searchPharmacies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPharmacies();
        }

        List<Pharmacy> pharmacies = pharmacyRepo.findAll().stream()
                .filter(pharmacy -> (pharmacy.getName() != null
                        && pharmacy.getName().toLowerCase().contains(query.toLowerCase())) ||
                        (pharmacy.getAddress() != null
                                && pharmacy.getAddress().toLowerCase().contains(query.toLowerCase())))
                .toList();

        return pharmacies;
    }

    /**
     * Get nearby pharmacies within specified radius (in kilometers)
     * Note: This is a simplified implementation. For production, use actual
     * geospatial queries
     * Requires Pharmacy entity to have latitude and longitude fields
     */
    public List<Pharmacy> getNearByPharmacies(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null || radiusKm == null || radiusKm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid latitude, longitude, and radius are required.");
        }

        List<Pharmacy> allPharmacies = getAllPharmacies();

        // Filter pharmacies within the radius
        List<Pharmacy> nearbyPharmacies = allPharmacies.stream()
                .filter(pharmacy -> {
                    // Check if pharmacy has location info
                    if (pharmacy.getLatitude() == null || pharmacy.getLongitude() == null) {
                        return false;
                    }

                    // Simple distance calculation (in real app, use Haversine formula)
                    double distance = Math.sqrt(
                            Math.pow((pharmacy.getLatitude() - latitude) * 111.32, 2) +
                                    Math.pow((pharmacy.getLongitude() - longitude) * 111.32
                                            * Math.cos(Math.toRadians(latitude)), 2));

                    return distance <= radiusKm;
                })
                .toList();

        return nearbyPharmacies;
    }

    // -------------------------------------------------------------------
    // DOCTOR RATING METHODS
    // -------------------------------------------------------------------

    /**
     * Get all ratings for a specific doctor.
     * 
     * @param doctorUserId The user ID of the doctor.
     * @return List of ratings for the doctor.
     */
    public List<DoctorRating> getDoctorRatings(String doctorUserId) {
        if (doctorUserId == null || doctorUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor user ID is required.");
        }

        // Verify the doctor exists
        DoctorProfile doctor = doctorRepo.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found."));

        return doctorRatingRepo.findByDoctorUserId(doctorUserId);
    }

    /**
     * Submit a rating for a doctor.
     * 
     * @param doctorUserId  The user ID of the doctor being rated.
     * @param patientUserId The user ID of the patient submitting the rating.
     * @param rating        The rating data.
     * @return The saved rating.
     */
    public DoctorRating submitDoctorRating(String doctorUserId, String patientUserId, DoctorRating rating) {
        if (doctorUserId == null || doctorUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor user ID is required.");
        }

        if (patientUserId == null || patientUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient user ID is required.");
        }

        if (rating.getRating() == null || rating.getRating() < 1 || rating.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5.");
        }

        // Verify the doctor exists
        DoctorProfile doctor = doctorRepo.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found."));

        // Verify the patient exists
        PatientProfile patient = patientRepo.findByUserId(patientUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found."));

        // Check if patient has already rated this doctor (prevent duplicate ratings
        // from same patient)
        var existingRating = doctorRatingRepo.findByDoctorUserIdAndPatientUserId(doctorUserId, patientUserId);
        if (existingRating.isPresent()) {
            // Update the existing rating instead of creating a new one
            DoctorRating existing = existingRating.get();
            existing.setRating(rating.getRating());
            existing.setReview(rating.getReview());
            return doctorRatingRepo.save(existing);
        }

        // Create new rating
        DoctorRating newRating = new DoctorRating(doctorUserId, patientUserId, rating.getRating(), rating.getReview());
        return doctorRatingRepo.save(newRating);
    }

    /**
     * Get the average rating for a doctor.
     * 
     * @param doctorUserId The user ID of the doctor.
     * @return The average rating (0 if no ratings exist).
     */
    public Double getAverageDoctorRating(String doctorUserId) {
        List<DoctorRating> ratings = doctorRatingRepo.findByDoctorUserId(doctorUserId);
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(DoctorRating::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Get the total number of ratings for a doctor.
     * 
     * @param doctorUserId The user ID of the doctor.
     * @return The number of ratings.
     */
    public long getDoctorRatingCount(String doctorUserId) {
        return doctorRatingRepo.findByDoctorUserId(doctorUserId).size();
    }
}

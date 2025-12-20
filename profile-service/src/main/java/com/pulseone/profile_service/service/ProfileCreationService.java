package com.pulseone.profile_service.service;

import com.pulseone.profile_service.client.AppointmentsServiceClient;
import com.pulseone.profile_service.dto.UserRegistrationEventDTO;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service to handle profile creation from user registration events
 * Automatically creates profiles based on user role
 */
@Service
public class ProfileCreationService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileCreationService.class);

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ClinicRepository clinicRepository;
    private final AppointmentsServiceClient appointmentsServiceClient;

    public ProfileCreationService(PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            PharmacyRepository pharmacyRepository,
            ClinicRepository clinicRepository,
            AppointmentsServiceClient appointmentsServiceClient) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.clinicRepository = clinicRepository;
        this.appointmentsServiceClient = appointmentsServiceClient;
    }

    /**
     * Creates appropriate profile based on user registration event
     */
    public void createProfileFromEvent(UserRegistrationEventDTO event) {
        String userId = event.getUserId();
        String role = event.getRole();

        switch (role) {
            case "PATIENT":
                createPatientProfile(event);
                break;
            case "DOCTOR":
                createDoctorProfile(event);
                break;
            case "PHARMACIST":
                createPharmacyProfile(event);
                break;
            case "CLINIC_ADMIN":
                createClinicProfile(event);
                break;
            case "SYS_ADMIN":
                logger.info("No profile creation needed for role: {} (user: {})", role, userId);
                break;
            default:
                logger.warn("Unknown user role: {} for user: {}. No profile created.", role, userId);
        }
    }

    /**
     * Creates a basic patient profile
     */
    private void createPatientProfile(UserRegistrationEventDTO event) {
        try {
            // Check if profile already exists
            if (patientProfileRepository.findByUserId(event.getUserId()).isPresent()) {
                logger.warn("Patient profile already exists for user: {}", event.getUserId());
                return;
            }

            PatientProfile profile = new PatientProfile();
            profile.setUserId(event.getUserId());

            // Set personal details from event
            profile.setFirstName(event.getFirstName());
            profile.setLastName(event.getLastName());

            // Set basic fields - phone number can be null initially
            profile.setPhoneNumber(null); // User will update this later
            profile.setAddress("");

            // Save the profile
            patientProfileRepository.save(profile);
            logger.info("Created patient profile for user: {} ({} {})", event.getUserId(),
                    event.getFirstName(), event.getLastName());

        } catch (Exception e) {
            logger.error("Error creating patient profile for user: {}", event.getUserId(), e);
        }
    }

    /**
     * Creates a basic doctor profile
     */
    private void createDoctorProfile(UserRegistrationEventDTO event) {
        try {
            // Check if profile already exists
            if (doctorProfileRepository.findByUserId(event.getUserId()).isPresent()) {
                logger.warn("Doctor profile already exists for user: {}", event.getUserId());
                return;
            }

            DoctorProfile profile = new DoctorProfile();
            profile.setUserId(event.getUserId());

            // Set personal details from event
            profile.setFirstName(event.getFirstName());
            profile.setLastName(event.getLastName());

            // Set basic required fields with placeholder values
            profile.setSpecialty("General Medicine"); // Default specialty
            profile.setConsultationFee(BigDecimal.valueOf(50.00)); // Default fee
            profile.setYearsOfExperience(0);
            profile.setVirtual(true); // Default to virtual consultations
            profile.setVerified(false); // Requires admin verification
            // clinicId will be set later when doctor confirms clinic association

            // Save the profile
            doctorProfileRepository.save(profile);
            logger.info("Created doctor profile for user: {} ({} {})", event.getUserId(),
                    event.getFirstName(), event.getLastName());

        } catch (Exception e) {
            logger.error("Error creating doctor profile for user: {}", event.getUserId(), e);
        }
    }

    /**
     * Creates a basic pharmacy profile
     */
    private void createPharmacyProfile(UserRegistrationEventDTO event) {
        try {
            // Check if profile already exists
            if (pharmacyRepository.findByPharmacistUserId(event.getUserId()).isPresent()) {
                logger.warn("Pharmacy profile already exists for user: {}", event.getUserId());
                return;
            }

            Pharmacy profile = new Pharmacy();
            profile.setPharmacistUserId(event.getUserId());

            // Set basic required fields with placeholder values
            profile.setName("New Pharmacy"); // Default name - user will update
            profile.setLicenseNumber("PENDING-" + event.getUserId()); // Temporary license number
            profile.setAddress(""); // User will need to provide
            profile.setFulfillmentRadiusKm(5); // Default radius
            profile.setVerified(false); // Requires admin verification

            // Save the profile
            pharmacyRepository.save(profile);
            logger.info("Created pharmacy profile for user: {}", event.getUserId());

        } catch (Exception e) {
            logger.error("Error creating pharmacy profile for user: {}", event.getUserId(), e);
        }
    }

    /**
     * Creates a clinic profile for clinic admin
     */
    private void createClinicProfile(UserRegistrationEventDTO event) {
        try {
            // Check if clinic already exists
            if (clinicRepository.findByAdminUserId(event.getUserId()).isPresent()) {
                logger.warn("Clinic profile already exists for admin user: {}", event.getUserId());
                return;
            }

            Clinic clinic = new Clinic();
            clinic.setAdminUserId(event.getUserId());

            // Set basic required fields from event data or defaults
            String clinicName = event.getClinicName();
            if (clinicName == null || clinicName.trim().isEmpty()) {
                clinicName = "New Clinic - " + event.getFirstName() + " " + event.getLastName();
            }
            clinic.setName(clinicName);

            String clinicAddress = event.getClinicAddress();
            if (clinicAddress == null || clinicAddress.trim().isEmpty()) {
                clinicAddress = "Address pending"; // Default placeholder
            }
            clinic.setPhysicalAddress(clinicAddress);

            // Set optional fields
            clinic.setContactPhone(event.getClinicPhone());
            clinic.setOperatingHours(event.getClinicOperatingHours());

            // Save the clinic
            Clinic savedClinic = clinicRepository.save(clinic);
            logger.info("Created clinic profile '{}' with ID {} for admin user: {}",
                    savedClinic.getName(), savedClinic.getId(), event.getUserId());

            // Notify appointments service asynchronously
            try {
                appointmentsServiceClient.notifyClinicCreated(
                        savedClinic.getId(),
                        savedClinic.getName(),
                        savedClinic.getPhysicalAddress(),
                        savedClinic.getContactPhone(),
                        savedClinic.getOperatingHours());
            } catch (Exception notificationError) {
                logger.error("Failed to notify appointments service of clinic creation: {}",
                        notificationError.getMessage(), notificationError);
                // Don't fail the clinic creation if notification fails
            }

        } catch (Exception e) {
            logger.error("Error creating clinic profile for admin user: {}", event.getUserId(), e);
        }
    }
}
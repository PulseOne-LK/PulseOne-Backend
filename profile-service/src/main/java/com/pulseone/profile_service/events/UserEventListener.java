package com.pulseone.profile_service.events;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pulseone.profile_service.config.RabbitMQConfig;
import com.pulseone.profile_service.service.ProfileService;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to user events from RabbitMQ and processes them
 */
@Service
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private ProfileService profileService;

    /**
     * Listen for user registration events
     * This method is called when a new user registers in the auth service
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    @Transactional
    public void handleUserRegistration(byte[] message) {
        try {
            // Parse the protobuf message
            UserEvents.UserRegistrationEvent event = UserEvents.UserRegistrationEvent.parseFrom(message);

            logger.info("Received user registration event for user: {} ({})", event.getUserId(), event.getEmail());

            // Process the event based on role
            processUserRegistration(event);

            logger.info("Successfully processed user registration event for user: {}", event.getUserId());

        } catch (Exception e) {
            logger.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process user registration based on their role
     */
    private void processUserRegistration(UserEvents.UserRegistrationEvent event) {
        String userId = event.getUserId();
        String email = event.getEmail();
        String role = event.getRole();

        switch (role) {
            case "PATIENT":
                logger.info("Creating patient profile for: {}", email);
                // Profile service creates patient profile
                if (profileService != null) {
                    profileService.createPatientProfileFromEvent(event);
                }
                break;

            case "DOCTOR":
                logger.info("Creating doctor profile for: {}", email);
                // Profile service creates doctor profile
                if (profileService != null) {
                    profileService.createDoctorProfileFromEvent(event);
                }
                break;

            case "PHARMACIST":
                logger.info("Creating pharmacist profile for: {}", email);
                // Profile service creates pharmacist profile
                if (profileService != null) {
                    profileService.createPharmacistProfileFromEvent(event);
                }
                break;

            case "CLINIC_ADMIN":
                logger.info("Creating clinic admin profile for clinic: {}", event.getClinicData().getName());
                // Profile service creates clinic admin profile
                if (profileService != null) {
                    profileService.createClinicAdminProfileFromEvent(event);
                }
                break;

            default:
                logger.info("User registration for role: {} - email: {}", role, email);
        }
    }

    /**
     * Listen for clinic created events from the profile service itself
     * This handles clinic creation events that may be published by other services
     */
    @RabbitListener(queues = RabbitMQConfig.CLINIC_UPDATE_QUEUE)
    @Transactional
    public void handleClinicCreatedEvent(byte[] message) {
        try {
            // Parse the protobuf message
            UserEvents.ClinicCreatedEvent event = UserEvents.ClinicCreatedEvent.parseFrom(message);

            logger.info("Received clinic created event: clinic_id={}, admin_user_id={}, clinic_name={}",
                    event.getClinicId(), event.getAdminUserId(), event.getName());

            // The clinic should already be created by the admin registration event,
            // but this event can be used for any additional processing or integration
            logger.info("Clinic created event processed for clinic: {}", event.getName());

        } catch (Exception e) {
            logger.error("Error processing clinic created event: {}", e.getMessage(), e);
        }
    }
}

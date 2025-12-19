package com.pulseone.appointments_service.events;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pulseone.appointments_service.config.RabbitMQConfig;
import com.pulseone.appointments_service.dto.UserRegistrationEventDTO;
import com.pulseone.appointments_service.service.AppointmentEventService;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to user events from RabbitMQ and processes them
 */
@Service
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    private final AppointmentEventService appointmentEventService;

    public UserEventListener(AppointmentEventService appointmentEventService) {
        this.appointmentEventService = appointmentEventService;
    }

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
            
            // Convert to DTO for processing
            UserRegistrationEventDTO dto = new UserRegistrationEventDTO();
            dto.setUserId(event.getUserId());
            dto.setEmail(event.getEmail());
            dto.setRole(event.getRole());
            dto.setFirstName(event.getFirstName());
            dto.setLastName(event.getLastName());
            dto.setPhoneNumber(event.getPhoneNumber());
            dto.setEventType(event.getEventType());
            
            // Handle clinic data if present
            if (event.hasClinicData()) {
                UserEvents.ClinicData clinicData = event.getClinicData();
                dto.setClinicName(clinicData.getName());
                dto.setClinicAddress(clinicData.getPhysicalAddress());
                dto.setClinicPhone(clinicData.getContactPhone());
                dto.setClinicOperatingHours(clinicData.getOperatingHours());
            }
            
            // Process the event using AppointmentEventService
            appointmentEventService.processUserRegistrationEvent(dto);
            
        } catch (Exception e) {
            logger.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }
}

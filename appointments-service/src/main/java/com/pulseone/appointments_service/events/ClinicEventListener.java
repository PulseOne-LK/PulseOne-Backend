package com.pulseone.appointments_service.events;

import com.pulseone.appointments_service.config.RabbitMQConfig;
import com.pulseone.appointments_service.service.ClinicSyncService;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to clinic events from RabbitMQ and processes them
 */
@Service
public class ClinicEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ClinicEventListener.class);

    private final ClinicSyncService clinicSyncService;

    public ClinicEventListener(ClinicSyncService clinicSyncService) {
        this.clinicSyncService = clinicSyncService;
    }

    /**
     * Listen for clinic update events
     * This method is called when profile service creates or updates a clinic
     */
    @RabbitListener(queues = RabbitMQConfig.CLINIC_UPDATE_QUEUE)
    @Transactional
    public void handleClinicUpdate(byte[] message) {
        try {
            // Parse the protobuf message
            UserEvents.ClinicUpdateEvent event = UserEvents.ClinicUpdateEvent.parseFrom(message);
            
            logger.info("Received clinic update event via RabbitMQ: clinicId={}, eventType={}, name={}", 
                       event.getClinicId(), event.getEventType(), event.getName());
            
            // Process the event using ClinicSyncService
            clinicSyncService.processClinicUpdateEvent(event);
            
            logger.info("Successfully processed clinic update event for clinic ID: {}", event.getClinicId());
            
        } catch (Exception e) {
            logger.error("Error processing clinic update event: {}", e.getMessage(), e);
        }
    }
}

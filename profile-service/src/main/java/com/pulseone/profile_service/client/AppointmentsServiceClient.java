package com.pulseone.profile_service.client;

import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Client for communicating with appointments service
 * Sends clinic update events when clinic data changes
 */
@Component
public class AppointmentsServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentsServiceClient.class);

    @Value("${appointments.service.url:http://appointments-service:8083}")
    private String appointmentsServiceUrl;

    private final RestTemplate restTemplate;

    public AppointmentsServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Notify appointments service when a clinic is created
     */
    public void notifyClinicCreated(Long clinicId, String name, String address, String contactPhone, String operatingHours) {
        try {
            // Create protobuf message
            UserEvents.ClinicUpdateEvent event = UserEvents.ClinicUpdateEvent.newBuilder()
                    .setClinicId(clinicId)
                    .setName(name)
                    .setAddress(address)
                    .setContactPhone(contactPhone != null ? contactPhone : "")
                    .setOperatingHours(operatingHours != null ? operatingHours : "")
                    .setIsActive(true)
                    .setTimestamp(System.currentTimeMillis() / 1000)
                    .setEventType("CLINIC_CREATED")
                    .build();

            // Send to appointments service
            sendClinicUpdateEvent(event);
            
        } catch (Exception e) {
            logger.error("Failed to notify appointments service of clinic creation: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify appointments service when a clinic is updated
     */
    public void notifyClinicUpdated(Long clinicId, String name, String address, String contactPhone, String operatingHours, Boolean isActive) {
        try {
            // Create protobuf message
            UserEvents.ClinicUpdateEvent event = UserEvents.ClinicUpdateEvent.newBuilder()
                    .setClinicId(clinicId)
                    .setName(name)
                    .setAddress(address)
                    .setContactPhone(contactPhone != null ? contactPhone : "")
                    .setOperatingHours(operatingHours != null ? operatingHours : "")
                    .setIsActive(isActive != null ? isActive : true)
                    .setTimestamp(System.currentTimeMillis() / 1000)
                    .setEventType("CLINIC_UPDATED")
                    .build();

            // Send to appointments service
            sendClinicUpdateEvent(event);
            
        } catch (Exception e) {
            logger.error("Failed to notify appointments service of clinic update: {}", e.getMessage(), e);
        }
    }

    /**
     * Send clinic update event to appointments service
     */
    private void sendClinicUpdateEvent(UserEvents.ClinicUpdateEvent event) throws Exception {
        String url = appointmentsServiceUrl + "/internal/clinic-events";
        
        // Serialize protobuf message to bytes
        byte[] eventBytes = event.toByteArray();
        
        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));
        
        // Create request entity
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(eventBytes, headers);
        
        // Send POST request
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Successfully notified appointments service of clinic event: {}", event.getEventType());
        } else {
            logger.error("Failed to notify appointments service. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to notify appointments service");
        }
    }
}
package com.pulseone.appointments_service.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Listener for video session events from the Video Consultation Service
 * Handles responses to video session creation, start, and completion
 */
@Component
public class VideoSessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(VideoSessionEventListener.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Listen for video session creation success events
     * Updates the appointment with meeting link and ID
     */
    @RabbitListener(queues = "video-session-responses-appointments")
    public void handleVideoSessionCreated(Message message) {
        try {
            // Manually convert message body to String to avoid Jackson deserialization issues
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received video session event from Video Service: {}", messageBody);
            
            JsonNode json = objectMapper.readTree(messageBody);
            String eventType = json.path("event_type").asText();
            JsonNode data = json.path("data");
            
            if ("video.session.created".equals(eventType)) {
                handleSessionCreated(data);
            } else if ("video.session.creation.failed".equals(eventType)) {
                handleSessionCreationFailed(data);
            } else if ("video.session.started".equals(eventType)) {
                log.info("Video session started: {}", data.path("session_id").asText());
            } else if ("video.session.completed".equals(eventType)) {
                log.info("Video session completed: {}", data.path("session_id").asText());
            }
            
        } catch (Exception e) {
            log.error("Error processing video session event", e);
        }
    }

    /**
     * Handle successful video session creation
     */
    private void handleSessionCreated(JsonNode data) {
        try {
            String appointmentIdStr = data.path("appointment_id").asText();
            String sessionId = data.path("session_id").asText();
            String roomId = data.path("room_id").asText();
            String meetingUrl = data.path("meeting_url").asText();

            UUID appointmentId = UUID.fromString(appointmentIdStr);

            // Update appointment with meeting link
            appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
                appointment.setMeetingId(roomId);  // Store room_id as meeting_id for compatibility
                appointment.setMeetingLink(meetingUrl);
                appointmentRepository.save(appointment);
                
                log.info("Updated appointment {} with video session: Room ID: {}, Link: {}", 
                        appointmentId, roomId, meetingUrl);
            });

        } catch (Exception e) {
            log.error("Error handling session created event", e);
        }
    }

    /**
     * Handle video session creation failure
     */
    private void handleSessionCreationFailed(JsonNode data) {
        try {
            String appointmentIdStr = data.path("appointment_id").asText();
            String error = data.path("error").asText();

            log.error("Video session creation failed for appointment: {}. Error: {}", 
                    appointmentIdStr, error);

            // Could implement retry logic or notification here

        } catch (Exception e) {
            log.error("Error handling session creation failed event", e);
        }
    }
}

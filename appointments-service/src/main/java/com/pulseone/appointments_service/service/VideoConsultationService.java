package com.pulseone.appointments_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service to interact with the Video Consultation Service via RabbitMQ
 * Handles video session creation, management through event-driven messaging
 */
@Service
public class VideoConsultationService {

    private static final Logger log = LoggerFactory.getLogger(VideoConsultationService.class);

    @Value("${rabbitmq.exchange.name:appointments-exchange}")
    private String exchangeName;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Request video session creation via RabbitMQ
     * Publishes an event that the video service will consume
     * 
     * @param appointmentId The appointment ID
     * @param doctorId Doctor's user ID
     * @param patientId Patient's user ID
     * @param scheduledTime When the consultation is scheduled
     */
    public void requestVideoSessionCreation(UUID appointmentId, String doctorId, 
                                           String patientId, LocalDateTime scheduledTime) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("appointment_id", appointmentId.toString());
            eventData.put("doctor_id", doctorId);
            eventData.put("patient_id", patientId);
            eventData.put("scheduled_time", scheduledTime.format(DateTimeFormatter.ISO_DATE_TIME));
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            
            Map<String, Object> message = new HashMap<>();
            message.put("event_type", "appointment.video.create");
            message.put("data", eventData);
            
            String routingKey = "appointment.video.create";
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            
            log.info("Published video session creation request for appointment: {} via RabbitMQ", appointmentId);
            
        } catch (Exception e) {
            log.error("Error publishing video session creation request for appointment: {}", appointmentId, e);
            throw new RuntimeException("Failed to request video session creation", e);
        }
    }

    /**
     * Request to start a video session via RabbitMQ
     */
    public void requestStartVideoSession(String sessionId, String doctorId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", sessionId);
            eventData.put("doctor_id", doctorId);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            
            Map<String, Object> message = new HashMap<>();
            message.put("event_type", "appointment.video.start");
            message.put("data", eventData);
            
            String routingKey = "appointment.video.start";
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            
            log.info("Published video session start request for session: {} via RabbitMQ", sessionId);
            
        } catch (Exception e) {
            log.error("Error publishing video session start request: {}", sessionId, e);
            throw new RuntimeException("Failed to start video session", e);
        }
    }

    /**
     * Request to end a video session via RabbitMQ
     */
    public void requestEndVideoSession(String sessionId, String userId, String endedBy) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", sessionId);
            eventData.put("ended_by", userId);
            eventData.put("ended_by_role", endedBy);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            
            Map<String, Object> message = new HashMap<>();
            message.put("event_type", "appointment.video.end");
            message.put("data", eventData);
            
            String routingKey = "appointment.video.end";
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            
            log.info("Published video session end request for session: {} via RabbitMQ", sessionId);
            
        } catch (Exception e) {
            log.error("Error publishing video session end request: {}", sessionId, e);
            throw new RuntimeException("Failed to end video session", e);
        }
    }

    // Response DTOs
    public static class VideoSessionResponse {
        private final String sessionId;
        private final String meetingLink;
        private final boolean success;
        private final String message;

        public VideoSessionResponse(String sessionId, String meetingLink, boolean success, String message) {
            this.sessionId = sessionId;
            this.meetingLink = meetingLink;
            this.success = success;
            this.message = message;
        }

        public String getSessionId() { return sessionId; }
        public String getMeetingLink() { return meetingLink; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class AttendeeResponse {
        private final String attendeeId;
        private final String joinToken;
        private final String attendeeUrl;
        private final boolean success;
        private final String message;

        public AttendeeResponse(String attendeeId, String joinToken, String attendeeUrl, boolean success, String message) {
            this.attendeeId = attendeeId;
            this.joinToken = joinToken;
            this.attendeeUrl = attendeeUrl;
            this.success = success;
            this.message = message;
        }

        public String getAttendeeId() { return attendeeId; }
        public String getJoinToken() { return joinToken; }
        public String getAttendeeUrl() { return attendeeUrl; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}

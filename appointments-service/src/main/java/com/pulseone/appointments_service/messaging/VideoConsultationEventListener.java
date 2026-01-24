package com.pulseone.appointments_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Listener for video consultation completion events from the Video Consultation Service.
 * 
 * When a doctor ends a video consultation (clinic-based booking), this listener receives
 * the completion event and updates the appointment status to COMPLETED in the database.
 * 
 * This maintains synchronization between the Video Consultation Service and Appointments Service
 * through asynchronous event messaging via RabbitMQ.
 */
@Slf4j
@Service
public class VideoConsultationEventListener {
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Listen for video consultation completion events from the Video Service.
     * 
     * Event structure:
     * {
     *   "event_type": "video.consultation.completed",
     *   "timestamp": "2026-01-25T14:30:00Z",
     *   "data": {
     *     "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
     *     "session_id": "video-session-uuid",
     *     "doctor_id": "doc_123",
     *     "patient_id": "pat_456",
     *     "duration_minutes": 30,
     *     "completed_at": "2026-01-25T14:30:00Z"
     *   }
     * }
     * 
     * @param message The RabbitMQ message containing the event
     */
    @RabbitListener(queues = "video-consultation-events-appointments")
    public void handleVideoConsultationCompleted(String message) {
        try {
            log.info("Received video consultation event from Video Service");
            
            // Parse the event message
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            
            String eventType = (String) eventData.get("event_type");
            
            // Only process consultation completed events
            if ("video.consultation.completed".equals(eventType)) {
                handleConsultationCompletion(eventData);
            } else {
                log.debug("Ignoring event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Error processing video consultation event: {}", message, e);
            // Don't rethrow - we want to acknowledge the message to avoid infinite loops
            // The message will be logged for manual review if needed
        }
    }
    
    /**
     * Process the consultation completion event and update appointment status.
     * 
     * @param eventData The parsed event data
     */
    private void handleConsultationCompletion(Map<String, Object> eventData) {
        try {
            // Extract event data
            Map<String, Object> data = (Map<String, Object>) eventData.get("data");
            
            String appointmentId = (String) data.get("appointment_id");
            Integer durationMinutes = ((Number) data.get("duration_minutes")).intValue();
            String completedAtStr = (String) data.get("completed_at");
            String doctorId = (String) data.get("doctor_id");
            String patientId = (String) data.get("patient_id");
            String sessionId = (String) data.get("session_id");
            
            log.info("Processing consultation completion for appointment: {} (session: {})", 
                    appointmentId, sessionId);
            
            // Get the appointment
            UUID appointmentUUID = UUID.fromString(appointmentId);
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentUUID);
            
            if (appointmentOpt.isEmpty()) {
                log.warn("Appointment not found: {}. This may be a direct doctor booking (not clinic-based).", 
                        appointmentId);
                return;
            }
            
            Appointment appointment = appointmentOpt.get();
            
            // Verify the appointment is linked to the correct doctor and patient
            if (!appointment.getDoctorId().equals(doctorId)) {
                log.warn("Doctor mismatch for appointment {}. Expected: {}, Got: {}", 
                        appointmentId, appointment.getDoctorId(), doctorId);
                return;
            }
            
            if (!appointment.getPatientId().equals(patientId)) {
                log.warn("Patient mismatch for appointment {}. Expected: {}, Got: {}", 
                        appointmentId, appointment.getPatientId(), patientId);
                return;
            }
            
            // Update appointment status to COMPLETED
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointment.setActualEndTime(LocalDateTime.now());
            
            // Save updated appointment
            appointmentRepository.save(appointment);
            
            log.info("Successfully updated appointment {} to COMPLETED. Consultation duration: {} minutes", 
                    appointmentId, durationMinutes);
            
        } catch (ClassCastException e) {
            log.error("Invalid event data format - type casting error", e);
        } catch (NumberFormatException e) {
            log.error("Invalid duration format in event", e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid appointment ID format: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing consultation completion event", e);
        }
    }
}

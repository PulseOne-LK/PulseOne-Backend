package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.response.AppointmentResponse;
import com.pulseone.appointments_service.service.AppointmentService;
import com.pulseone.appointments_service.service.VideoConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Video Consultation operations
 * Handles video session management for VIRTUAL appointments
 */
@RestController
@RequestMapping("/api/video-consultations")
@Tag(name = "Video Consultations", description = "Video consultation management for virtual appointments")
public class VideoConsultationController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VideoConsultationService videoConsultationService;

    /**
     * Verify payment and activate video consultation access
     * This must be called after payment is completed
     */
    @PostMapping("/{appointmentId}/verify-payment")
    @Operation(summary = "Verify payment for virtual appointment", 
               description = "Verifies payment and activates access to video consultation link")
    public ResponseEntity<?> verifyPayment(
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, String> request) {
        
        String paymentId = request.get("paymentId");
        
        if (paymentId == null || paymentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Payment ID is required"
            ));
        }
        
        try {
            AppointmentResponse response = appointmentService.verifyPaymentAndActivate(appointmentId, paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Start a video consultation session
     * Called by doctor when ready to begin consultation
     */
    @PostMapping("/{appointmentId}/start")
    @Operation(summary = "Start video consultation", 
               description = "Doctor starts the video consultation session")
    public ResponseEntity<?> startConsultation(
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, String> request) {
        
        String doctorId = request.get("doctorId");
        
        if (doctorId == null || doctorId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Doctor ID is required"
            ));
        }
        
        try {
            AppointmentResponse response = appointmentService.startVideoConsultation(appointmentId, doctorId);
            return ResponseEntity.ok(Map.of(
                "message", "Video consultation started successfully",
                "appointment", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to start video consultation: " + e.getMessage()
            ));
        }
    }

    /**
     * End a video consultation session
     * Can be called by doctor or patient
     */
    @PostMapping("/{appointmentId}/end")
    @Operation(summary = "End video consultation", 
               description = "Ends the video consultation session")
    public ResponseEntity<?> endConsultation(
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, String> request) {
        
        String userId = request.get("userId");
        String role = request.get("role"); // "DOCTOR" or "PATIENT"
        
        if (userId == null || role == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "userId and role are required"
            ));
        }
        
        try {
            AppointmentResponse response = appointmentService.endVideoConsultation(appointmentId, userId, role);
            return ResponseEntity.ok(Map.of(
                "message", "Video consultation ended successfully",
                "appointment", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get appointment details with video link
     * Patient/doctor can use this to retrieve the meeting link
     */
    @GetMapping("/{appointmentId}/details")
    @Operation(summary = "Get appointment with video details", 
               description = "Retrieve appointment details including video meeting link")
    public ResponseEntity<?> getAppointmentDetails(@PathVariable UUID appointmentId) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            
            if (appointment.getMeetingLink() == null) {
                return ResponseEntity.ok(Map.of(
                    "appointment", appointment,
                    "message", "Video session is being created. Please check again in a moment."
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "appointment", appointment,
                "meetingLink", appointment.getMeetingLink(),
                "meetingId", appointment.getMeetingId(),
                "status", appointment.getStatus(),
                "paymentStatus", appointment.getPaymentStatus()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Check video session status for an appointment
     */
    @GetMapping("/{appointmentId}/status")
    @Operation(summary = "Check video session status", 
               description = "Check if video session is ready for an appointment")
    public ResponseEntity<?> checkVideoStatus(@PathVariable UUID appointmentId) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            
            boolean videoReady = appointment.getMeetingId() != null && appointment.getMeetingLink() != null;
            
            return ResponseEntity.ok(Map.of(
                "appointmentId", appointmentId,
                "videoReady", videoReady,
                "meetingId", appointment.getMeetingId() != null ? appointment.getMeetingId() : "pending",
                "meetingLink", appointment.getMeetingLink() != null ? appointment.getMeetingLink() : "pending",
                "paymentStatus", appointment.getPaymentStatus(),
                "appointmentStatus", appointment.getStatus(),
                "message", videoReady ? "Video session is ready" : "Video session is being created"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check for video consultation controller
     */
    @GetMapping("/health")
    @Operation(summary = "Check video consultation health", 
               description = "Verify video consultation endpoints are working")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "video-consultations",
            "integration", "rabbitmq",
            "message", "Video consultation endpoints are available"
        ));
    }
}

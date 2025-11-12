package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.BookAppointmentRequest;
import com.pulseone.appointments_service.dto.response.AppointmentResponse;
import com.pulseone.appointments_service.dto.response.BookingResponse;
import com.pulseone.appointments_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing appointments.
 * Provides endpoints for booking, viewing, and cancelling appointments.
 */
@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment Management", description = "APIs for booking, viewing, and managing appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * Book a new appointment
     */
    @PostMapping("/book")
    @Operation(summary = "Book new appointment", description = "Book a new appointment with a doctor for a specific session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Appointment booked successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid booking request or validation error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Appointment conflict or session fully booked",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        try {
            BookingResponse response = appointmentService.bookAppointment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            // Handle business logic validation errors
            HttpStatus status = e.getMessage().contains("fully booked") || 
                              e.getMessage().contains("already has an appointment") ? 
                              HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while booking the appointment"));
        }
    }

    /**
     * Get all appointments for a specific patient
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient appointments", description = "Retrieve all appointments for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient appointments retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getPatientAppointments(
            @Parameter(description = "Patient's user ID from auth service", required = true)
            @PathVariable String patientId) {
        
        try {
            List<AppointmentResponse> appointments = appointmentService.getPatientAppointments(patientId);
            return ResponseEntity.ok(appointments);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while retrieving patient appointments"));
        }
    }

    /**
     * Get upcoming appointments for a specific patient
     */
    @GetMapping("/patient/{patientId}/upcoming")
    @Operation(summary = "Get patient upcoming appointments", description = "Retrieve upcoming appointments for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upcoming appointments retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getPatientUpcomingAppointments(
            @Parameter(description = "Patient's user ID from auth service", required = true)
            @PathVariable String patientId) {
        
        try {
            List<AppointmentResponse> appointments = appointmentService.getPatientUpcomingAppointments(patientId);
            return ResponseEntity.ok(Map.of(
                    "patientId", patientId,
                    "upcomingAppointments", appointments,
                    "count", appointments.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while retrieving upcoming appointments"));
        }
    }

    /**
     * Get past appointments for a specific patient
     */
    @GetMapping("/patient/{patientId}/past")
    @Operation(summary = "Get patient past appointments", description = "Retrieve past appointments for a specific patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Past appointments retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getPatientPastAppointments(
            @Parameter(description = "Patient's user ID from auth service", required = true)
            @PathVariable String patientId) {
        
        try {
            List<AppointmentResponse> appointments = appointmentService.getPatientPastAppointments(patientId);
            return ResponseEntity.ok(Map.of(
                    "patientId", patientId,
                    "pastAppointments", appointments,
                    "count", appointments.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while retrieving past appointments"));
        }
    }

    /**
     * Get appointment by ID
     */
    @GetMapping("/{appointmentId}")
    @Operation(summary = "Get appointment by ID", description = "Retrieve a specific appointment by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAppointmentById(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        try {
            return appointmentService.getAppointmentById(appointmentId)
                    .map(appointment -> ResponseEntity.ok(appointment))
                    .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while retrieving the appointment"));
        }
    }

    /**
     * Cancel an appointment
     */
    @PutMapping("/{appointmentId}/cancel")
    @Operation(summary = "Cancel appointment", description = "Cancel an existing appointment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid cancellation request",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> cancelAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId,
            
            @Parameter(description = "Reason for cancellation")
            @RequestParam(value = "reason", required = false) String reason,
            
            @Parameter(description = "ID of user cancelling the appointment")
            @RequestParam(value = "cancelledBy", required = false) String cancelledBy,
            
            @Parameter(description = "Type of user cancelling (PATIENT, DOCTOR, ADMIN, SYSTEM)")
            @RequestParam(value = "cancelledByType", defaultValue = "PATIENT") String cancelledByType) {
        
        try {
            AppointmentResponse response = appointmentService.cancelAppointment(
                    appointmentId, 
                    cancelledBy != null ? cancelledBy : "unknown", 
                    cancelledByType, 
                    reason
            );
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().contains("not found") ? 
                               HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while cancelling the appointment"));
        }
    }

    /**
     * Get appointment booking statistics (for admin/analytics)
     */
    @GetMapping("/stats")
    @Operation(summary = "Get appointment statistics", description = "Get booking statistics for analytics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAppointmentStatistics() {
        try {
            // This is a placeholder for future statistics implementation
            return ResponseEntity.ok(Map.of(
                    "message", "Statistics endpoint placeholder - to be implemented in Part 3",
                    "availableEndpoints", List.of(
                            "GET /appointments/patient/{patientId}",
                            "GET /appointments/patient/{patientId}/upcoming",
                            "GET /appointments/patient/{patientId}/past",
                            "POST /appointments/book",
                            "PUT /appointments/{appointmentId}/cancel"
                    )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while retrieving statistics"));
        }
    }
}
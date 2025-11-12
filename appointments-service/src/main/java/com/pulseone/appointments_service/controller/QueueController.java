package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.CheckInRequest;
import com.pulseone.appointments_service.dto.request.CallNextPatientRequest;
import com.pulseone.appointments_service.dto.response.QueueStatusResponse;
import com.pulseone.appointments_service.dto.response.DoctorQueueResponse;
import com.pulseone.appointments_service.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for queue management operations
 * Handles patient check-in, calling patients, and waiting room status
 */
@RestController
@RequestMapping("/api/queue")
@Tag(name = "Queue Management", description = "APIs for managing patient queues and waiting room operations")
public class QueueController {

    @Autowired
    private QueueService queueService;

    // ========================================
    // DOCTOR DASHBOARD ENDPOINTS
    // ========================================

    @GetMapping("/doctor/{doctorId}/today")
    @Operation(summary = "Get doctor's queue for today", description = "Returns today's complete queue list with statistics for a doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor's queue"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DoctorQueueResponse> getDoctorQueueToday(
            @Parameter(description = "Doctor's user ID", required = true)
            @PathVariable String doctorId) {
        
        try {
            DoctorQueueResponse queueResponse = queueService.getDoctorQueueToday(doctorId);
            return ResponseEntity.ok(queueResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/doctor/{doctorId}/date/{date}")
    @Operation(summary = "Get doctor's queue for specific date", description = "Returns complete queue list with statistics for a doctor on a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor's queue"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DoctorQueueResponse> getDoctorQueueByDate(
            @Parameter(description = "Doctor's user ID", required = true)
            @PathVariable String doctorId,
            @Parameter(description = "Date in YYYY-MM-DD format", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            DoctorQueueResponse queueResponse = queueService.getDoctorQueue(doctorId, date);
            return ResponseEntity.ok(queueResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/doctor/{doctorId}/current")
    @Operation(summary = "Get current queue status", description = "Returns only patients currently in waiting room or consultation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved current queue status"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<QueueStatusResponse>> getCurrentQueueStatus(
            @Parameter(description = "Doctor's user ID", required = true)
            @PathVariable String doctorId) {
        
        try {
            List<QueueStatusResponse> queueStatus = queueService.getCurrentQueueStatus(doctorId);
            return ResponseEntity.ok(queueStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/doctor/{doctorId}/next-patient")
    @Operation(summary = "Get next patient to call", description = "Returns the next patient in queue who should be called for consultation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Next patient found"),
            @ApiResponse(responseCode = "204", description = "No patients waiting to be called"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> getNextPatientToCall(
            @Parameter(description = "Doctor's user ID", required = true)
            @PathVariable String doctorId) {
        
        try {
            Optional<QueueStatusResponse> nextPatient = queueService.getNextPatientToCall(doctorId);
            return nextPatient.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.noContent().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========================================
    // PATIENT CHECK-IN AND QUEUE MANAGEMENT
    // ========================================

    @PutMapping("/check-in")
    @Operation(summary = "Check in patient", description = "Mark patient as arrived and add to waiting room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient successfully checked in"),
            @ApiResponse(responseCode = "400", description = "Invalid request or appointment cannot be checked in"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> checkInPatient(
            @Parameter(description = "Check-in request details", required = true)
            @RequestBody @Valid CheckInRequest request) {
        
        try {
            QueueStatusResponse response = queueService.checkInPatient(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/call-next")
    @Operation(summary = "Call next patient", description = "Call patient for consultation and update status to IN_PROGRESS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient successfully called"),
            @ApiResponse(responseCode = "400", description = "Invalid request or patient cannot be called"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> callNextPatient(
            @Parameter(description = "Call next patient request details", required = true)
            @RequestBody @Valid CallNextPatientRequest request) {
        
        try {
            QueueStatusResponse response = queueService.callNextPatient(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/appointments/{appointmentId}/start-consultation")
    @Operation(summary = "Start consultation", description = "Mark consultation as started (patient entered consultation room)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consultation successfully started"),
            @ApiResponse(responseCode = "400", description = "Consultation cannot be started"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> startConsultation(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        try {
            QueueStatusResponse response = queueService.startConsultation(appointmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/appointments/{appointmentId}/complete")
    @Operation(summary = "Complete consultation", description = "Mark consultation as completed and remove from waiting room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consultation successfully completed"),
            @ApiResponse(responseCode = "400", description = "Consultation cannot be completed"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> completeConsultation(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        try {
            QueueStatusResponse response = queueService.completeConsultation(appointmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/appointments/{appointmentId}/no-show")
    @Operation(summary = "Mark patient as no-show", description = "Mark appointment as no-show and remove from queue")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment marked as no-show"),
            @ApiResponse(responseCode = "400", description = "Appointment cannot be marked as no-show"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> markNoShow(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        try {
            QueueStatusResponse response = queueService.markNoShow(appointmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // ========================================
    // PATIENT WAITING ROOM EXPERIENCE
    // ========================================

    @GetMapping("/waiting-room/doctor/{doctorId}")
    @Operation(summary = "Get live queue status for waiting room display", description = "Returns current queue information for waiting room displays")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved waiting room status"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<QueueStatusResponse>> getWaitingRoomStatus(
            @Parameter(description = "Doctor's user ID", required = true)
            @PathVariable String doctorId) {
        
        try {
            List<QueueStatusResponse> waitingRoom = queueService.getCurrentQueueStatus(doctorId);
            return ResponseEntity.ok(waitingRoom);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/waiting-room/patient/{appointmentId}/position")
    @Operation(summary = "Get patient's position in queue", description = "Returns patient's current position, estimated wait time, and queue status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient position"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueueStatusResponse> getPatientQueuePosition(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        try {
            QueueStatusResponse position = queueService.getPatientQueuePosition(appointmentId);
            return ResponseEntity.ok(position);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========================================
    // QUEUE ANALYTICS AND MONITORING
    // ========================================

    @GetMapping("/analytics/waiting-too-long")
    @Operation(summary = "Get patients waiting too long", description = "Returns patients who have been waiting longer than the specified threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patients waiting too long"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<QueueStatusResponse>> getPatientsWaitingTooLong(
            @Parameter(description = "Threshold in minutes (default: 30)", required = false)
            @RequestParam(defaultValue = "30") int thresholdMinutes) {
        
        try {
            List<QueueStatusResponse> longWaitingPatients = queueService.getPatientsWaitingTooLong(thresholdMinutes);
            return ResponseEntity.ok(longWaitingPatients);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================
    // HEALTH CHECK ENDPOINTS
    // ========================================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint for queue service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Queue service is healthy");
    }

    // ========================================
    // ERROR HANDLING
    // ========================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
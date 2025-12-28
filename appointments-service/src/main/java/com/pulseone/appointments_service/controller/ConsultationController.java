package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.ConsultationNotesRequest;
import com.pulseone.appointments_service.dto.response.ConsultationNotesResponse;
import com.pulseone.appointments_service.service.ConsultationService;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for consultation notes and medical records management
 * Handles creation, retrieval, and management of medical consultation data
 */
@RestController
@RequestMapping("/api/consultation-notes")
@Tag(name = "Consultation Notes", description = "APIs for managing medical consultation notes and patient history")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    // ========================================
    // CONSULTATION NOTES CRUD OPERATIONS
    // ========================================

    @PostMapping
    @Operation(summary = "Create consultation notes", description = "Create medical record for a completed appointment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Consultation notes successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or appointment not completed"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "409", description = "Consultation notes already exist for this appointment"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createConsultationNotes(
            @Parameter(description = "Consultation notes details", required = true)
            @RequestBody @Valid ConsultationNotesRequest request) {
        
        try {
            ConsultationNotesResponse response = consultationService.createConsultationNotes(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Consultation notes created successfully");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exist")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().contains("COMPLETED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Appointment must be in COMPLETED status. " + e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage() != null ? e.getMessage() : "Invalid request");
        }
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update consultation notes", description = "Update existing medical record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consultation notes successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Consultation notes not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConsultationNotesResponse> updateConsultationNotes(
            @Parameter(description = "Consultation notes ID", required = true)
            @PathVariable UUID noteId,
            @Parameter(description = "Updated consultation notes details", required = true)
            @RequestBody @Valid ConsultationNotesRequest request) {
        
        try {
            ConsultationNotesResponse response = consultationService.updateConsultationNotes(noteId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "Get consultation notes by ID", description = "Retrieve specific consultation notes by note ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved consultation notes"),
            @ApiResponse(responseCode = "404", description = "Consultation notes not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConsultationNotesResponse> getConsultationNotesById(
            @Parameter(description = "Consultation notes ID", required = true)
            @PathVariable UUID noteId) {
        
        Optional<ConsultationNotesResponse> response = consultationService.getConsultationNotesById(noteId);
        return response.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Get consultation notes by appointment", description = "Retrieve consultation notes for a specific appointment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved consultation notes"),
            @ApiResponse(responseCode = "404", description = "Consultation notes not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConsultationNotesResponse> getConsultationNotesByAppointment(
            @Parameter(description = "Appointment ID", required = true)
            @PathVariable UUID appointmentId) {
        
        Optional<ConsultationNotesResponse> response = consultationService.getConsultationNotesByAppointment(appointmentId);
        return response.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete consultation notes", description = "Delete specific consultation notes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Consultation notes successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Consultation notes not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteConsultationNotes(
            @Parameter(description = "Consultation notes ID", required = true)
            @PathVariable UUID noteId) {
        
        try {
            consultationService.deleteConsultationNotes(noteId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================================
    // PATIENT MEDICAL HISTORY
    // ========================================

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient consultation history", description = "Retrieve complete medical history for a patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient history"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getPatientConsultationHistory(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable String patientId) {
        
        List<ConsultationNotesResponse> history = consultationService.getPatientConsultationHistory(patientId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/patient/{patientId}/doctor/{doctorId}")
    @Operation(summary = "Get patient history with specific doctor", description = "Retrieve patient's consultation history with a specific doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient-doctor history"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getPatientHistoryWithDoctor(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable String patientId,
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId) {
        
        List<ConsultationNotesResponse> history = consultationService.getPatientHistoryWithDoctor(patientId, doctorId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/patient/{patientId}/has-history")
    @Operation(summary = "Check if patient has consultation history", description = "Check if patient has any previous consultation records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked patient history"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Boolean> hasConsultationHistory(
            @Parameter(description = "Patient ID", required = true)
            @PathVariable String patientId) {
        
        boolean hasHistory = consultationService.hasConsultationHistory(patientId);
        return ResponseEntity.ok(hasHistory);
    }

    // ========================================
    // DOCTOR CONSULTATION RECORDS
    // ========================================

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get consultation notes by doctor", description = "Retrieve all consultation notes created by a specific doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor's consultation notes"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getConsultationNotesByDoctor(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId) {
        
        List<ConsultationNotesResponse> consultationNotes = consultationService.getConsultationNotesByDoctor(doctorId);
        return ResponseEntity.ok(consultationNotes);
    }

    @GetMapping("/doctor/{doctorId}/date/{date}")
    @Operation(summary = "Get doctor's consultation notes for specific date", description = "Retrieve consultation notes created by doctor on a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctor's consultation notes for date"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getConsultationNotesByDoctorAndDate(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId,
            @Parameter(description = "Date in YYYY-MM-DD format", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<ConsultationNotesResponse> consultationNotes = consultationService.getConsultationNotesByDoctorAndDate(doctorId, date);
        return ResponseEntity.ok(consultationNotes);
    }

    @GetMapping("/doctor/{doctorId}/statistics")
    @Operation(summary = "Get doctor consultation statistics", description = "Retrieve consultation statistics for a doctor within date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConsultationService.ConsultationStatistics> getConsultationStatistics(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId,
            @Parameter(description = "Start date in YYYY-MM-DD format", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date in YYYY-MM-DD format", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Default to last 30 days if dates not provided
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        
        ConsultationService.ConsultationStatistics statistics = consultationService.getConsultationStatistics(doctorId, start, end);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/doctor/{doctorId}/average-duration")
    @Operation(summary = "Get average consultation duration", description = "Get doctor's average consultation duration for recent consultations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved average duration"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Double> getAverageConsultationDuration(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId,
            @Parameter(description = "Number of days to look back (default: 30)", required = false)
            @RequestParam(defaultValue = "30") int days) {
        
        Double averageDuration = consultationService.getAverageConsultationDuration(doctorId, days);
        return ResponseEntity.ok(averageDuration);
    }

    // ========================================
    // FOLLOW-UP MANAGEMENT
    // ========================================

    @GetMapping("/follow-ups/required")
    @Operation(summary = "Get patients requiring follow-up", description = "Retrieve all patients who require follow-up appointments")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patients requiring follow-up"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getPatientsRequiringFollowUp() {
        
        List<ConsultationNotesResponse> followUps = consultationService.getPatientsRequiringFollowUp();
        return ResponseEntity.ok(followUps);
    }

    @GetMapping("/follow-ups/overdue/doctor/{doctorId}")
    @Operation(summary = "Get overdue follow-ups for doctor", description = "Retrieve overdue follow-up appointments for a specific doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved overdue follow-ups"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getOverdueFollowUps(
            @Parameter(description = "Doctor ID", required = true)
            @PathVariable String doctorId) {
        
        List<ConsultationNotesResponse> overdueFollowUps = consultationService.getOverdueFollowUps(doctorId);
        return ResponseEntity.ok(overdueFollowUps);
    }

    @GetMapping("/follow-ups/urgent")
    @Operation(summary = "Get urgent follow-ups", description = "Retrieve follow-ups that are due within the next 7 days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved urgent follow-ups"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getUrgentFollowUps() {
        
        List<ConsultationNotesResponse> urgentFollowUps = consultationService.getUrgentFollowUps();
        return ResponseEntity.ok(urgentFollowUps);
    }

    // ========================================
    // SEARCH AND ANALYTICS
    // ========================================

    @GetMapping("/search/diagnosis")
    @Operation(summary = "Search by diagnosis", description = "Search consultation notes by diagnosis keywords")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> searchByDiagnosis(
            @Parameter(description = "Diagnosis keyword to search for", required = true)
            @RequestParam String keyword) {
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ConsultationNotesResponse> results = consultationService.searchByDiagnosis(keyword.trim());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/medication")
    @Operation(summary = "Search by medication", description = "Search consultation notes by prescribed medications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> searchByMedication(
            @Parameter(description = "Medication name to search for", required = true)
            @RequestParam String medication) {
        
        if (medication == null || medication.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ConsultationNotesResponse> results = consultationService.searchByMedication(medication.trim());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent consultation notes", description = "Retrieve consultation notes from the last N days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent consultation notes"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsultationNotesResponse>> getRecentConsultations(
            @Parameter(description = "Number of days to look back (default: 7)", required = false)
            @RequestParam(defaultValue = "7") int days) {
        
        List<ConsultationNotesResponse> recentConsultations = consultationService.getRecentConsultations(days);
        return ResponseEntity.ok(recentConsultations);
    }

    // ========================================
    // HEALTH CHECK ENDPOINTS
    // ========================================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint for consultation service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Consultation service is healthy");
    }

    // ========================================
    // ERROR HANDLING
    // ========================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage() != null ? e.getMessage() : "Internal error");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage() != null ? e.getMessage() : "Invalid argument");
    }
}
package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.CreateSessionOverrideRequest;
import com.pulseone.appointments_service.dto.request.CreateSessionRequest;
import com.pulseone.appointments_service.dto.request.UpdateSessionRequest;
import com.pulseone.appointments_service.dto.response.SessionOverrideResponse;
import com.pulseone.appointments_service.dto.response.SessionResponse;
import com.pulseone.appointments_service.service.SessionService;
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

/**
 * REST Controller for managing doctor sessions and session overrides.
 * Provides endpoints for CRUD operations on sessions and session exceptions.
 */
@RestController
@RequestMapping("/sessions")
@Tag(name = "Session Management", description = "APIs for managing doctor sessions and availability")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * Create a new session
     */
    @PostMapping
    @Operation(summary = "Create a new session", description = "Create a new recurring weekly session for a doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Session created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Doctor or clinic not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateSessionRequest request) {
        try {
            SessionResponse response = sessionService.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Get all sessions for a specific doctor
     */
    @GetMapping("/doctor/{doctorUserId}")
    @Operation(summary = "Get sessions by doctor", description = "Retrieve all active sessions for a specific doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getSessionsByDoctor(
            @Parameter(description = "Doctor's user ID from auth service", required = true)
            @PathVariable String doctorUserId) {
        try {
            List<SessionResponse> sessions = sessionService.getSessionsByDoctorUserId(doctorUserId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Get all sessions for a specific clinic
     */
    @GetMapping("/clinic/{clinicId}")
    @Operation(summary = "Get sessions by clinic", description = "Retrieve all active sessions for a specific clinic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Clinic not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getSessionsByClinic(
            @Parameter(description = "Clinic ID", required = true)
            @PathVariable Long clinicId) {
        try {
            List<SessionResponse> sessions = sessionService.getSessionsByClinicId(clinicId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Get a specific session by ID
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session by ID", description = "Retrieve a specific session by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getSessionById(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        try {
            return sessionService.getSessionById(sessionId)
                    .map(session -> ResponseEntity.ok(session))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Update an existing session
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "Update session", description = "Update details of an existing session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionRequest request) {
        try {
            SessionResponse response = sessionService.updateSession(sessionId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Delete (soft delete) a session
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete session", description = "Soft delete a session (sets isActive to false)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Session deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deleteSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Create a session override (holiday/exception)
     */
    @PostMapping("/override")
    @Operation(summary = "Create session override", description = "Create an override for a session on a specific date (holiday, special hours, etc.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Session override created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionOverrideResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Override already exists for this date",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createSessionOverride(@Valid @RequestBody CreateSessionOverrideRequest request) {
        try {
            SessionOverrideResponse response = sessionService.createSessionOverride(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }
}
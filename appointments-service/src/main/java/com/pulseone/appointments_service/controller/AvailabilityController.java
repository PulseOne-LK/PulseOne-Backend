package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.AvailabilitySearchRequest;
import com.pulseone.appointments_service.dto.response.DoctorAvailabilityResponse;
import com.pulseone.appointments_service.dto.response.DoctorCalendarResponse;
import com.pulseone.appointments_service.enums.ServiceType;
import com.pulseone.appointments_service.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing doctor availability searches and calendar views.
 * Provides endpoints for finding available doctors and viewing availability calendars.
 */
@RestController
@RequestMapping("/availability")
@Tag(name = "Availability Management", description = "APIs for searching doctor availability and viewing calendars")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    /**
     * Search for available doctors based on criteria
     */
    @GetMapping("/search")
    @Operation(summary = "Search available doctors", description = "Find available doctors based on date, service type, specialization, and clinic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available doctors retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> searchAvailableDoctors(
            @Parameter(description = "Date to search for availability (YYYY-MM-DD). If not provided, searches next 7 days")
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(description = "Service type filter")
            @RequestParam(value = "serviceType", required = false) ServiceType serviceType,
            
            @Parameter(description = "Medical specialization filter")
            @RequestParam(value = "specialization", required = false) String specialization,
            
            @Parameter(description = "Clinic ID filter")
            @RequestParam(value = "clinicId", required = false) Long clinicId,
            
            @Parameter(description = "Specific doctor user ID")
            @RequestParam(value = "doctorUserId", required = false) String doctorUserId) {
        
        try {
            AvailabilitySearchRequest request = new AvailabilitySearchRequest();
            request.setDate(date);
            request.setServiceType(serviceType);
            request.setSpecialization(specialization);
            request.setClinicId(clinicId);
            request.setDoctorUserId(doctorUserId);

            List<DoctorAvailabilityResponse> availableDoctors = availabilityService.searchAvailableDoctors(request);
            return ResponseEntity.ok(availableDoctors);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "An unexpected error occurred while searching for available doctors"));
        }
    }

    /**
     * Get doctor's availability calendar for the next 30 days
     */
    @GetMapping("/doctor/{doctorUserId}/calendar")
    @Operation(summary = "Get doctor calendar", description = "Retrieve doctor's availability calendar for the next 30 days")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor calendar retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DoctorCalendarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getDoctorCalendar(
            @Parameter(description = "Doctor's user ID from auth service", required = true)
            @PathVariable String doctorUserId) {
        
        try {
            DoctorCalendarResponse calendar = availabilityService.getDoctorCalendar(doctorUserId);
            return ResponseEntity.ok(calendar);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "An unexpected error occurred while retrieving doctor calendar"));
        }
    }

    /**
     * Search available doctors for a specific date (simplified endpoint)
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get availability for specific date", description = "Get all available doctors for a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability for date retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid date format",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAvailabilityForDate(
            @Parameter(description = "Date in YYYY-MM-DD format", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            AvailabilitySearchRequest request = new AvailabilitySearchRequest();
            request.setDate(date);

            List<DoctorAvailabilityResponse> availableDoctors = availabilityService.searchAvailableDoctors(request);
            
            return ResponseEntity.ok(Map.of(
                    "date", date,
                    "availableDoctors", availableDoctors,
                    "totalDoctors", availableDoctors.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "An unexpected error occurred while retrieving availability for date"));
        }
    }

    /**
     * Get available specializations with doctor counts
     */
    @GetMapping("/specializations")
    @Operation(summary = "Get available specializations", description = "Get list of available medical specializations with doctor counts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specializations retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAvailableSpecializations(
            @Parameter(description = "Date to filter by (optional)")
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            AvailabilitySearchRequest request = new AvailabilitySearchRequest();
            request.setDate(date);

            List<DoctorAvailabilityResponse> availableDoctors = availabilityService.searchAvailableDoctors(request);
            
            // Group by specialization and count
            Map<String, Long> specializationCounts = availableDoctors.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            DoctorAvailabilityResponse::getSpecialization,
                            java.util.stream.Collectors.counting()
                    ));

            return ResponseEntity.ok(Map.of(
                    "date", date != null ? date : "next_7_days",
                    "specializations", specializationCounts
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "An unexpected error occurred while retrieving specializations"));
        }
    }
}
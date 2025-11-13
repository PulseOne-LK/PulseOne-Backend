package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.request.CreateDoctorRequest;
import com.pulseone.appointments_service.dto.response.DoctorResponse;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.repository.DoctorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for managing doctors in the appointments system
 * This is mainly for admin operations and manual doctor management
 */
@RestController
@RequestMapping("/doctors")
@Tag(name = "Doctor Management", description = "APIs for managing doctors in appointments system")
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * Create a new doctor record manually
     * Usually doctors are created automatically via user registration events
     */
    @PostMapping
    @Operation(summary = "Create a new doctor record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Doctor created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or doctor already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        try {
            // Check if doctor already exists
            Optional<Doctor> existingDoctor = doctorRepository.findByUserId(request.getUserId());
            if (existingDoctor.isPresent()) {
                return ResponseEntity.badRequest().build();
            }

            // Create new doctor
            Doctor doctor = new Doctor();
            doctor.setUserId(request.getUserId());
            doctor.setName(request.getName());
            doctor.setSpecialization(request.getSpecialization());
            doctor.setIsActive(true);

            Doctor savedDoctor = doctorRepository.save(doctor);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(savedDoctor));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active doctors
     */
    @GetMapping
    @Operation(summary = "Get all active doctors")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        try {
            List<Doctor> doctors = doctorRepository.findByIsActiveTrue();
            List<DoctorResponse> responses = doctors.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get doctor by user ID
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get doctor by user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctor found"),
        @ApiResponse(responseCode = "404", description = "Doctor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DoctorResponse> getDoctorByUserId(@PathVariable String userId) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findByUserId(userId);
            if (doctorOpt.isPresent()) {
                return ResponseEntity.ok(convertToResponse(doctorOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update doctor information
     */
    @PutMapping("/{doctorId}")
    @Operation(summary = "Update doctor information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctor updated successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DoctorResponse> updateDoctor(@PathVariable Long doctorId, 
                                                       @RequestBody CreateDoctorRequest request) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (!doctorOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Doctor doctor = doctorOpt.get();
            if (request.getName() != null) {
                doctor.setName(request.getName());
            }
            if (request.getSpecialization() != null) {
                doctor.setSpecialization(request.getSpecialization());
            }

            Doctor updatedDoctor = doctorRepository.save(doctor);
            return ResponseEntity.ok(convertToResponse(updatedDoctor));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deactivate a doctor
     */
    @DeleteMapping("/{doctorId}")
    @Operation(summary = "Deactivate a doctor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Doctor deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Doctor not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deactivateDoctor(@PathVariable Long doctorId) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (!doctorOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Doctor doctor = doctorOpt.get();
            doctor.setIsActive(false);
            doctorRepository.save(doctor);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert Doctor entity to DoctorResponse DTO
     */
    private DoctorResponse convertToResponse(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUserId(doctor.getUserId());
        response.setName(doctor.getName());
        response.setSpecialization(doctor.getSpecialization());
        response.setIsActive(doctor.getIsActive());
        response.setClinicId(doctor.getClinic() != null ? doctor.getClinic().getId() : null);
        return response;
    }
}
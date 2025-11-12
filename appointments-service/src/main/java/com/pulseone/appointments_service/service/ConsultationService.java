package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.request.ConsultationNotesRequest;
import com.pulseone.appointments_service.dto.response.ConsultationNotesResponse;
import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.entity.ConsultationNotes;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.repository.AppointmentRepository;
import com.pulseone.appointments_service.repository.ConsultationNotesRepository;
import com.pulseone.appointments_service.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for consultation notes and medical records management
 * Handles creation, retrieval, and management of medical consultation data
 */
@Service
@Transactional
public class ConsultationService {

    @Autowired
    private ConsultationNotesRepository consultationNotesRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * Create consultation notes for a completed appointment
     */
    public ConsultationNotesResponse createConsultationNotes(ConsultationNotesRequest request) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + request.getAppointmentId()));

        // Validate appointment status
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Consultation notes can only be created for COMPLETED appointments. Current status: " + appointment.getStatus());
        }

        // Check if consultation notes already exist
        if (consultationNotesRepository.findByAppointment_AppointmentId(request.getAppointmentId()).isPresent()) {
            throw new RuntimeException("Consultation notes already exist for this appointment");
        }

        // Create consultation notes entity
        ConsultationNotes consultationNotes = new ConsultationNotes(appointment, appointment.getDoctorId(), appointment.getPatientId());
        
        // Map request data
        consultationNotes.setChiefComplaint(request.getChiefComplaint());
        consultationNotes.setDiagnosis(request.getDiagnosis());
        consultationNotes.setTreatmentPlan(request.getTreatmentPlan());
        consultationNotes.setVitalSigns(request.getVitalSigns());
        consultationNotes.setMedicationsPrescribed(request.getMedicationsPrescribed());
        consultationNotes.setFollowUpRequired(request.getFollowUpRequired());
        consultationNotes.setFollowUpInDays(request.getFollowUpInDays());
        consultationNotes.setFollowUpInstructions(request.getFollowUpInstructions());
        consultationNotes.setConsultationDurationMinutes(request.getConsultationDurationMinutes());

        // Save consultation notes
        ConsultationNotes savedNotes = consultationNotesRepository.save(consultationNotes);

        return buildConsultationNotesResponse(savedNotes);
    }

    /**
     * Update existing consultation notes
     */
    public ConsultationNotesResponse updateConsultationNotes(UUID noteId, ConsultationNotesRequest request) {
        // Find existing consultation notes
        ConsultationNotes consultationNotes = consultationNotesRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Consultation notes not found: " + noteId));

        // Update fields
        consultationNotes.setChiefComplaint(request.getChiefComplaint());
        consultationNotes.setDiagnosis(request.getDiagnosis());
        consultationNotes.setTreatmentPlan(request.getTreatmentPlan());
        consultationNotes.setVitalSigns(request.getVitalSigns());
        consultationNotes.setMedicationsPrescribed(request.getMedicationsPrescribed());
        consultationNotes.setFollowUpRequired(request.getFollowUpRequired());
        consultationNotes.setFollowUpInDays(request.getFollowUpInDays());
        consultationNotes.setFollowUpInstructions(request.getFollowUpInstructions());
        consultationNotes.setConsultationDurationMinutes(request.getConsultationDurationMinutes());

        // Save updated notes
        ConsultationNotes updatedNotes = consultationNotesRepository.save(consultationNotes);

        return buildConsultationNotesResponse(updatedNotes);
    }

    /**
     * Get consultation notes by appointment ID
     */
    public Optional<ConsultationNotesResponse> getConsultationNotesByAppointment(UUID appointmentId) {
        Optional<ConsultationNotes> consultationNotes = consultationNotesRepository.findByAppointment_AppointmentId(appointmentId);
        return consultationNotes.map(this::buildConsultationNotesResponse);
    }

    /**
     * Get consultation notes by note ID
     */
    public Optional<ConsultationNotesResponse> getConsultationNotesById(UUID noteId) {
        Optional<ConsultationNotes> consultationNotes = consultationNotesRepository.findById(noteId);
        return consultationNotes.map(this::buildConsultationNotesResponse);
    }

    /**
     * Get all consultation notes for a patient (medical history)
     */
    public List<ConsultationNotesResponse> getPatientConsultationHistory(String patientId) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findByPatientId(patientId);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get consultation notes by doctor
     */
    public List<ConsultationNotesResponse> getConsultationNotesByDoctor(String doctorId) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findByDoctorId(doctorId);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get consultation notes for a doctor on a specific date
     */
    public List<ConsultationNotesResponse> getConsultationNotesByDoctorAndDate(String doctorId, LocalDate date) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findByDoctorIdAndDate(doctorId, date);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get patient's consultation history with a specific doctor
     */
    public List<ConsultationNotesResponse> getPatientHistoryWithDoctor(String patientId, String doctorId) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findPatientHistoryWithDoctor(patientId, doctorId);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get patients requiring follow-up
     */
    public List<ConsultationNotesResponse> getPatientsRequiringFollowUp() {
        LocalDate currentDate = LocalDate.now();
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findPatientsRequiringFollowUp(currentDate);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue follow-ups for a doctor
     */
    public List<ConsultationNotesResponse> getOverdueFollowUps(String doctorId) {
        LocalDate currentDate = LocalDate.now();
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findOverdueFollowUps(doctorId, currentDate);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get urgent follow-ups (within next 7 days)
     */
    public List<ConsultationNotesResponse> getUrgentFollowUps() {
        LocalDate currentDate = LocalDate.now();
        LocalDate nextWeek = currentDate.plusDays(7);
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findUrgentFollowUps(currentDate, nextWeek);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search consultation notes by diagnosis
     */
    public List<ConsultationNotesResponse> searchByDiagnosis(String keyword) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findByDiagnosisContaining(keyword);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search consultation notes by medication
     */
    public List<ConsultationNotesResponse> searchByMedication(String medication) {
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findByMedicationPrescribed(medication);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent consultation notes (last N days)
     */
    public List<ConsultationNotesResponse> getRecentConsultations(int days) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        List<ConsultationNotes> consultationNotes = consultationNotesRepository.findRecentConsultations(sinceDate);
        return consultationNotes.stream()
                .map(this::buildConsultationNotesResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get consultation statistics for a doctor
     */
    public ConsultationStatistics getConsultationStatistics(String doctorId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Object[] stats = consultationNotesRepository.getConsultationStatistics(doctorId, startDateTime, endDateTime);
        
        if (stats != null && stats.length >= 4) {
            return new ConsultationStatistics(
                    ((Number) stats[0]).longValue(),  // totalConsultations
                    ((Number) stats[1]).longValue(),  // followUpRequired
                    stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0,  // avgDurationMinutes
                    ((Number) stats[3]).longValue()   // uniquePatients
            );
        }
        
        return new ConsultationStatistics(0L, 0L, 0.0, 0L);
    }

    /**
     * Check if patient has consultation history
     */
    public boolean hasConsultationHistory(String patientId) {
        return consultationNotesRepository.hasConsultationHistory(patientId);
    }

    /**
     * Get average consultation duration for a doctor
     */
    public Double getAverageConsultationDuration(String doctorId, int days) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        Optional<Double> avgDuration = consultationNotesRepository.getAverageConsultationDuration(doctorId, sinceDate);
        return avgDuration.orElse(0.0);
    }

    /**
     * Delete consultation notes
     */
    public void deleteConsultationNotes(UUID noteId) {
        if (!consultationNotesRepository.existsById(noteId)) {
            throw new RuntimeException("Consultation notes not found: " + noteId);
        }
        consultationNotesRepository.deleteById(noteId);
    }

    // Helper Methods

    /**
     * Build ConsultationNotesResponse from entity
     */
    private ConsultationNotesResponse buildConsultationNotesResponse(ConsultationNotes consultationNotes) {
        ConsultationNotesResponse response = new ConsultationNotesResponse();
        
        response.setNoteId(consultationNotes.getNoteId());
        response.setAppointmentId(consultationNotes.getAppointment().getAppointmentId());
        response.setDoctorId(consultationNotes.getDoctorId());
        response.setPatientId(consultationNotes.getPatientId());
        response.setChiefComplaint(consultationNotes.getChiefComplaint());
        response.setDiagnosis(consultationNotes.getDiagnosis());
        response.setTreatmentPlan(consultationNotes.getTreatmentPlan());
        response.setVitalSigns(consultationNotes.getVitalSigns());
        response.setMedicationsPrescribed(consultationNotes.getMedicationsPrescribed());
        response.setFollowUpRequired(consultationNotes.getFollowUpRequired());
        response.setFollowUpInDays(consultationNotes.getFollowUpInDays());
        response.setFollowUpInstructions(consultationNotes.getFollowUpInstructions());
        response.setConsultationDurationMinutes(consultationNotes.getConsultationDurationMinutes());
        response.setCreatedAt(consultationNotes.getCreatedAt());
        response.setUpdatedAt(consultationNotes.getUpdatedAt());

        // Set computed fields
        response.setFollowUpDueDate(consultationNotes.getFollowUpDueDate());
        response.setIsFollowUpOverdue(consultationNotes.isFollowUpOverdue());

        // Get doctor name
        Optional<Doctor> doctor = doctorRepository.findByUserId(consultationNotes.getDoctorId());
        if (doctor.isPresent()) {
            response.setDoctorName(doctor.get().getName());
        }

        return response;
    }

    /**
     * Inner class for consultation statistics
     */
    public static class ConsultationStatistics {
        private final Long totalConsultations;
        private final Long followUpRequired;
        private final Double averageDurationMinutes;
        private final Long uniquePatients;

        public ConsultationStatistics(Long totalConsultations, Long followUpRequired, 
                                    Double averageDurationMinutes, Long uniquePatients) {
            this.totalConsultations = totalConsultations;
            this.followUpRequired = followUpRequired;
            this.averageDurationMinutes = averageDurationMinutes;
            this.uniquePatients = uniquePatients;
        }

        public Long getTotalConsultations() { return totalConsultations; }
        public Long getFollowUpRequired() { return followUpRequired; }
        public Double getAverageDurationMinutes() { return averageDurationMinutes; }
        public Long getUniquePatients() { return uniquePatients; }
        
        public Double getFollowUpRate() {
            return totalConsultations > 0 ? (followUpRequired.doubleValue() / totalConsultations * 100) : 0.0;
        }
    }
}
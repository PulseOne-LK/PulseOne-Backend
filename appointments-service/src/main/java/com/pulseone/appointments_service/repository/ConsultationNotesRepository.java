package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.ConsultationNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ConsultationNotes entity operations
 * Handles queries related to medical records and consultation history
 */
@Repository
public interface ConsultationNotesRepository extends JpaRepository<ConsultationNotes, UUID> {

    /**
     * Find consultation notes by appointment ID
     */
    Optional<ConsultationNotes> findByAppointment_AppointmentId(UUID appointmentId);

    /**
     * Find all consultation notes for a specific patient
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "JOIN cn.appointment a " +
           "WHERE cn.patientId = :patientId " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findByPatientId(@Param("patientId") String patientId);

    /**
     * Find all consultation notes by a specific doctor
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "JOIN cn.appointment a " +
           "WHERE cn.doctorId = :doctorId " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findByDoctorId(@Param("doctorId") String doctorId);

    /**
     * Find consultation notes for a specific doctor on a specific date
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "JOIN cn.appointment a " +
           "WHERE cn.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "ORDER BY cn.createdAt")
    List<ConsultationNotes> findByDoctorIdAndDate(@Param("doctorId") String doctorId, 
                                                   @Param("date") LocalDate date);

    /**
     * Find patients requiring follow-up
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "JOIN cn.appointment a " +
           "WHERE cn.followUpRequired = true " +
           "AND cn.followUpInDays IS NOT NULL " +
           "AND FUNCTION('DATE_ADD', a.appointmentDate, cn.followUpInDays) <= :currentDate " +
           "ORDER BY a.appointmentDate, cn.followUpInDays")
    List<ConsultationNotes> findPatientsRequiringFollowUp(@Param("currentDate") LocalDate currentDate);

    /**
     * Find patients with overdue follow-ups for a specific doctor
     * Calculates follow-up date dynamically: appointmentDate + followUpInDays
     */
    @Query(value = "SELECT cn.* FROM consultation_notes cn " +
           "JOIN appointments a ON cn.appointment_id = a.appointment_id " +
           "WHERE cn.doctor_id = :doctorId " +
           "AND cn.follow_up_required = true " +
           "AND cn.follow_up_in_days IS NOT NULL " +
           "AND (a.appointment_date + (cn.follow_up_in_days || ' days')::interval)::date < :currentDate " +
           "ORDER BY a.appointment_date", 
           nativeQuery = true)
    List<ConsultationNotes> findOverdueFollowUps(@Param("doctorId") String doctorId, 
                                                  @Param("currentDate") LocalDate currentDate);

    /**
     * Find consultation notes by diagnosis keyword
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "WHERE LOWER(cn.diagnosis) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findByDiagnosisContaining(@Param("keyword") String keyword);

    /**
     * Find consultation notes with specific vital signs
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "WHERE cn.vitalSigns IS NOT NULL " +
           "AND JSON_EXTRACT(cn.vitalSigns, :vitalSignPath) IS NOT NULL " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findWithVitalSign(@Param("vitalSignPath") String vitalSignPath);

    /**
     * Get consultation statistics for a doctor
     */
    @Query("SELECT " +
           "COUNT(cn) as totalConsultations, " +
           "COUNT(CASE WHEN cn.followUpRequired = true THEN 1 END) as followUpRequired, " +
           "AVG(cn.consultationDurationMinutes) as avgDurationMinutes, " +
           "COUNT(DISTINCT cn.patientId) as uniquePatients " +
           "FROM ConsultationNotes cn " +
           "WHERE cn.doctorId = :doctorId " +
           "AND cn.createdAt BETWEEN :startDate AND :endDate")
    Object[] getConsultationStatistics(@Param("doctorId") String doctorId, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find consultation notes within a date range
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "JOIN cn.appointment a " +
           "WHERE a.appointmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDate DESC, cn.createdAt DESC")
    List<ConsultationNotes> findByDateRange(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * Find patient's consultation history with a specific doctor
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "WHERE cn.patientId = :patientId " +
           "AND cn.doctorId = :doctorId " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findPatientHistoryWithDoctor(@Param("patientId") String patientId, 
                                                          @Param("doctorId") String doctorId);

    /**
     * Find recent consultation notes (last N days)
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "WHERE cn.createdAt >= :sinceDate " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findRecentConsultations(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Count consultations by doctor for a specific period
     */
    @Query("SELECT COUNT(cn) FROM ConsultationNotes cn " +
           "WHERE cn.doctorId = :doctorId " +
           "AND cn.createdAt BETWEEN :startDate AND :endDate")
    Long countConsultationsByDoctorInPeriod(@Param("doctorId") String doctorId, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find patients with chronic conditions (multiple consultations with similar diagnosis)
     */
    @Query("SELECT cn.patientId, cn.diagnosis, COUNT(cn) as consultationCount " +
           "FROM ConsultationNotes cn " +
           "WHERE cn.diagnosis IS NOT NULL " +
           "GROUP BY cn.patientId, cn.diagnosis " +
           "HAVING COUNT(cn) >= :minConsultations " +
           "ORDER BY consultationCount DESC")
    List<Object[]> findPatientsWithChronicConditions(@Param("minConsultations") Long minConsultations);

    /**
     * Find consultation notes that mention specific medications
     */
    @Query("SELECT cn FROM ConsultationNotes cn " +
           "WHERE LOWER(cn.medicationsPrescribed) LIKE LOWER(CONCAT('%', :medication, '%')) " +
           "ORDER BY cn.createdAt DESC")
    List<ConsultationNotes> findByMedicationPrescribed(@Param("medication") String medication);

    /**
     * Get average consultation duration for a doctor
     */
    @Query("SELECT AVG(cn.consultationDurationMinutes) " +
           "FROM ConsultationNotes cn " +
           "WHERE cn.doctorId = :doctorId " +
           "AND cn.consultationDurationMinutes IS NOT NULL " +
           "AND cn.createdAt >= :sinceDate")
    Optional<Double> getAverageConsultationDuration(@Param("doctorId") String doctorId, 
                                                    @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find consultation notes that require urgent follow-up (within next 7 days)
     * Calculates follow-up date dynamically: appointmentDate + followUpInDays
     */
    @Query(value = "SELECT cn.* FROM consultation_notes cn " +
           "JOIN appointments a ON cn.appointment_id = a.appointment_id " +
           "WHERE cn.follow_up_required = true " +
           "AND cn.follow_up_in_days IS NOT NULL " +
           "AND cn.follow_up_in_days <= 7 " +
           "AND (a.appointment_date + (cn.follow_up_in_days || ' days')::interval)::date BETWEEN :currentDate AND :nextWeek " +
           "ORDER BY (a.appointment_date + (cn.follow_up_in_days || ' days')::interval)::date", 
           nativeQuery = true)
    List<ConsultationNotes> findUrgentFollowUps(@Param("currentDate") LocalDate currentDate, 
                                                 @Param("nextWeek") LocalDate nextWeek);

    /**
     * Check if patient has consultation notes
     */
    @Query("SELECT CASE WHEN COUNT(cn) > 0 THEN true ELSE false END " +
           "FROM ConsultationNotes cn " +
           "WHERE cn.patientId = :patientId")
    boolean hasConsultationHistory(@Param("patientId") String patientId);
}
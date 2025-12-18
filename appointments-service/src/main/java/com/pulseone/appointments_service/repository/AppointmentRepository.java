package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.entity.Session;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Appointment entity operations.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Find all appointments for a specific patient
     */
    List<Appointment> findByPatientIdOrderByAppointmentDateDescCreatedAtDesc(String patientId);

    /**
     * Find all appointments for a specific doctor
     */
    List<Appointment> findByDoctorIdOrderByAppointmentDateDescCreatedAtDesc(String doctorId);

    /**
     * Find appointments for a specific session on a specific date
     */
    List<Appointment> findBySessionAndAppointmentDateOrderByQueueNumberAsc(Session session, LocalDate appointmentDate);

    /**
     * Find appointments for a doctor on a specific date
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date ORDER BY a.queueNumber ASC")
    List<Appointment> findByDoctorIdAndAppointmentDateOrderByQueueNumberAsc(@Param("doctorId") String doctorId, 
                                                                           @Param("date") LocalDate date);

    /**
     * Get the maximum queue number for a session on a specific date
     */
    @Query("SELECT COALESCE(MAX(a.queueNumber), 0) FROM Appointment a WHERE a.session = :session AND a.appointmentDate = :date AND a.status != 'CANCELLED'")
    Optional<Integer> getMaxQueueNumberForSessionAndDate(@Param("session") Session session, @Param("date") LocalDate date);

    /**
     * Count active appointments for a session on a specific date
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.session = :session AND a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    Long countActiveAppointmentsForSessionAndDate(@Param("session") Session session, @Param("date") LocalDate date);

    /**
     * Check if a patient already has an appointment with a doctor on a specific date
     */
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    Optional<Appointment> findActiveAppointmentByPatientDoctorAndDate(@Param("patientId") String patientId,
                                                                     @Param("doctorId") String doctorId,
                                                                     @Param("date") LocalDate date);

    /**
     * Find appointments by status
     */
    List<Appointment> findByStatusOrderByAppointmentDateAscQueueNumberAsc(AppointmentStatus status);

    /**
     * Find appointments for a patient with specific status
     */
    List<Appointment> findByPatientIdAndStatusOrderByAppointmentDateDesc(String patientId, AppointmentStatus status);

    /**
     * Find appointments for a doctor with specific status
     */
    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateAscQueueNumberAsc(String doctorId, AppointmentStatus status);

    /**
     * Find appointments within a date range for a doctor
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate >= :startDate AND a.appointmentDate <= :endDate ORDER BY a.appointmentDate ASC, a.queueNumber ASC")
    List<Appointment> findByDoctorIdAndDateRangeOrderByDateAndQueue(@Param("doctorId") String doctorId,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);

    /**
     * Find upcoming appointments for a patient (not cancelled, not completed)
     */
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.appointmentDate >= :currentDate AND a.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingAppointmentsByPatientId(@Param("patientId") String patientId, @Param("currentDate") LocalDate currentDate);

    /**
     * Find past appointments for a patient
     */
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND (a.appointmentDate < :currentDate OR a.status IN ('COMPLETED', 'NO_SHOW')) ORDER BY a.appointmentDate DESC")
    List<Appointment> findPastAppointmentsByPatientId(@Param("patientId") String patientId, @Param("currentDate") LocalDate currentDate);

    /**
     * Find today's appointments for a specific clinic
     */
    @Query("SELECT a FROM Appointment a WHERE a.clinic.profileClinicId = :clinicId AND a.appointmentDate = :today AND a.status NOT IN ('CANCELLED', 'NO_SHOW') ORDER BY a.queueNumber ASC")
    List<Appointment> findTodayAppointmentsByClinicId(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);
}
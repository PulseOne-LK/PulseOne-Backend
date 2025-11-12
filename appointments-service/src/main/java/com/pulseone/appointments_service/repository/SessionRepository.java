package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.Session;
import com.pulseone.appointments_service.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository interface for Session entity operations.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Find all active sessions for a specific doctor
     */
    List<Session> findByDoctorAndIsActiveTrueOrderByDayOfWeekAscSessionStartTimeAsc(Doctor doctor);

    /**
     * Find all active sessions for a doctor by user ID
     */
    @Query("SELECT s FROM Session s WHERE s.doctor.userId = :userId AND s.isActive = true ORDER BY s.dayOfWeek ASC, s.sessionStartTime ASC")
    List<Session> findByDoctorUserIdAndIsActiveTrue(@Param("userId") String userId);

    /**
     * Find sessions for a specific doctor ID and day of week
     */
    @Query("SELECT s FROM Session s WHERE s.doctor.userId = :doctorId AND s.dayOfWeek = :dayOfWeek AND s.isActive = true ORDER BY s.sessionStartTime ASC")
    List<Session> findByDoctorIdAndDayOfWeek(@Param("doctorId") String doctorId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Find sessions for a specific doctor and day of week
     */
    List<Session> findByDoctorAndDayOfWeekAndIsActiveTrueOrderBySessionStartTimeAsc(Doctor doctor, DayOfWeek dayOfWeek);

    /**
     * Check for overlapping sessions for the same doctor on the same day
     * Used for validation to prevent scheduling conflicts
     */
    @Query("SELECT s FROM Session s WHERE s.doctor = :doctor AND s.dayOfWeek = :dayOfWeek AND s.isActive = true " +
           "AND ((s.sessionStartTime < :endTime AND s.sessionEndTime > :startTime))")
    List<Session> findOverlappingSessions(@Param("doctor") Doctor doctor, 
                                        @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                        @Param("startTime") LocalTime startTime, 
                                        @Param("endTime") LocalTime endTime);

    /**
     * Find sessions that are effective on a specific date
     */
    @Query("SELECT s FROM Session s WHERE s.isActive = true " +
           "AND s.effectiveFrom <= :date " +
           "AND (s.effectiveUntil IS NULL OR s.effectiveUntil >= :date)")
    List<Session> findSessionsEffectiveOnDate(@Param("date") LocalDate date);

    /**
     * Find all active sessions at a specific clinic
     */
    @Query("SELECT s FROM Session s WHERE s.clinic.id = :clinicId AND s.isActive = true ORDER BY s.dayOfWeek ASC, s.sessionStartTime ASC")
    List<Session> findByClinicIdAndIsActiveTrue(@Param("clinicId") Long clinicId);
}
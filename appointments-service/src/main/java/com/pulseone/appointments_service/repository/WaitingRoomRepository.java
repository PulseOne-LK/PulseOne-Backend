package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.WaitingRoom;
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
 * Repository interface for WaitingRoom entity operations
 * Handles queries related to patient check-in and waiting room management
 */
@Repository
public interface WaitingRoomRepository extends JpaRepository<WaitingRoom, UUID> {

    /**
     * Find waiting room entry by appointment ID
     */
    Optional<WaitingRoom> findByAppointment_AppointmentId(UUID appointmentId);

    /**
     * Find all patients currently in waiting room for a doctor today
     * (checked in but not yet started consultation)
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status IN ('CHECKED_IN', 'IN_PROGRESS') " +
           "ORDER BY a.queueNumber")
    List<WaitingRoom> findCurrentWaitingPatients(@Param("doctorId") String doctorId, 
                                                  @Param("date") LocalDate date);

    /**
     * Find all patients who have checked in but not been called yet
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'CHECKED_IN' " +
           "AND wr.calledAt IS NULL " +
           "ORDER BY a.queueNumber")
    List<WaitingRoom> findPatientsWaitingToBeCalled(@Param("doctorId") String doctorId, 
                                                     @Param("date") LocalDate date);

    /**
     * Find patients currently in consultation (called but not completed)
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'IN_PROGRESS' " +
           "AND wr.calledAt IS NOT NULL " +
           "ORDER BY wr.calledAt")
    List<WaitingRoom> findPatientsInConsultation(@Param("doctorId") String doctorId, 
                                                  @Param("date") LocalDate date);

    /**
     * Get the next patient to be called (lowest queue number, checked in, not called)
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'CHECKED_IN' " +
           "AND wr.calledAt IS NULL " +
           "ORDER BY a.queueNumber " +
           "LIMIT 1")
    Optional<WaitingRoom> findNextPatientToCall(@Param("doctorId") String doctorId, 
                                                 @Param("date") LocalDate date);

    /**
     * Count patients ahead of a specific queue number who are still waiting
     */
    @Query("SELECT COUNT(wr) FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.queueNumber < :queueNumber " +
           "AND a.status IN ('CHECKED_IN', 'IN_PROGRESS')")
    Long countPatientsAhead(@Param("doctorId") String doctorId, 
                           @Param("date") LocalDate date, 
                           @Param("queueNumber") Integer queueNumber);

    /**
     * Get current queue number being served (highest queue number in progress)
     */
    @Query("SELECT MAX(a.queueNumber) FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'IN_PROGRESS'")
    Optional<Integer> getCurrentQueueNumberBeingServed(@Param("doctorId") String doctorId, 
                                                       @Param("date") LocalDate date);

    /**
     * Get average wait time for completed appointments today
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (wr.calledAt - wr.checkedInAt))/60) " +
           "FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND wr.calledAt IS NOT NULL " +
           "AND a.status IN ('COMPLETED', 'IN_PROGRESS')")
    Optional<Double> getAverageWaitTimeMinutes(@Param("doctorId") String doctorId, 
                                               @Param("date") LocalDate date);

    /**
     * Find all waiting room entries for a specific date range
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.appointmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDate, a.queueNumber")
    List<WaitingRoom> findByDateRange(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    /**
     * Find patients who checked in but were marked as no-show
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'NO_SHOW' " +
           "AND wr.checkedInAt IS NOT NULL " +
           "ORDER BY a.queueNumber")
    List<WaitingRoom> findNoShowPatientsWhoCheckedIn(@Param("doctorId") String doctorId, 
                                                      @Param("date") LocalDate date);

    /**
     * Get waiting room statistics for a doctor on a specific date
     */
    @Query("SELECT " +
           "COUNT(wr) as totalCheckedIn, " +
           "COUNT(CASE WHEN a.status = 'CHECKED_IN' THEN 1 END) as stillWaiting, " +
           "COUNT(CASE WHEN a.status = 'IN_PROGRESS' THEN 1 END) as inConsultation, " +
           "COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completed, " +
           "AVG(CASE WHEN wr.calledAt IS NOT NULL THEN EXTRACT(EPOCH FROM (wr.calledAt - wr.checkedInAt))/60 END) as avgWaitMinutes " +
           "FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date")
    Object[] getWaitingRoomStatistics(@Param("doctorId") String doctorId, 
                                      @Param("date") LocalDate date);

    /**
     * Find patients who have been waiting longer than expected
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.status = 'CHECKED_IN' " +
           "AND wr.calledAt IS NULL " +
           "AND wr.checkedInAt < :thresholdTime " +
           "ORDER BY wr.checkedInAt")
    List<WaitingRoom> findPatientsWaitingLongerThan(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Delete waiting room entry when appointment is completed or cancelled
     */
    void deleteByAppointment_AppointmentId(UUID appointmentId);

    /**
     * Check if patient is currently in waiting room
     */
    @Query("SELECT CASE WHEN COUNT(wr) > 0 THEN true ELSE false END " +
           "FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.appointmentId = :appointmentId " +
           "AND a.status IN ('CHECKED_IN', 'IN_PROGRESS')")
    boolean isPatientInWaitingRoom(@Param("appointmentId") UUID appointmentId);

    /**
     * Find all patients who completed consultation today
     */
    @Query("SELECT wr FROM WaitingRoom wr " +
           "JOIN wr.appointment a " +
           "WHERE a.doctorId = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status = 'COMPLETED' " +
           "AND wr.consultationStartedAt IS NOT NULL " +
           "ORDER BY wr.consultationStartedAt DESC")
    List<WaitingRoom> findCompletedConsultationsToday(@Param("doctorId") String doctorId, 
                                                       @Param("date") LocalDate date);
}
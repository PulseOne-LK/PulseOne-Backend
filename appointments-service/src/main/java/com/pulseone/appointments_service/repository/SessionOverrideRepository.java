package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.SessionOverride;
import com.pulseone.appointments_service.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SessionOverride entity operations.
 */
@Repository
public interface SessionOverrideRepository extends JpaRepository<SessionOverride, Long> {

    /**
     * Find override for a specific session on a specific date
     */
    Optional<SessionOverride> findBySessionAndOverrideDate(Session session, LocalDate overrideDate);

    /**
     * Find all overrides for a specific session
     */
    List<SessionOverride> findBySessionOrderByOverrideDateAsc(Session session);

    /**
     * Find all overrides for a session within a date range
     */
    @Query("SELECT so FROM SessionOverride so WHERE so.session = :session " +
           "AND so.overrideDate >= :startDate AND so.overrideDate <= :endDate " +
           "ORDER BY so.overrideDate ASC")
    List<SessionOverride> findBySessionAndDateRange(@Param("session") Session session,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Find all overrides for a doctor's sessions on a specific date
     */
    @Query("SELECT so FROM SessionOverride so WHERE so.session.doctor.userId = :doctorUserId " +
           "AND so.overrideDate = :date")
    List<SessionOverride> findByDoctorUserIdAndDate(@Param("doctorUserId") String doctorUserId,
                                                    @Param("date") LocalDate date);

    /**
     * Check if a session has any override on a specific date
     */
    boolean existsBySessionAndOverrideDate(Session session, LocalDate overrideDate);
}
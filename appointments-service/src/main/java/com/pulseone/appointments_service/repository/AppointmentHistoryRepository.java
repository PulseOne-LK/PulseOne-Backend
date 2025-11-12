package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.entity.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AppointmentHistory entity operations.
 */
@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {

    /**
     * Find all history records for a specific appointment
     */
    List<AppointmentHistory> findByAppointmentOrderByChangedAtAsc(Appointment appointment);

    /**
     * Find all history records for a specific appointment by appointment ID
     */
    List<AppointmentHistory> findByAppointment_AppointmentIdOrderByChangedAtAsc(UUID appointmentId);
}
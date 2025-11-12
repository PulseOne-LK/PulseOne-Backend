package com.pulseone.appointments_service.enums;

/**
 * Defines the current status of an appointment throughout its lifecycle.
 */
public enum AppointmentStatus {
    /**
     * Appointment has been booked and is confirmed
     */
    BOOKED,
    
    /**
     * Patient has arrived and checked in at the clinic/online
     */
    CHECKED_IN,
    
    /**
     * Consultation is currently in progress
     */
    IN_PROGRESS,
    
    /**
     * Consultation has been completed successfully
     */
    COMPLETED,
    
    /**
     * Appointment has been cancelled by patient or doctor
     */
    CANCELLED,
    
    /**
     * Patient did not show up for the appointment
     */
    NO_SHOW
}
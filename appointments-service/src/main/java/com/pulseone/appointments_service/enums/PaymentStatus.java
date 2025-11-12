package com.pulseone.appointments_service.enums;

/**
 * Defines the payment status of an appointment.
 */
public enum PaymentStatus {
    /**
     * Payment is pending/not yet made
     */
    PENDING,
    
    /**
     * Payment has been completed successfully
     */
    PAID,
    
    /**
     * Payment failed to process
     */
    FAILED,
    
    /**
     * Payment was refunded
     */
    REFUNDED
}
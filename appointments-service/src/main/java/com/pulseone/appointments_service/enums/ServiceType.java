package com.pulseone.appointments_service.enums;

/**
 * Defines the types of medical services that can be offered during a session.
 * 
 * IMPORTANT: The Dual-Mode Doctor Concept
 * - VIRTUAL: Direct doctor workflow (self-managed, time slots, online pre-payment required)
 * - IN_PERSON: Clinic workflow (admin-managed, token/queue system, cash or app payment)
 * 
 * A session is NEVER both - it's strictly one or the other.
 */
public enum ServiceType {
    /**
     * Virtual/online consultation via telemedicine platform.
     * - Managed by: Doctor (self-managed)
     * - Booking: Time-slot based (not token-based)
     * - Payment: Online pre-payment REQUIRED before consultation
     * - Video: AWS Chime meeting link generated upon booking
     */
    VIRTUAL,
    
    /**
     * In-person consultation at a physical clinic location.
     * - Managed by: Clinic Admin
     * - Booking: Token/queue system
     * - Payment: Cash at desk or via app (not mandatory upfront)
     * - Video: No video link (physical only)
     */
    IN_PERSON
}
package com.pulseone.appointments_service.enums;

/**
 * Defines the types of medical services that can be offered during a session.
 */
public enum ServiceType {
    /**
     * Virtual/online consultation via telemedicine platform
     */
    VIRTUAL,
    
    /**
     * In-person consultation at a physical clinic location
     */
    IN_PERSON,
    
    /**
     * Both virtual and in-person options available for the session
     */
    BOTH
}
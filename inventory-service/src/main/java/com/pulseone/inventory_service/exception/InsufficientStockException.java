package com.pulseone.inventory_service.exception;

/**
 * InsufficientStockException - Exception thrown when stock quantity is
 * insufficient
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

package com.pulseone.inventory_service.exception;

/**
 * CatalogItemNotFoundException - Exception thrown when a catalog item is not
 * found
 */
public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(String message) {
        super(message);
    }
}

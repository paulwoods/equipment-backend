package com.mrpaulwoods.equipment.backend.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entityType, String id) {
        super(entityType + " not found: " + id);
    }
}

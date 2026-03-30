package com.mrpaulwoods.equipment.backend.exception;

public class ProcedureNotFoundException extends RuntimeException {
    public ProcedureNotFoundException(String id) {
        super("Procedure not found: " + id);
    }
}

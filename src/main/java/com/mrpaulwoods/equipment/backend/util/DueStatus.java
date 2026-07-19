package com.mrpaulwoods.equipment.backend.util;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The maintenance due-status of a procedure, as computed by {@link DueDetails}.
 * The wire value (used for both JSON serialization and email rendering) is
 * preserved via {@link #toString()} / {@link #wireValue()} so existing
 * consumers see the same literal strings as before this type existed.
 */
public enum DueStatus {
    OVERDUE("OVERDUE"),
    UPCOMING("Upcoming"),
    NO_HISTORY("No history");

    private final String wireValue;

    DueStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @Override
    public String toString() {
        return wireValue;
    }
}

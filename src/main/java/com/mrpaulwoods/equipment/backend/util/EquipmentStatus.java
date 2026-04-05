package com.mrpaulwoods.equipment.backend.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EquipmentStatus {
    ACTIVE("Active"),
    IN_USE("In Use"),
    UNDER_REPAIR("Under Repair"),
    DECOMMISSIONED("Decommissioned"),
    IN_STORAGE("In Storage");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static EquipmentStatus fromDisplayName(String value) {
        for (EquipmentStatus status : values()) {
            if (status.displayName.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown equipment status: " + value);
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}

package com.mrpaulwoods.equipment.backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentStatusTest {

    @ParameterizedTest
    @CsvSource({
            "Active,          ACTIVE",
            "In Use,          IN_USE",
            "Under Repair,    UNDER_REPAIR",
            "Decommissioned,  DECOMMISSIONED",
            "In Storage,      IN_STORAGE"
    })
    void fromDisplayName_knownValues_returnsCorrectEnum(String displayName, String expectedName) {
        EquipmentStatus status = EquipmentStatus.fromDisplayName(displayName.trim());
        assertThat(status.name()).isEqualTo(expectedName.trim());
    }

    @Test
    void fromDisplayName_unknownValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> EquipmentStatus.fromDisplayName("Broken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown equipment status: Broken");
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE,          Active",
            "IN_USE,          In Use",
            "UNDER_REPAIR,    Under Repair",
            "DECOMMISSIONED,  Decommissioned",
            "IN_STORAGE,      In Storage"
    })
    void getDisplayName_returnsExpectedString(String enumName, String expectedDisplay) {
        EquipmentStatus status = EquipmentStatus.valueOf(enumName.trim());
        assertThat(status.getDisplayName()).isEqualTo(expectedDisplay.trim());
    }

    @Test
    void fromDisplayName_roundTrips() {
        for (EquipmentStatus status : EquipmentStatus.values()) {
            assertThat(EquipmentStatus.fromDisplayName(status.getDisplayName())).isEqualTo(status);
        }
    }
}

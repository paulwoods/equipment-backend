package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @TempDir
    Path tempDir;

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        equipmentService = new EquipmentService(props, new ObjectMapper());
    }

    private Equipment sampleEquipment() {
        Equipment e = new Equipment();
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 1));
        return e;
    }

    @Test
    void getAll_whenNoFile_returnsEmptyList() {
        assertThat(equipmentService.getAll()).isEmpty();
    }

    @Test
    void create_assignsIdAndPersists() {
        Equipment created = equipmentService.create(sampleEquipment());

        assertThat(created.getId()).isNotNull().hasSize(7);
        assertThat(equipmentService.getAll()).hasSize(1);
    }

    @Test
    void getById_whenExists_returnsEquipment() {
        Equipment created = equipmentService.create(sampleEquipment());

        Equipment found = equipmentService.getById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getManufacturer()).isEqualTo("Acme");
    }

    @Test
    void getById_whenNotFound_throwsException() {
        assertThatThrownBy(() -> equipmentService.getById("no-such-id"))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("no-such-id");
    }

    @Test
    void update_replacesEquipment() {
        Equipment created = equipmentService.create(sampleEquipment());

        Equipment updated = sampleEquipment();
        updated.setManufacturer("NewCorp");
        equipmentService.update(created.getId(), updated);

        assertThat(equipmentService.getById(created.getId()).getManufacturer()).isEqualTo("NewCorp");
    }

    @Test
    void update_preservesId() {
        Equipment created = equipmentService.create(sampleEquipment());

        Equipment updated = sampleEquipment();
        Equipment result = equipmentService.update(created.getId(), updated);

        assertThat(result.getId()).isEqualTo(created.getId());
    }

    @Test
    void update_whenNotFound_throwsException() {
        assertThatThrownBy(() -> equipmentService.update("no-such-id", sampleEquipment()))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void delete_removesEquipment() {
        Equipment created = equipmentService.create(sampleEquipment());

        equipmentService.delete(created.getId());

        assertThat(equipmentService.getAll()).isEmpty();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        assertThatThrownBy(() -> equipmentService.delete("no-such-id"))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void save_persistsMultipleEquipment() {
        equipmentService.create(sampleEquipment());
        equipmentService.create(sampleEquipment());

        assertThat(equipmentService.getAll()).hasSize(2);
    }

    @Test
    void getAll_afterSave_returnsPersistedData() {
        Equipment created = equipmentService.create(sampleEquipment());

        // Re-read from disk by creating a new service instance over same tempDir
        EquipmentService fresh = new EquipmentService(
                new AppProperties() {{
                    setDataDir(tempDir.toString());
                }},
                new ObjectMapper()
        );
        List<Equipment> all = fresh.getAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getId()).isEqualTo(created.getId());
    }
}

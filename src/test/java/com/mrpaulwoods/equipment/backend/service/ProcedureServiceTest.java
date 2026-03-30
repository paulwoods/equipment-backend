package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.exception.ProcedureNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.EquipmentStatus;
import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProcedureServiceTest {

    @TempDir
    Path tempDir;

    private EquipmentService equipmentService;
    private ProcedureService procedureService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        equipmentService = new EquipmentService(props, new ObjectMapper());
        procedureService = new ProcedureService(equipmentService);
    }

    private Equipment createEquipment() {
        Equipment e = new Equipment();
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 1));
        return equipmentService.create(e);
    }

    private Procedure sampleProcedure() {
        Procedure p = new Procedure();
        p.setName("Oil Change");
        p.setSteps("Drain and refill");
        p.setIntervalDays(90);
        return p;
    }

    @Test
    void getAllForEquipment_whenNoProcedures_returnsEmptyList() {
        Equipment eq = createEquipment();
        assertThat(procedureService.getAllForEquipment(eq.getId())).isEmpty();
    }

    @Test
    void create_assignsIdAndPersists() {
        Equipment eq = createEquipment();

        Procedure created = procedureService.create(eq.getId(), sampleProcedure());

        assertThat(created.getId()).isNotNull().hasSize(7);
        assertThat(procedureService.getAllForEquipment(eq.getId())).hasSize(1);
    }

    @Test
    void getById_whenExists_returnsProcedure() {
        Equipment eq = createEquipment();
        Procedure created = procedureService.create(eq.getId(), sampleProcedure());

        Procedure found = procedureService.getById(eq.getId(), created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Oil Change");
    }

    @Test
    void getById_whenNotFound_throwsException() {
        Equipment eq = createEquipment();

        assertThatThrownBy(() -> procedureService.getById(eq.getId(), "no-such-id"))
                .isInstanceOf(ProcedureNotFoundException.class)
                .hasMessageContaining("no-such-id");
    }

    @Test
    void update_replacesProcedure() {
        Equipment eq = createEquipment();
        Procedure created = procedureService.create(eq.getId(), sampleProcedure());

        Procedure updated = sampleProcedure();
        updated.setName("Full Service");
        procedureService.update(eq.getId(), created.getId(), updated);

        assertThat(procedureService.getById(eq.getId(), created.getId()).getName())
                .isEqualTo("Full Service");
    }

    @Test
    void update_preservesHistory() {
        Equipment eq = createEquipment();
        Procedure created = procedureService.create(eq.getId(), sampleProcedure());

        // Add history directly via equipmentService
        List<Equipment> all = equipmentService.getAll();
        Perform perform = new Perform("perf-1", LocalDate.now(), "Done");
        all.get(0).getProcedures().get(0).setHistory(new ArrayList<>(List.of(perform)));
        equipmentService.save(all);

        Procedure updated = sampleProcedure();
        updated.setName("Updated");
        Procedure result = procedureService.update(eq.getId(), created.getId(), updated);

        assertThat(result.getHistory()).hasSize(1);
        assertThat(result.getHistory().get(0).getId()).isEqualTo("perf-1");
    }

    @Test
    void update_whenNotFound_throwsException() {
        Equipment eq = createEquipment();

        assertThatThrownBy(() -> procedureService.update(eq.getId(), "no-such-id", sampleProcedure()))
                .isInstanceOf(ProcedureNotFoundException.class);
    }

    @Test
    void delete_removesProcedure() {
        Equipment eq = createEquipment();
        Procedure created = procedureService.create(eq.getId(), sampleProcedure());

        procedureService.delete(eq.getId(), created.getId());

        assertThat(procedureService.getAllForEquipment(eq.getId())).isEmpty();
    }

    @Test
    void delete_whenNotFound_throwsException() {
        Equipment eq = createEquipment();

        assertThatThrownBy(() -> procedureService.delete(eq.getId(), "no-such-id"))
                .isInstanceOf(ProcedureNotFoundException.class);
    }
}

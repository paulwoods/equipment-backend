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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PerformHistoryServiceTest {

    @TempDir
    Path tempDir;

    private EquipmentService equipmentService;
    private ProcedureService procedureService;
    private PerformHistoryService performHistoryService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setDataDir(tempDir.toString());
        equipmentService = new EquipmentService(props, new ObjectMapper());
        procedureService = new ProcedureService(equipmentService);
        performHistoryService = new PerformHistoryService(equipmentService);
    }

    private String createEquipmentWithProcedure() {
        Equipment e = new Equipment();
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 1));
        Equipment eq = equipmentService.create(e);

        Procedure p = new Procedure();
        p.setName("Oil Change");
        p.setSteps("Drain and refill");
        p.setIntervalDays(90);
        Procedure proc = procedureService.create(eq.getId(), p);

        return eq.getId() + ":" + proc.getId();
    }

    @Test
    void getHistory_whenNoHistory_returnsEmptyList() {
        String[] ids = createEquipmentWithProcedure().split(":");
        assertThat(performHistoryService.getHistory(ids[0], ids[1])).isEmpty();
    }

    @Test
    void record_assignsIdAndPersists() {
        String[] ids = createEquipmentWithProcedure().split(":");

        Perform perform = new Perform();
        perform.setDate(LocalDate.of(2024, 6, 1));
        perform.setNotes("Completed");

        Perform recorded = performHistoryService.record(ids[0], ids[1], perform);

        assertThat(recorded.getId()).isNotNull().hasSize(7);
        assertThat(performHistoryService.getHistory(ids[0], ids[1])).hasSize(1);
    }

    @Test
    void record_multipleEntries_allPersisted() {
        String[] ids = createEquipmentWithProcedure().split(":");

        Perform p1 = new Perform();
        p1.setDate(LocalDate.of(2024, 3, 1));
        Perform p2 = new Perform();
        p2.setDate(LocalDate.of(2024, 6, 1));

        performHistoryService.record(ids[0], ids[1], p1);
        performHistoryService.record(ids[0], ids[1], p2);

        List<Perform> history = performHistoryService.getHistory(ids[0], ids[1]);
        assertThat(history).hasSize(2);
    }

    @Test
    void getHistory_whenProcedureNotFound_throwsException() {
        Equipment e = new Equipment();
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 1));
        Equipment eq = equipmentService.create(e);

        assertThatThrownBy(() -> performHistoryService.getHistory(eq.getId(), "no-such-proc"))
                .isInstanceOf(ProcedureNotFoundException.class);
    }
}

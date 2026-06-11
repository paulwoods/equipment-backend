package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ExportResponse;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @InjectMocks
    private ExportService exportService;

    @Test
    void exportAll_mapsEquipmentWithProceduresAndHistoryToDtos() {
        Equipment equipment = new Equipment();
        equipment.setId(UUID.randomUUID());
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setSerialNumber("SN-1");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 15));

        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setEquipment(equipment);
        procedure.setName("Oil change");
        procedure.setSteps("Drain and refill");
        procedure.setIntervalDays(90);
        equipment.getProcedures().add(procedure);

        Perform perform = new Perform();
        perform.setId(UUID.randomUUID());
        perform.setProcedure(procedure);
        perform.setDate(LocalDate.of(2024, 6, 1));
        perform.setNotes("Done");
        procedure.getHistory().add(perform);

        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<ExportResponse.EquipmentExport> result = exportService.exportAll();

        assertThat(result).hasSize(1);
        ExportResponse.EquipmentExport exported = result.getFirst();
        assertThat(exported.id()).isEqualTo(equipment.getId().toString());
        assertThat(exported.manufacturer()).isEqualTo("Acme");
        assertThat(exported.modelNumber()).isEqualTo("X100");
        assertThat(exported.serialNumber()).isEqualTo("SN-1");
        assertThat(exported.status()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(exported.purchaseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(exported.procedures()).hasSize(1);

        ExportResponse.ProcedureExport exportedProcedure = exported.procedures().getFirst();
        assertThat(exportedProcedure.id()).isEqualTo(procedure.getId().toString());
        assertThat(exportedProcedure.name()).isEqualTo("Oil change");
        assertThat(exportedProcedure.steps()).isEqualTo("Drain and refill");
        assertThat(exportedProcedure.intervalDays()).isEqualTo(90);
        assertThat(exportedProcedure.history()).hasSize(1);

        ExportResponse.PerformExport exportedPerform = exportedProcedure.history().getFirst();
        assertThat(exportedPerform.id()).isEqualTo(perform.getId().toString());
        assertThat(exportedPerform.date()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(exportedPerform.notes()).isEqualTo("Done");

        verify(equipmentRepository).findAllWithProcedures();
        verify(procedureRepository).findAllWithHistory();
    }

    @Test
    void exportAll_whenEmpty_returnsEmptyList() {
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of());

        List<ExportResponse.EquipmentExport> result = exportService.exportAll();

        assertThat(result).isEmpty();
        verify(equipmentRepository).findAllWithProcedures();
    }
}

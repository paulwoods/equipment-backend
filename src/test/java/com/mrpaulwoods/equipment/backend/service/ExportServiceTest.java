package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
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

    @InjectMocks
    private ExportService exportService;

    @Test
    void exportAll_returnsAllEquipmentWithProceduresAndHistory() {
        Equipment equipment = new Equipment();
        equipment.setId(UUID.randomUUID());
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 15));

        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<Equipment> result = exportService.exportAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getManufacturer()).isEqualTo("Acme");
        verify(equipmentRepository).findAllWithProcedures();
    }

    @Test
    void exportAll_whenEmpty_returnsEmptyList() {
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of());

        List<Equipment> result = exportService.exportAll();

        assertThat(result).isEmpty();
        verify(equipmentRepository).findAllWithProcedures();
    }
}

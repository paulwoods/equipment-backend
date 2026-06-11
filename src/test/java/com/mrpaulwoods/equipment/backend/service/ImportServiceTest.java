package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ImportRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        importService = new ImportService(
                equipmentRepository,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    private ImportRequest.EquipmentImport validEquipment() {
        return new ImportRequest.EquipmentImport(
                "old-id",
                "Dell",
                "G15",
                null, null, null,
                EquipmentStatus.ACTIVE,
                "Gaming Laptop",
                LocalDate.of(2020, 12, 28),
                List.of()
        );
    }

    private ImportRequest.ProcedureImport validProcedure() {
        return new ImportRequest.ProcedureImport(
                "old-proc-id",
                "Clean Fans",
                null,
                "1. Blow it out",
                null,
                90,
                List.of()
        );
    }

    private ImportRequest.PerformImport validPerform() {
        return new ImportRequest.PerformImport(
                "old-perf-id",
                LocalDate.of(2025, 1, 1),
                "Done"
        );
    }

    @Test
    void importEquipment_withValidData_returnsCorrectCounts() {
        ImportRequest.PerformImport perf = validPerform();
        ImportRequest.ProcedureImport proc = new ImportRequest.ProcedureImport(
                "p1", "Clean Fans", null, "1. Blow", null, 90, List.of(perf));
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(List.of(item));

        assertThat(result.equipmentImported()).isEqualTo(1);
        assertThat(result.proceduresImported()).isEqualTo(1);
        assertThat(result.historyImported()).isEqualTo(1);
    }

    @Test
    void importEquipment_setsNullStatus_toActive() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                null, null, LocalDate.of(2020, 1, 1), List.of());

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        importService.importEquipment(List.of(item));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Equipment>> captor = ArgumentCaptor.forClass(List.class);
        verify(equipmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
    }

    @Test
    void importEquipment_doesNotSetId_soPrePersistGeneratesIt() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        importService.importEquipment(List.of(item));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Equipment>> captor = ArgumentCaptor.forClass(List.class);
        verify(equipmentRepository).saveAll(captor.capture());
        // id should be null at this point — @PrePersist fires on actual persist, not in-memory
        assertThat(captor.getValue().get(0).getId()).isNull();
    }

    @Test
    void importEquipment_withNullProcedures_importsEquipmentOnly() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), null);

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(List.of(item));

        assertThat(result.equipmentImported()).isEqualTo(1);
        assertThat(result.proceduresImported()).isEqualTo(0);
        assertThat(result.historyImported()).isEqualTo(0);
    }

    @Test
    void importEquipment_withMultipleItems_returnsCorrectCounts() {
        ImportRequest.EquipmentImport item1 = validEquipment();
        ImportRequest.EquipmentImport item2 = validEquipment();

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(List.of(item1, item2));

        assertThat(result.equipmentImported()).isEqualTo(2);
    }

    @Test
    void validate_missingManufacturer_throwsException() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank");
    }

    @Test
    void validate_blankManufacturer_throwsException() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "  ", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank");
    }

    @Test
    void validate_missingModelNumber_throwsException() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", null, null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].modelNumber: must not be blank");
    }

    @Test
    void validate_missingPurchaseDate_throwsException() {
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, null, List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].purchaseDate: must not be null");
    }

    @Test
    void validate_missingProcedureName_throwsException() {
        ImportRequest.ProcedureImport proc = new ImportRequest.ProcedureImport(
                "p1", null, null, "steps", null, 90, List.of());
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].name: must not be blank");
    }

    @Test
    void validate_missingProcedureSteps_throwsException() {
        ImportRequest.ProcedureImport proc = new ImportRequest.ProcedureImport(
                "p1", "Clean", null, null, null, 90, List.of());
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].steps: must not be blank");
    }

    @Test
    void validate_nullIntervalDays_throwsException() {
        ImportRequest.ProcedureImport proc = new ImportRequest.ProcedureImport(
                "p1", "Clean", null, "steps", null, null, List.of());
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].intervalDays: must not be null");
    }

    @Test
    void validate_zeroIntervalDays_throwsException() {
        ImportRequest.ProcedureImport proc = new ImportRequest.ProcedureImport(
                "p1", "Clean", null, "steps", null, 0, List.of());
        ImportRequest.EquipmentImport item = new ImportRequest.EquipmentImport(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(List.of(item)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].intervalDays: must be greater than or equal to 1");
    }

    @Test
    void validate_errorMessageIncludesCorrectIndex() {
        ImportRequest.EquipmentImport valid = validEquipment();
        ImportRequest.EquipmentImport invalid = new ImportRequest.EquipmentImport(
                "e2", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(valid, invalid)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[1].manufacturer: must not be blank");
    }

    @Test
    void validate_collectsAllErrorsAcrossItemsInOneException() {
        ImportRequest.EquipmentImport first = new ImportRequest.EquipmentImport(
                "e1", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());
        ImportRequest.EquipmentImport second = new ImportRequest.EquipmentImport(
                "e2", "Dell", null, null, null, null,
                EquipmentStatus.ACTIVE, null, null, List.of());

        assertThatThrownBy(() -> importService.importEquipment(List.of(first, second)))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank")
                .hasMessageContaining("Equipment[1].modelNumber: must not be blank")
                .hasMessageContaining("Equipment[1].purchaseDate: must not be null");
    }
}

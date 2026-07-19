package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.dto.PerformTransfer;
import com.mrpaulwoods.equipment.backend.dto.ProcedureTransfer;
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
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        importService = new ImportService(
                equipmentRepository,
                Validation.buildDefaultValidatorFactory().getValidator(),
                objectMapper
        );
    }

    private InputStream toStream(List<EquipmentTransfer> items) {
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(items));
    }

    private EquipmentTransfer validEquipment() {
        return new EquipmentTransfer(
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

    @Test
    void importEquipment_withValidData_returnsCorrectCounts() throws Exception {
        PerformTransfer perf = new PerformTransfer("old-perf-id", LocalDate.of(2025, 1, 1), "Done");
        ProcedureTransfer proc = new ProcedureTransfer(
                "p1", "Clean Fans", null, "1. Blow", null, 90, List.of(perf));
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(toStream(List.of(item)));

        assertThat(result.equipmentImported()).isEqualTo(1);
        assertThat(result.proceduresImported()).isEqualTo(1);
        assertThat(result.historyImported()).isEqualTo(1);
    }

    @Test
    void importEquipment_setsNullStatus_toActive() throws Exception {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                null, null, LocalDate.of(2020, 1, 1), List.of());

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        importService.importEquipment(toStream(List.of(item)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Equipment>> captor = ArgumentCaptor.forClass(List.class);
        verify(equipmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
    }

    @Test
    void importEquipment_doesNotSetId_soPrePersistGeneratesIt() throws Exception {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        importService.importEquipment(toStream(List.of(item)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Equipment>> captor = ArgumentCaptor.forClass(List.class);
        verify(equipmentRepository).saveAll(captor.capture());
        // id should be null at this point — @PrePersist fires on actual persist, not in-memory
        assertThat(captor.getValue().get(0).getId()).isNull();
    }

    @Test
    void importEquipment_withNullProcedures_importsEquipmentOnly() throws Exception {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), null);

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(toStream(List.of(item)));

        assertThat(result.equipmentImported()).isEqualTo(1);
        assertThat(result.proceduresImported()).isEqualTo(0);
        assertThat(result.historyImported()).isEqualTo(0);
    }

    @Test
    void importEquipment_withMultipleItems_returnsCorrectCounts() throws Exception {
        EquipmentTransfer item1 = validEquipment();
        EquipmentTransfer item2 = validEquipment();

        when(equipmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = importService.importEquipment(toStream(List.of(item1, item2)));

        assertThat(result.equipmentImported()).isEqualTo(2);
    }

    @Test
    void importEquipment_withEmptyStream_throwsException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);

        assertThatThrownBy(() -> importService.importEquipment(empty))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessage("Import file is empty");
    }

    @Test
    void importEquipment_withInvalidJson_throwsException() {
        InputStream invalid = new ByteArrayInputStream("not json".getBytes());

        assertThatThrownBy(() -> importService.importEquipment(invalid))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageStartingWith("Invalid JSON in import file: ");
    }

    @Test
    void validate_missingManufacturer_throwsException() {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank");
    }

    @Test
    void validate_blankManufacturer_throwsException() {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "  ", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank");
    }

    @Test
    void validate_missingModelNumber_throwsException() {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", null, null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].modelNumber: must not be blank");
    }

    @Test
    void validate_missingPurchaseDate_throwsException() {
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, null, List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].purchaseDate: must not be null");
    }

    @Test
    void validate_missingProcedureName_throwsException() {
        ProcedureTransfer proc = new ProcedureTransfer(
                "p1", null, null, "steps", null, 90, List.of());
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].name: must not be blank");
    }

    @Test
    void validate_missingProcedureSteps_throwsException() {
        ProcedureTransfer proc = new ProcedureTransfer(
                "p1", "Clean", null, null, null, 90, List.of());
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].steps: must not be blank");
    }

    @Test
    void validate_nullIntervalDays_throwsException() {
        ProcedureTransfer proc = new ProcedureTransfer(
                "p1", "Clean", null, "steps", null, null, List.of());
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].intervalDays: must not be null");
    }

    @Test
    void validate_zeroIntervalDays_throwsException() {
        ProcedureTransfer proc = new ProcedureTransfer(
                "p1", "Clean", null, "steps", null, 0, List.of());
        EquipmentTransfer item = new EquipmentTransfer(
                "e1", "Dell", "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of(proc));

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(item))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].procedures[0].intervalDays: must be greater than or equal to 1");
    }

    @Test
    void validate_errorMessageIncludesCorrectIndex() {
        EquipmentTransfer valid = validEquipment();
        EquipmentTransfer invalid = new EquipmentTransfer(
                "e2", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(valid, invalid))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[1].manufacturer: must not be blank");
    }

    @Test
    void validate_collectsAllErrorsAcrossItemsInOneException() {
        EquipmentTransfer first = new EquipmentTransfer(
                "e1", null, "G15", null, null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2020, 1, 1), List.of());
        EquipmentTransfer second = new EquipmentTransfer(
                "e2", "Dell", null, null, null, null,
                EquipmentStatus.ACTIVE, null, null, List.of());

        assertThatThrownBy(() -> importService.importEquipment(toStream(List.of(first, second))))
                .isInstanceOf(ImportEquipmentException.class)
                .hasMessageContaining("Equipment[0].manufacturer: must not be blank")
                .hasMessageContaining("Equipment[1].modelNumber: must not be blank")
                .hasMessageContaining("Equipment[1].purchaseDate: must not be null");
    }
}

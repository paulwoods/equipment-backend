package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.PerformRequest;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
import com.mrpaulwoods.equipment.backend.repository.PerformRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PerformHistoryServiceTest {

    private static final UUID EQ_ID = UUID.randomUUID();
    private static final UUID PROC_ID = UUID.randomUUID();
    @Mock
    private ProcedureService procedureService;
    @Mock
    private PerformRepository performRepository;
    @InjectMocks
    private PerformHistoryService performHistoryService;
    private Procedure procedure;

    @BeforeEach
    void setUp() {
        var equipment = new Equipment();
        equipment.setId(EQ_ID);
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 1));

        procedure = new Procedure();
        procedure.setId(PROC_ID);
        procedure.setName("Oil Change");
        procedure.setIntervalDays(30);
        procedure.setEquipment(equipment);
        procedure.setHistory(new ArrayList<>());
    }

    private Perform samplePerform(UUID id, LocalDate date, String notes) {
        var p = new Perform();
        p.setId(id);
        p.setDate(date);
        p.setNotes(notes);
        p.setProcedure(procedure);
        return p;
    }

    // ─── getHistory ───────────────────────────────────────────────────────

    @Test
    void getHistory_withRecords_returnsListResponse() {
        var h1 = samplePerform(UUID.randomUUID(), LocalDate.of(2025, 1, 10), "Looked good");
        var h2 = samplePerform(UUID.randomUUID(), LocalDate.of(2025, 4, 10), null);
        procedure.setHistory(new ArrayList<>(List.of(h1, h2)));
        given(procedureService.getEntityById(EQ_ID, PROC_ID)).willReturn(procedure);

        var result = performHistoryService.getHistory(EQ_ID, PROC_ID);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().date()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(result.getFirst().notes()).isEqualTo("Looked good");
    }

    @Test
    void getHistory_emptyHistory_returnsEmptyList() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID)).willReturn(procedure);

        var result = performHistoryService.getHistory(EQ_ID, PROC_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_unknownProcedure_throwsNotFoundException() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID))
                .willThrow(new NotFoundException("Procedure", PROC_ID.toString()));

        assertThatThrownBy(() -> performHistoryService.getHistory(EQ_ID, PROC_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── record ───────────────────────────────────────────────────────────

    @Test
    void record_validRequest_savesAndReturnsCreateResponse() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID)).willReturn(procedure);
        var performId = UUID.randomUUID();
        given(performRepository.save(any())).willAnswer(inv -> {
            Perform p = inv.getArgument(0);
            p.setId(performId);
            return p;
        });

        var request = new PerformRequest(LocalDate.of(2025, 6, 1), "All good");
        var result = performHistoryService.record(EQ_ID, PROC_ID, request);

        assertThat(result.id()).isEqualTo(performId);
        assertThat(result.date()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(result.notes()).isEqualTo("All good");
    }

    @Test
    void record_setsProcedureOnPerform() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID)).willReturn(procedure);
        given(performRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var captor = ArgumentCaptor.forClass(Perform.class);
        performHistoryService.record(EQ_ID, PROC_ID, new PerformRequest(LocalDate.now(), null));

        then(performRepository).should().save(captor.capture());
        assertThat(captor.getValue().getProcedure()).isEqualTo(procedure);
    }

    @Test
    void record_withNullNotes_savesWithNullNotes() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID)).willReturn(procedure);
        var captor = ArgumentCaptor.forClass(Perform.class);
        given(performRepository.save(any())).willAnswer(inv -> {
            Perform p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        performHistoryService.record(EQ_ID, PROC_ID, new PerformRequest(LocalDate.now(), null));

        then(performRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNotes()).isNull();
    }

    @Test
    void record_unknownProcedure_throwsNotFoundException() {
        given(procedureService.getEntityById(EQ_ID, PROC_ID))
                .willThrow(new NotFoundException("Procedure", PROC_ID.toString()));

        assertThatThrownBy(() -> performHistoryService.record(EQ_ID, PROC_ID,
                new PerformRequest(LocalDate.now(), null)))
                .isInstanceOf(NotFoundException.class);

        then(performRepository).should(org.mockito.Mockito.never()).save(any());
    }
}

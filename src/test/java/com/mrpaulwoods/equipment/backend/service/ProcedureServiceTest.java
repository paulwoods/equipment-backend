package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ProcedureRequest;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProcedureServiceTest {

    private static final UUID EQ_ID = UUID.randomUUID();
    private static final UUID PROC_ID = UUID.randomUUID();
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private ProcedureRepository procedureRepository;
    @InjectMocks
    private ProcedureService procedureService;
    private Equipment equipment;
    private Procedure procedure;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setId(EQ_ID);
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 1));

        procedure = new Procedure();
        procedure.setId(PROC_ID);
        procedure.setName("Oil Change");
        procedure.setSteps("Step 1");
        procedure.setIntervalDays(30);
        procedure.setHistory(new ArrayList<>());

        equipment.setProcedures(new ArrayList<>(List.of(procedure)));
    }

    private ProcedureRequest sampleRequest() {
        return new ProcedureRequest("Oil Change", "Desc", "Step 1", "Wrench", 30);
    }

    // ─── getAllForEquipment ────────────────────────────────────────────────

    @Test
    void getAllForEquipment_returnsListOfProcedures() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);

        var result = procedureService.getAllForEquipment(EQ_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Oil Change");
        assertThat(result.getFirst().intervalDays()).isEqualTo(30);
    }

    @Test
    void getAllForEquipment_unknownEquipment_throwsNotFoundException() {
        given(equipmentService.getEntityById(EQ_ID)).willThrow(new NotFoundException("Equipment", EQ_ID.toString()));

        assertThatThrownBy(() -> procedureService.getAllForEquipment(EQ_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── getById ──────────────────────────────────────────────────────────

    @Test
    void getById_existingProcedure_returnsDetailResponse() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);

        var result = procedureService.getById(EQ_ID, PROC_ID);

        assertThat(result.id()).isEqualTo(PROC_ID);
        assertThat(result.name()).isEqualTo("Oil Change");
    }

    @Test
    void getById_procedureNotOnEquipment_throwsNotFoundException() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        var otherId = UUID.randomUUID();

        assertThatThrownBy(() -> procedureService.getById(EQ_ID, otherId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(otherId.toString());
    }

    // ─── create ───────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturnsCreateResponse() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        given(procedureRepository.save(any())).willAnswer(inv -> {
            Procedure p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var result = procedureService.create(EQ_ID, sampleRequest());

        assertThat(result.name()).isEqualTo("Oil Change");
        assertThat(result.intervalDays()).isEqualTo(30);
        then(procedureRepository).should().save(any(Procedure.class));
    }

    @Test
    void create_setsEquipmentOnProcedure() {
        var captor = org.mockito.ArgumentCaptor.forClass(Procedure.class);
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        given(procedureRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        procedureService.create(EQ_ID, sampleRequest());

        then(procedureRepository).should().save(captor.capture());
        assertThat(captor.getValue().getEquipment()).isEqualTo(equipment);
    }

    // ─── update ───────────────────────────────────────────────────────────

    @Test
    void update_existingProcedure_updatesFieldsAndReturnsResponse() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        given(procedureRepository.save(procedure)).willReturn(procedure);

        var request = new ProcedureRequest("Filter Change", "New desc", "Steps", null, 60);
        var result = procedureService.update(EQ_ID, PROC_ID, request);

        assertThat(result.name()).isEqualTo("Filter Change");
        assertThat(result.intervalDays()).isEqualTo(60);
    }

    @Test
    void update_unknownProcedure_throwsNotFoundException() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        var otherId = UUID.randomUUID();

        assertThatThrownBy(() -> procedureService.update(EQ_ID, otherId, sampleRequest()))
                .isInstanceOf(NotFoundException.class);

        then(procedureRepository).should(org.mockito.Mockito.never()).save(any());
    }

    // ─── delete ───────────────────────────────────────────────────────────

    @Test
    void delete_existingProcedure_deletesProcedure() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);

        procedureService.delete(EQ_ID, PROC_ID);

        then(procedureRepository).should().delete(procedure);
    }

    @Test
    void delete_unknownProcedure_throwsNotFoundException() {
        given(equipmentService.getEntityById(EQ_ID)).willReturn(equipment);
        var otherId = UUID.randomUUID();

        assertThatThrownBy(() -> procedureService.delete(EQ_ID, otherId))
                .isInstanceOf(NotFoundException.class);

        then(procedureRepository).should(org.mockito.Mockito.never()).delete(any());
    }
}

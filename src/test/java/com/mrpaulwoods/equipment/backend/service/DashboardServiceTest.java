package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import com.mrpaulwoods.equipment.backend.util.DueStatus;
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
import static org.mockito.Mockito.when;

@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setId(UUID.randomUUID());
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 1));
    }

    private Procedure procedureWithHistory(UUID id, String name, int intervalDays, LocalDate lastPerformed) {
        Procedure p = new Procedure();
        p.setId(id);
        p.setName(name);
        p.setIntervalDays(intervalDays);
        if (lastPerformed != null) {
            Perform perform = new Perform();
            perform.setDate(lastPerformed);
            p.setHistory(new ArrayList<>(List.of(perform)));
        } else {
            p.setHistory(new ArrayList<>());
        }
        return p;
    }

    @Test
    void getDashboardItems_whenNoEquipment_returnsEmptyList() {
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of());
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of());
        assertThat(dashboardService.getDashboardItems()).isEmpty();
    }

    @Test
    void getDashboardItems_withNullProcedures_skipsEquipment() {
        equipment.setProcedures(null);
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of());
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        assertThat(dashboardService.getDashboardItems()).isEmpty();
    }

    @Test
    void getDashboardItems_withNoHistory_statusIsNoHistory() {
        Procedure proc = procedureWithHistory(UUID.randomUUID(), "Oil Change", 30, null);
        equipment.setProcedures(List.of(proc));
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of(proc));
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).status()).isEqualTo(DueStatus.NO_HISTORY);
        assertThat(items.get(0).daysTillDue()).isNull();
        assertThat(items.get(0).dueDate()).isNull();
    }

    @Test
    void getDashboardItems_withUpcomingProcedure_statusIsUpcoming() {
        Procedure proc = procedureWithHistory(UUID.randomUUID(), "Oil Change", 30, LocalDate.now().minusDays(10));
        equipment.setProcedures(List.of(proc));
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of(proc));
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).status()).isEqualTo(DueStatus.UPCOMING);
        assertThat(items.get(0).daysTillDue()).isEqualTo(20);
    }

    @Test
    void getDashboardItems_withOverdueProcedure_statusIsOverdue() {
        Procedure proc = procedureWithHistory(UUID.randomUUID(), "Oil Change", 30, LocalDate.now().minusDays(40));
        equipment.setProcedures(List.of(proc));
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of(proc));
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).status()).isEqualTo(DueStatus.OVERDUE);
        assertThat(items.get(0).daysTillDue()).isNegative();
    }

    @Test
    void getDashboardItems_sortsByDaysTillDue_overdueThenUpcoming() {
        Procedure overdue = procedureWithHistory(UUID.randomUUID(), "Overdue", 30, LocalDate.now().minusDays(40));
        Procedure upcoming = procedureWithHistory(UUID.randomUUID(), "Upcoming", 30, LocalDate.now().minusDays(5));
        equipment.setProcedures(List.of(upcoming, overdue)); // reversed order
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of(upcoming, overdue));
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).procedureName()).isEqualTo("Overdue");
        assertThat(items.get(1).procedureName()).isEqualTo("Upcoming");
    }

    @Test
    void getDashboardItems_equipmentNameIsManufacturerPlusModel() {
        Procedure proc = procedureWithHistory(UUID.randomUUID(), "Oil Change", 30, null);
        equipment.setProcedures(List.of(proc));
        when(procedureRepository.findAllWithHistory()).thenReturn(List.of(proc));
        when(equipmentRepository.findAllWithProcedures()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).equipmentName()).isEqualTo("Acme X100");
    }
}

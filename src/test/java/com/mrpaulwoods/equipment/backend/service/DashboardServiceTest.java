package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.EquipmentStatus;
import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private DashboardService dashboardService;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setId("eq-1");
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 1));
    }

    private Procedure procedureWithHistory(String id, String name, int intervalDays, LocalDate lastPerformed) {
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
        when(equipmentService.getAll()).thenReturn(List.of());
        assertThat(dashboardService.getDashboardItems()).isEmpty();
    }

    @Test
    void getDashboardItems_withNullProcedures_skipsEquipment() {
        equipment.setProcedures(null);
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        assertThat(dashboardService.getDashboardItems()).isEmpty();
    }

    @Test
    void getDashboardItems_withNoHistory_statusIsNoHistory() {
        Procedure proc = procedureWithHistory("proc-1", "Oil Change", 30, null);
        equipment.setProcedures(List.of(proc));
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).status()).isEqualTo("No history");
        assertThat(items.get(0).daysTillDue()).isNull();
        assertThat(items.get(0).dueDate()).isNull();
    }

    @Test
    void getDashboardItems_withUpcomingProcedure_statusIsUpcoming() {
        Procedure proc = procedureWithHistory("proc-1", "Oil Change", 30, LocalDate.now().minusDays(10));
        equipment.setProcedures(List.of(proc));
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).status()).isEqualTo("Upcoming");
        assertThat(items.get(0).daysTillDue()).isEqualTo(20);
    }

    @Test
    void getDashboardItems_withOverdueProcedure_statusIsOverdue() {
        Procedure proc = procedureWithHistory("proc-1", "Oil Change", 30, LocalDate.now().minusDays(40));
        equipment.setProcedures(List.of(proc));
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).status()).isEqualTo("OVERDUE");
        assertThat(items.get(0).daysTillDue()).isNegative();
    }

    @Test
    void getDashboardItems_sortsByDaysTillDue_overdueThenUpcoming() {
        Procedure overdue = procedureWithHistory("proc-1", "Overdue", 30, LocalDate.now().minusDays(40));
        Procedure upcoming = procedureWithHistory("proc-2", "Upcoming", 30, LocalDate.now().minusDays(5));
        equipment.setProcedures(List.of(upcoming, overdue)); // reversed order
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).procedureName()).isEqualTo("Overdue");
        assertThat(items.get(1).procedureName()).isEqualTo("Upcoming");
    }

    @Test
    void getDashboardItems_equipmentNameIsManufacturerPlusModel() {
        Procedure proc = procedureWithHistory("proc-1", "Oil Change", 30, null);
        equipment.setProcedures(List.of(proc));
        when(equipmentService.getAll()).thenReturn(List.of(equipment));

        List<DashboardItem> items = dashboardService.getDashboardItems();

        assertThat(items.get(0).equipmentName()).isEqualTo("Acme X100");
    }
}

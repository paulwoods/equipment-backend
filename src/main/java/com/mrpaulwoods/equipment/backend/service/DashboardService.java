package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import com.mrpaulwoods.equipment.backend.util.DueDetails;
import com.mrpaulwoods.equipment.backend.util.DueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final ProcedureRepository procedureRepository;

    @Transactional(readOnly = true)
    public List<DashboardItem> getDashboardItems() {
        // Two separate JOIN FETCH queries avoid the MultipleBagFetchException.
        // findAllWithHistory warms the session cache so proc.getHistory() hits L1, not the DB.
        procedureRepository.findAllWithHistory();
        List<Equipment> equipment = equipmentRepository.findAllWithProcedures();

        return equipment.stream()
                .filter(eq -> eq.getProcedures() != null)
                .flatMap(eq -> eq.getProcedures().stream()
                        .map(proc -> toDashboardItem(eq, proc)))
                .sorted(Comparator.comparingInt(item ->
                        item.daysTillDue() != null ? item.daysTillDue() : Integer.MIN_VALUE))
                .toList();
    }

    private DashboardItem toDashboardItem(Equipment eq, Procedure proc) {
        Optional<DueDetails> due = DueDetails.calculate(proc);
        String equipmentName = eq.getManufacturer() + " " + eq.getModelNumber();
        return new DashboardItem(
                eq.getId().toString(),
                equipmentName,
                proc.getId().toString(),
                proc.getName(),
                proc.getDescription(),
                proc.getIntervalDays(),
                due.map(DueDetails::daysTillDue).orElse(null),
                due.map(d -> d.dueDate().toString()).orElse(null),
                due.map(DueDetails::status).orElse(DueStatus.NO_HISTORY)
        );
    }
}

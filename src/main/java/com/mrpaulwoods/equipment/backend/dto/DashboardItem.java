package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.DueStatus;

public record DashboardItem(
        String equipmentId,
        String equipmentName,
        String procedureId,
        String procedureName,
        String procedureDescription,
        int intervalDays,
        Integer daysTillDue,
        String dueDate,
        DueStatus status
) {
}

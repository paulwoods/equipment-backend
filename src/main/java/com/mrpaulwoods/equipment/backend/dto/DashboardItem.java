package com.mrpaulwoods.equipment.backend.dto;

public record DashboardItem(
        String equipmentId,
        String equipmentName,
        String procedureId,
        String procedureName,
        String procedureDescription,
        int intervalDays,
        Integer daysTillDue,
        String dueDate,
        String status
) {
}

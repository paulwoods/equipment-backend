package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.dto.PerformTransfer;
import com.mrpaulwoods.equipment.backend.dto.ProcedureTransfer;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;

/**
 * The one place that translates between the Equipment/Procedure/Perform
 * entity graph and its various DTO shapes (CRUD request/response, and the
 * shared import/export transfer). No Spring bean, no framework — just static
 * mapping methods so every caller goes through the same field list.
 */
final class EquipmentMapper {

    private EquipmentMapper() {
    }

    // ─── CRUD (EquipmentRequest / EquipmentResponse) ───────────────────────

    static Equipment toEntity(EquipmentRequest request) {
        var equipment = new Equipment();
        applyTo(equipment, request);
        return equipment;
    }

    static void applyTo(Equipment existing, EquipmentRequest request) {
        existing.setManufacturer(request.manufacturer());
        existing.setModelNumber(request.modelNumber());
        existing.setSerialNumber(request.serialNumber());
        existing.setAssetTag(request.assetTag());
        existing.setLocation(request.location());
        existing.setStatus(request.status());
        existing.setDescription(request.description());
        existing.setPurchaseDate(request.purchaseDate());
    }

    static EquipmentResponse toResponse(Equipment e) {
        return new EquipmentResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }

    // ─── Import (EquipmentTransfer -> entity graph) ────────────────────────

    static Equipment toEntity(EquipmentTransfer transfer) {
        var equipment = new Equipment();
        equipment.setManufacturer(transfer.manufacturer());
        equipment.setModelNumber(transfer.modelNumber());
        equipment.setSerialNumber(transfer.serialNumber());
        equipment.setAssetTag(transfer.assetTag());
        equipment.setLocation(transfer.location());
        equipment.setStatus(transfer.status() != null ? transfer.status() : EquipmentStatus.ACTIVE);
        equipment.setDescription(transfer.description());
        equipment.setPurchaseDate(transfer.purchaseDate());

        if (transfer.procedures() != null) {
            for (ProcedureTransfer procedureTransfer : transfer.procedures()) {
                equipment.getProcedures().add(toEntity(procedureTransfer, equipment));
            }
        }
        return equipment;
    }

    private static Procedure toEntity(ProcedureTransfer transfer, Equipment equipment) {
        var procedure = new Procedure();
        procedure.setEquipment(equipment);
        procedure.setName(transfer.name());
        procedure.setDescription(transfer.description());
        procedure.setSteps(transfer.steps());
        procedure.setRequiredTools(transfer.requiredTools());
        procedure.setIntervalDays(transfer.intervalDays());

        if (transfer.history() != null) {
            for (PerformTransfer performTransfer : transfer.history()) {
                procedure.getHistory().add(toEntity(performTransfer, procedure));
            }
        }
        return procedure;
    }

    private static Perform toEntity(PerformTransfer transfer, Procedure procedure) {
        var perform = new Perform();
        perform.setProcedure(procedure);
        perform.setDate(transfer.date());
        perform.setNotes(transfer.notes());
        return perform;
    }

    // ─── Export (entity graph -> EquipmentTransfer) ────────────────────────

    static EquipmentTransfer toTransfer(Equipment e) {
        return new EquipmentTransfer(
                e.getId().toString(),
                e.getManufacturer(),
                e.getModelNumber(),
                e.getSerialNumber(),
                e.getAssetTag(),
                e.getLocation(),
                e.getStatus(),
                e.getDescription(),
                e.getPurchaseDate(),
                e.getProcedures().stream().map(EquipmentMapper::toTransfer).toList()
        );
    }

    private static ProcedureTransfer toTransfer(Procedure p) {
        return new ProcedureTransfer(
                p.getId().toString(),
                p.getName(),
                p.getDescription(),
                p.getSteps(),
                p.getRequiredTools(),
                p.getIntervalDays(),
                p.getHistory().stream().map(EquipmentMapper::toTransfer).toList()
        );
    }

    private static PerformTransfer toTransfer(Perform perform) {
        return new PerformTransfer(
                perform.getId().toString(),
                perform.getDate(),
                perform.getNotes()
        );
    }
}

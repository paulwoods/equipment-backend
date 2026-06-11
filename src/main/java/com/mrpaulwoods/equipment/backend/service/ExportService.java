package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ExportResponse;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final EquipmentRepository equipmentRepository;
    private final ProcedureRepository procedureRepository;

    public List<ExportResponse.EquipmentExport> exportAll() {
        // Two separate JOIN FETCH queries avoid the MultipleBagFetchException.
        // findAllWithHistory warms the session cache so getHistory() hits L1, not the DB.
        procedureRepository.findAllWithHistory();
        return equipmentRepository.findAllWithProcedures().stream()
                .map(this::toExport)
                .toList();
    }

    private ExportResponse.EquipmentExport toExport(Equipment e) {
        return new ExportResponse.EquipmentExport(
                e.getId().toString(),
                e.getManufacturer(),
                e.getModelNumber(),
                e.getSerialNumber(),
                e.getAssetTag(),
                e.getLocation(),
                e.getStatus(),
                e.getDescription(),
                e.getPurchaseDate(),
                e.getProcedures().stream().map(this::toExport).toList()
        );
    }

    private ExportResponse.ProcedureExport toExport(Procedure p) {
        return new ExportResponse.ProcedureExport(
                p.getId().toString(),
                p.getName(),
                p.getDescription(),
                p.getSteps(),
                p.getRequiredTools(),
                p.getIntervalDays(),
                p.getHistory().stream().map(this::toExport).toList()
        );
    }

    private ExportResponse.PerformExport toExport(Perform perform) {
        return new ExportResponse.PerformExport(
                perform.getId().toString(),
                perform.getDate(),
                perform.getNotes()
        );
    }
}

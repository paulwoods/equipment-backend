package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ImportRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportService {

    private final EquipmentRepository equipmentRepository;

    public ImportResult importEquipment(List<ImportRequest.EquipmentImport> items) {
        validate(items);

        List<Equipment> equipmentList = new ArrayList<>();
        int procedureCount = 0;
        int historyCount = 0;

        for (ImportRequest.EquipmentImport item : items) {
            Equipment equipment = new Equipment();
            equipment.setManufacturer(item.manufacturer());
            equipment.setModelNumber(item.modelNumber());
            equipment.setSerialNumber(item.serialNumber());
            equipment.setAssetTag(item.assetTag());
            equipment.setLocation(item.location());
            equipment.setStatus(item.status() != null ? item.status() : EquipmentStatus.ACTIVE);
            equipment.setDescription(item.description());
            equipment.setPurchaseDate(item.purchaseDate());

            if (item.procedures() != null) {
                for (ImportRequest.ProcedureImport proc : item.procedures()) {
                    Procedure procedure = new Procedure();
                    procedure.setEquipment(equipment);
                    procedure.setName(proc.name());
                    procedure.setDescription(proc.description());
                    procedure.setSteps(proc.steps());
                    procedure.setRequiredTools(proc.requiredTools());
                    procedure.setIntervalDays(proc.intervalDays());

                    if (proc.history() != null) {
                        for (ImportRequest.PerformImport perf : proc.history()) {
                            Perform perform = new Perform();
                            perform.setProcedure(procedure);
                            perform.setDate(perf.date());
                            perform.setNotes(perf.notes());
                            procedure.getHistory().add(perform);
                            historyCount++;
                        }
                    }

                    equipment.getProcedures().add(procedure);
                    procedureCount++;
                }
            }

            equipmentList.add(equipment);
        }

        equipmentRepository.saveAll(equipmentList);
        return new ImportResult(equipmentList.size(), procedureCount, historyCount);
    }

    private void validate(List<ImportRequest.EquipmentImport> items) {
        for (int i = 0; i < items.size(); i++) {
            ImportRequest.EquipmentImport item = items.get(i);

            if (item.manufacturer() == null || item.manufacturer().isBlank()) {
                throw new ImportEquipmentException("Equipment[" + i + "]: manufacturer is required");
            }
            if (item.modelNumber() == null || item.modelNumber().isBlank()) {
                throw new ImportEquipmentException("Equipment[" + i + "]: modelNumber is required");
            }
            if (item.purchaseDate() == null) {
                throw new ImportEquipmentException("Equipment[" + i + "]: purchaseDate is required");
            }

            if (item.procedures() != null) {
                for (int j = 0; j < item.procedures().size(); j++) {
                    ImportRequest.ProcedureImport proc = item.procedures().get(j);

                    if (proc.name() == null || proc.name().isBlank()) {
                        throw new ImportEquipmentException("Equipment[" + i + "], Procedure[" + j + "]: name is required");
                    }
                    if (proc.steps() == null || proc.steps().isBlank()) {
                        throw new ImportEquipmentException("Equipment[" + i + "], Procedure[" + j + "]: steps is required");
                    }
                    if (proc.intervalDays() == null || proc.intervalDays() < 1) {
                        throw new ImportEquipmentException("Equipment[" + i + "], Procedure[" + j + "]: intervalDays must be at least 1");
                    }
                }
            }
        }
    }
}

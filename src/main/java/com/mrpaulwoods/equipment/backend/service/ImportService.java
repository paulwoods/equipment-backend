package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ImportRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.Validator;
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
    private final Validator validator;

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

    // Collects every violation across the whole payload so a user fixing a large
    // import sees all errors at once instead of one per attempt.
    private void validate(List<ImportRequest.EquipmentImport> items) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            validator.validate(items.get(i)).stream()
                    .map(v -> "Equipment[" + index + "]." + v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .forEach(errors::add);
        }
        if (!errors.isEmpty()) {
            throw new ImportEquipmentException(String.join("; ", errors));
        }
    }
}

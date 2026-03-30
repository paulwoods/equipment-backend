package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.exception.ProcedureNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import com.mrpaulwoods.equipment.backend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformHistoryService {

    private final EquipmentService equipmentService;

    public List<Perform> getHistory(String equipmentId, String procedureId) {
        List<Equipment> all = equipmentService.getAll();
        Procedure procedure = findProcedure(all, equipmentId, procedureId);
        return procedure.getHistory() != null ? procedure.getHistory() : new ArrayList<>();
    }

    public Perform record(String equipmentId, String procedureId, Perform perform) {
        perform.setId(IdGenerator.generate());
        List<Equipment> all = equipmentService.getAll();
        Procedure procedure = findProcedure(all, equipmentId, procedureId);
        if (procedure.getHistory() == null) {
            procedure.setHistory(new ArrayList<>());
        }
        procedure.getHistory().add(perform);
        equipmentService.save(all);
        return perform;
    }

    private Procedure findProcedure(List<Equipment> all, String equipmentId, String procedureId) {
        Equipment equipment = all.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow();
        if (equipment.getProcedures() == null) throw new ProcedureNotFoundException(procedureId);
        return equipment.getProcedures().stream()
                .filter(p -> p.getId().equals(procedureId))
                .findFirst()
                .orElseThrow(() -> new ProcedureNotFoundException(procedureId));
    }
}

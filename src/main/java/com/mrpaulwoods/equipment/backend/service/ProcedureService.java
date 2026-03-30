package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.exception.ProcedureNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import com.mrpaulwoods.equipment.backend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcedureService {

    private final EquipmentService equipmentService;

    public List<Procedure> getAllForEquipment(String equipmentId) {
        Equipment equipment = equipmentService.getById(equipmentId);
        return equipment.getProcedures() != null ? equipment.getProcedures() : new ArrayList<>();
    }

    public Procedure getById(String equipmentId, String procedureId) {
        return getAllForEquipment(equipmentId).stream()
                .filter(p -> p.getId().equals(procedureId))
                .findFirst()
                .orElseThrow(() -> new ProcedureNotFoundException(procedureId));
    }

    public Procedure create(String equipmentId, Procedure procedure) {
        procedure.setId(IdGenerator.generate());
        List<Equipment> all = equipmentService.getAll();
        Equipment equipment = all.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow();
        if (equipment.getProcedures() == null) {
            equipment.setProcedures(new ArrayList<>());
        }
        equipment.getProcedures().add(procedure);
        equipmentService.save(all);
        return procedure;
    }

    public Procedure update(String equipmentId, String procedureId, Procedure updated) {
        List<Equipment> all = equipmentService.getAll();
        Equipment equipment = all.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow();
        List<Procedure> procedures = equipment.getProcedures();
        if (procedures == null) throw new ProcedureNotFoundException(procedureId);
        int index = -1;
        for (int i = 0; i < procedures.size(); i++) {
            if (procedures.get(i).getId().equals(procedureId)) {
                index = i;
                break;
            }
        }
        if (index == -1) throw new ProcedureNotFoundException(procedureId);
        updated.setId(procedureId);
        // preserve existing history
        updated.setHistory(procedures.get(index).getHistory());
        procedures.set(index, updated);
        equipmentService.save(all);
        return updated;
    }

    public void delete(String equipmentId, String procedureId) {
        List<Equipment> all = equipmentService.getAll();
        Equipment equipment = all.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow();
        if (equipment.getProcedures() == null ||
            !equipment.getProcedures().removeIf(p -> p.getId().equals(procedureId))) {
            throw new ProcedureNotFoundException(procedureId);
        }
        equipmentService.save(all);
    }
}

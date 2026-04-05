package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.ProcedureNotFoundException;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcedureService {

    private final EquipmentService equipmentService;
    private final ProcedureRepository procedureRepository;

    @Transactional(readOnly = true)
    public List<Procedure> getAllForEquipment(UUID equipmentId) {
        Equipment equipment = equipmentService.getById(equipmentId);
        return equipment.getProcedures();
    }

    @Transactional(readOnly = true)
    public Procedure getById(UUID equipmentId, UUID procedureId) {
        return getAllForEquipment(equipmentId).stream()
                .filter(p -> p.getId().equals(procedureId))
                .findFirst()
                .orElseThrow(() -> new ProcedureNotFoundException(procedureId.toString()));
    }

    public Procedure create(UUID equipmentId, Procedure procedure) {
        Equipment equipment = equipmentService.getById(equipmentId);
        procedure.setEquipment(equipment);
        return procedureRepository.save(procedure);
    }

    public Procedure update(UUID equipmentId, UUID procedureId, Procedure updated) {
        Procedure existing = getById(equipmentId, procedureId);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setSteps(updated.getSteps());
        existing.setRequiredTools(updated.getRequiredTools());
        existing.setIntervalDays(updated.getIntervalDays());
        return procedureRepository.save(existing);
    }

    public void delete(UUID equipmentId, UUID procedureId) {
        Procedure procedure = getById(equipmentId, procedureId);
        procedureRepository.delete(procedure);
    }
}

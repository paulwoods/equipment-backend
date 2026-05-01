package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.ProcedureRequest;
import com.mrpaulwoods.equipment.backend.dto.ProcedureResponse;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcedureService {

    private final EquipmentService equipmentService;
    private final ProcedureRepository procedureRepository;

    public List<ProcedureResponse> getAllForEquipment(UUID equipmentId) {
        var equipment = equipmentService.getEntityById(equipmentId);
        return equipment.getProcedures().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProcedureResponse getById(UUID equipmentId, UUID procedureId) {
        return toResponse(getEntityById(equipmentId, procedureId));
    }

    Procedure getEntityById(UUID equipmentId, UUID procedureId) {
        var equipment = equipmentService.getEntityById(equipmentId);
        return equipment.getProcedures().stream()
                .filter(p -> p.getId().equals(procedureId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Procedure", procedureId.toString()));
    }

    @Transactional
    public ProcedureResponse create(UUID equipmentId, ProcedureRequest request) {
        var equipment = equipmentService.getEntityById(equipmentId);
        var procedure = toEntity(request);
        procedure.setEquipment(equipment);
        return toResponse(procedureRepository.save(procedure));
    }

    @Transactional
    public ProcedureResponse update(UUID equipmentId, UUID procedureId, ProcedureRequest request) {
        var existing = getEntityById(equipmentId, procedureId);
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setSteps(request.steps());
        existing.setRequiredTools(request.requiredTools());
        existing.setIntervalDays(request.intervalDays());
        return toResponse(procedureRepository.save(existing));
    }

    @Transactional
    public void delete(UUID equipmentId, UUID procedureId) {
        var procedure = getEntityById(equipmentId, procedureId);
        procedureRepository.delete(procedure);
    }

    private Procedure toEntity(ProcedureRequest request) {
        var p = new Procedure();
        p.setName(request.name());
        p.setDescription(request.description());
        p.setSteps(request.steps());
        p.setRequiredTools(request.requiredTools());
        p.setIntervalDays(request.intervalDays());
        return p;
    }

    private ProcedureResponse toResponse(Procedure p) {
        return new ProcedureResponse(p.getId(), p.getName(), p.getDescription(),
                p.getSteps(), p.getRequiredTools(), p.getIntervalDays());
    }
}

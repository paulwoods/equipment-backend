package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
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
@Transactional(readOnly = true)
public class ProcedureService {

    private final EquipmentService equipmentService;
    private final ProcedureRepository procedureRepository;

    public List<ProcedureListResponse> getAllForEquipment(UUID equipmentId) {
        var equipment = equipmentService.getEntityById(equipmentId);
        return equipment.getProcedures().stream()
                .map(this::toListResponse)
                .toList();
    }

    public ProcedureDetailResponse getById(UUID equipmentId, UUID procedureId) {
        return toDetailResponse(getEntityById(equipmentId, procedureId));
    }

    Procedure getEntityById(UUID equipmentId, UUID procedureId) {
        var equipment = equipmentService.getEntityById(equipmentId);
        return equipment.getProcedures().stream()
                .filter(p -> p.getId().equals(procedureId))
                .findFirst()
                .orElseThrow(() -> new ProcedureNotFoundException(procedureId.toString()));
    }

    @Transactional
    public ProcedureCreateResponse create(UUID equipmentId, ProcedureRequest request) {
        var equipment = equipmentService.getEntityById(equipmentId);
        var procedure = toEntity(request);
        procedure.setEquipment(equipment);
        return toCreateResponse(procedureRepository.save(procedure));
    }

    @Transactional
    public ProcedureUpdateResponse update(UUID equipmentId, UUID procedureId, ProcedureRequest request) {
        var existing = getEntityById(equipmentId, procedureId);
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setSteps(request.steps());
        existing.setRequiredTools(request.requiredTools());
        existing.setIntervalDays(request.intervalDays());
        return toUpdateResponse(procedureRepository.save(existing));
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

    private ProcedureListResponse toListResponse(Procedure p) {
        return new ProcedureListResponse(p.getId(), p.getName(), p.getDescription(),
                p.getSteps(), p.getRequiredTools(), p.getIntervalDays());
    }

    private ProcedureDetailResponse toDetailResponse(Procedure p) {
        return new ProcedureDetailResponse(p.getId(), p.getName(), p.getDescription(),
                p.getSteps(), p.getRequiredTools(), p.getIntervalDays());
    }

    private ProcedureCreateResponse toCreateResponse(Procedure p) {
        return new ProcedureCreateResponse(p.getId(), p.getName(), p.getDescription(),
                p.getSteps(), p.getRequiredTools(), p.getIntervalDays());
    }

    private ProcedureUpdateResponse toUpdateResponse(Procedure p) {
        return new ProcedureUpdateResponse(p.getId(), p.getName(), p.getDescription(),
                p.getSteps(), p.getRequiredTools(), p.getIntervalDays());
    }
}

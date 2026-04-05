package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.ProcedureRequest;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.service.ProcedureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public List<Procedure> getAll(@PathVariable UUID equipmentId) {
        return procedureService.getAllForEquipment(equipmentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{procedureId}")
    public Procedure getById(@PathVariable UUID equipmentId, @PathVariable UUID procedureId) {
        return procedureService.getById(equipmentId, procedureId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedure create(@PathVariable UUID equipmentId,
                            @Valid @RequestBody ProcedureRequest request) {
        Procedure procedure = toProcedure(request);
        return procedureService.create(equipmentId, procedure);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{procedureId}")
    public Procedure update(@PathVariable UUID equipmentId,
                            @PathVariable UUID procedureId,
                            @Valid @RequestBody ProcedureRequest request) {
        Procedure procedure = toProcedure(request);
        return procedureService.update(equipmentId, procedureId, procedure);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{procedureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID equipmentId, @PathVariable UUID procedureId) {
        procedureService.delete(equipmentId, procedureId);
    }

    private Procedure toProcedure(ProcedureRequest request) {
        Procedure procedure = new Procedure();
        procedure.setName(request.name());
        procedure.setDescription(request.description());
        procedure.setSteps(request.steps());
        procedure.setRequiredTools(request.requiredTools());
        procedure.setIntervalDays(request.intervalDays());
        return procedure;
    }
}

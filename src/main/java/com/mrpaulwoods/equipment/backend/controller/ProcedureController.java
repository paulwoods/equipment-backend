package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.ProcedureRequest;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import com.mrpaulwoods.equipment.backend.service.ProcedureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;

    @GetMapping
    public List<Procedure> getAll(@PathVariable String equipmentId) {
        return procedureService.getAllForEquipment(equipmentId);
    }

    @GetMapping("/{procedureId}")
    public Procedure getById(@PathVariable String equipmentId, @PathVariable String procedureId) {
        return procedureService.getById(equipmentId, procedureId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedure create(@PathVariable String equipmentId,
                            @Valid @RequestBody ProcedureRequest request) {
        Procedure procedure = toProcedure(request);
        return procedureService.create(equipmentId, procedure);
    }

    @PutMapping("/{procedureId}")
    public Procedure update(@PathVariable String equipmentId,
                            @PathVariable String procedureId,
                            @Valid @RequestBody ProcedureRequest request) {
        Procedure procedure = toProcedure(request);
        return procedureService.update(equipmentId, procedureId, procedure);
    }

    @DeleteMapping("/{procedureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String equipmentId, @PathVariable String procedureId) {
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

package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.PerformRequest;
import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.service.PerformHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/procedures/{procedureId}/history")
@RequiredArgsConstructor
public class PerformHistoryController {

    private final PerformHistoryService performHistoryService;

    @GetMapping
    public List<Perform> getHistory(@PathVariable String equipmentId,
                                    @PathVariable String procedureId) {
        return performHistoryService.getHistory(equipmentId, procedureId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Perform record(@PathVariable String equipmentId,
                          @PathVariable String procedureId,
                          @Valid @RequestBody PerformRequest request) {
        Perform perform = new Perform();
        perform.setDate(request.date());
        perform.setNotes(request.notes());
        return performHistoryService.record(equipmentId, procedureId, perform);
    }
}

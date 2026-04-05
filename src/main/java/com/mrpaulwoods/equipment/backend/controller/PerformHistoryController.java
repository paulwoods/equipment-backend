package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.PerformRequest;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.service.PerformHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/procedures/{procedureId}/history")
@RequiredArgsConstructor
public class PerformHistoryController {

    private final PerformHistoryService performHistoryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public List<Perform> getHistory(@PathVariable UUID equipmentId,
                                    @PathVariable UUID procedureId) {
        return performHistoryService.getHistory(equipmentId, procedureId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Perform record(@PathVariable UUID equipmentId,
                          @PathVariable UUID procedureId,
                          @Valid @RequestBody PerformRequest request) {
        Perform perform = new Perform();
        perform.setDate(request.date());
        perform.setNotes(request.notes());
        return performHistoryService.record(equipmentId, procedureId, perform);
    }
}

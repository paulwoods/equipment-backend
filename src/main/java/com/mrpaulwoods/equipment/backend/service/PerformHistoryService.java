package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.PerformRequest;
import com.mrpaulwoods.equipment.backend.dto.PerformResponse;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.repository.PerformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformHistoryService {

    private final ProcedureService procedureService;
    private final PerformRepository performRepository;

    public List<PerformResponse> getHistory(UUID equipmentId, UUID procedureId) {
        var procedure = procedureService.getEntityById(equipmentId, procedureId);
        return procedure.getHistory().stream()
                .map(p -> new PerformResponse(p.getId(), p.getDate(), p.getNotes()))
                .toList();
    }

    @Transactional
    public PerformResponse record(UUID equipmentId, UUID procedureId, PerformRequest request) {
        var procedure = procedureService.getEntityById(equipmentId, procedureId);
        var perform = new Perform();
        perform.setDate(request.date());
        perform.setNotes(request.notes());
        perform.setProcedure(procedure);
        var saved = performRepository.save(perform);
        return new PerformResponse(saved.getId(), saved.getDate(), saved.getNotes());
    }
}

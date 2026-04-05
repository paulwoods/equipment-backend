package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.repository.PerformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformHistoryService {

    private final ProcedureService procedureService;
    private final PerformRepository performRepository;

    @Transactional(readOnly = true)
    public List<Perform> getHistory(UUID equipmentId, UUID procedureId) {
        Procedure procedure = procedureService.getById(equipmentId, procedureId);
        return procedure.getHistory();
    }

    public Perform record(UUID equipmentId, UUID procedureId, Perform perform) {
        Procedure procedure = procedureService.getById(equipmentId, procedureId);
        perform.setProcedure(procedure);
        return performRepository.save(perform);
    }
}

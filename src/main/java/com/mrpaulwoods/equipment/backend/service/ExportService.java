package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final EquipmentRepository equipmentRepository;
    private final ProcedureRepository procedureRepository;

    public List<EquipmentTransfer> exportAll() {
        // Two separate JOIN FETCH queries avoid the MultipleBagFetchException.
        // findAllWithHistory warms the session cache so getHistory() hits L1, not the DB.
        procedureRepository.findAllWithHistory();
        return equipmentRepository.findAllWithProcedures().stream()
                .map(EquipmentMapper::toTransfer)
                .toList();
    }
}

package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final EquipmentRepository equipmentRepository;

    public List<Equipment> exportAll() {
        List<Equipment> equipment = equipmentRepository.findAllWithProcedures();
        // Initialize history in a separate pass to avoid MultipleBagFetchException
        equipment.forEach(e -> e.getProcedures().forEach(p -> p.getHistory().size()));
        return equipment;
    }
}

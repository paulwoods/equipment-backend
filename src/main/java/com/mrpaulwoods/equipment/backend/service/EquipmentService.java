package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Equipment getById(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException(id.toString()));
    }

    public Equipment create(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public Equipment update(UUID id, Equipment updated) {
        Equipment existing = getById(id);
        existing.setManufacturer(updated.getManufacturer());
        existing.setModelNumber(updated.getModelNumber());
        existing.setSerialNumber(updated.getSerialNumber());
        existing.setAssetTag(updated.getAssetTag());
        existing.setLocation(updated.getLocation());
        existing.setStatus(updated.getStatus());
        existing.setDescription(updated.getDescription());
        existing.setPurchaseDate(updated.getPurchaseDate());
        return equipmentRepository.save(existing);
    }

    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new EquipmentNotFoundException(id.toString());
        }
        equipmentRepository.deleteById(id);
    }
}

package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public Page<EquipmentResponse> getAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(EquipmentMapper::toResponse);
    }

    public EquipmentResponse getById(UUID id) {
        return equipmentRepository.findById(id)
                .map(EquipmentMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Equipment", id.toString()));
    }

    Equipment getEntityById(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment", id.toString()));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        var equipment = EquipmentMapper.toEntity(request);
        return EquipmentMapper.toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(UUID id, EquipmentRequest request) {
        var existing = getEntityById(id);
        EquipmentMapper.applyTo(existing, request);
        return EquipmentMapper.toResponse(equipmentRepository.save(existing));
    }

    @Transactional
    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new NotFoundException("Equipment", id.toString());
        }
        equipmentRepository.deleteById(id);
    }
}

package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportService {

    private final EquipmentRepository equipmentRepository;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public ImportResult importEquipment(InputStream inputStream) throws IOException {
        List<EquipmentTransfer> items = parse(inputStream);
        validate(items);

        List<Equipment> equipmentList = items.stream().map(EquipmentMapper::toEntity).toList();

        int procedureCount = equipmentList.stream()
                .mapToInt(e -> e.getProcedures().size())
                .sum();
        int historyCount = equipmentList.stream()
                .flatMap(e -> e.getProcedures().stream())
                .mapToInt(p -> p.getHistory().size())
                .sum();

        equipmentRepository.saveAll(equipmentList);
        return new ImportResult(equipmentList.size(), procedureCount, historyCount);
    }

    private List<EquipmentTransfer> parse(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            throw new ImportEquipmentException("Import file is empty");
        }
        try {
            return objectMapper.readValue(
                    bytes,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EquipmentTransfer.class)
            );
        } catch (StreamReadException e) {
            throw new ImportEquipmentException("Invalid JSON in import file: " + e.getOriginalMessage());
        }
    }

    // Collects every violation across the whole payload so a user fixing a large
    // import sees all errors at once instead of one per attempt.
    private void validate(List<EquipmentTransfer> items) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            validator.validate(items.get(i)).stream()
                    .map(v -> "Equipment[" + index + "]." + v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .forEach(errors::add);
        }
        if (!errors.isEmpty()) {
            throw new ImportEquipmentException(String.join("; ", errors));
        }
    }
}

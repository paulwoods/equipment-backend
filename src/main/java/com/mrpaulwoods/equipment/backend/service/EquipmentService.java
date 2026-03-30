package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentService {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private Path dataFile() {
        return Path.of(appProperties.getDataDir(), "equipment.json");
    }

    public synchronized List<Equipment> getAll() {
        Path file = dataFile();
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(file.toFile(), new TypeReference<List<Equipment>>() {
        });
    }

    public Equipment getById(String id) {
        return getAll().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new EquipmentNotFoundException(id));
    }

    public Equipment create(Equipment equipment) {
        equipment.setId(IdGenerator.generate());
        List<Equipment> all = getAll();
        all.add(equipment);
        save(all);
        return equipment;
    }

    public Equipment update(String id, Equipment updated) {
        List<Equipment> all = getAll();
        int index = indexOf(all, id);
        updated.setId(id);
        all.set(index, updated);
        save(all);
        return updated;
    }

    public void delete(String id) {
        List<Equipment> all = getAll();
        indexOf(all, id); // validates existence
        all.removeIf(e -> e.getId().equals(id));
        save(all);
    }

    public synchronized void save(List<Equipment> equipment) {
        Path file = dataFile();
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), equipment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save equipment data", e);
        }
    }

    private int indexOf(List<Equipment> all, String id) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) return i;
        }
        throw new EquipmentNotFoundException(id);
    }
}

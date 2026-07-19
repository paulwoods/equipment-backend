package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.entity.Perform;
import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentMapperTest {

    // ─── CRUD shape ─────────────────────────────────────────────────────────

    @Test
    void toEntity_fromRequest_copiesAllFields() {
        var request = new EquipmentRequest("Acme", "X100", "SN-1", "TAG-1", "Lab",
                EquipmentStatus.ACTIVE, "A pump", LocalDate.of(2024, 1, 1));

        Equipment entity = EquipmentMapper.toEntity(request);

        assertThat(entity.getManufacturer()).isEqualTo("Acme");
        assertThat(entity.getModelNumber()).isEqualTo("X100");
        assertThat(entity.getSerialNumber()).isEqualTo("SN-1");
        assertThat(entity.getAssetTag()).isEqualTo("TAG-1");
        assertThat(entity.getLocation()).isEqualTo("Lab");
        assertThat(entity.getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(entity.getDescription()).isEqualTo("A pump");
        assertThat(entity.getPurchaseDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void applyTo_overwritesAllFieldsOnExistingEntity() {
        Equipment existing = new Equipment();
        existing.setId(UUID.randomUUID());
        existing.setManufacturer("Old");
        existing.setModelNumber("Old100");

        var request = new EquipmentRequest("NewCorp", "Z300", null, null, null,
                EquipmentStatus.UNDER_REPAIR, null, LocalDate.of(2024, 3, 1));
        EquipmentMapper.applyTo(existing, request);

        assertThat(existing.getManufacturer()).isEqualTo("NewCorp");
        assertThat(existing.getModelNumber()).isEqualTo("Z300");
        assertThat(existing.getStatus()).isEqualTo(EquipmentStatus.UNDER_REPAIR);
        assertThat(existing.getPurchaseDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void toResponse_copiesAllFieldsIncludingId() {
        UUID id = UUID.randomUUID();
        Equipment entity = new Equipment();
        entity.setId(id);
        entity.setManufacturer("Acme");
        entity.setModelNumber("X100");
        entity.setSerialNumber("SN-1");
        entity.setAssetTag("TAG-1");
        entity.setLocation("Lab");
        entity.setStatus(EquipmentStatus.ACTIVE);
        entity.setDescription("A pump");
        entity.setPurchaseDate(LocalDate.of(2024, 1, 1));

        EquipmentResponse response = EquipmentMapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.manufacturer()).isEqualTo("Acme");
        assertThat(response.modelNumber()).isEqualTo("X100");
        assertThat(response.serialNumber()).isEqualTo("SN-1");
        assertThat(response.assetTag()).isEqualTo("TAG-1");
        assertThat(response.location()).isEqualTo("Lab");
        assertThat(response.status()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(response.description()).isEqualTo("A pump");
        assertThat(response.purchaseDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    // ─── Import/export round trip ──────────────────────────────────────────

    private Equipment sampleEquipmentGraph() {
        Equipment equipment = new Equipment();
        equipment.setId(UUID.randomUUID());
        equipment.setManufacturer("Acme");
        equipment.setModelNumber("X100");
        equipment.setSerialNumber("SN-1");
        equipment.setAssetTag("TAG-1");
        equipment.setLocation("Lab");
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipment.setDescription("A pump");
        equipment.setPurchaseDate(LocalDate.of(2024, 1, 1));

        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setEquipment(equipment);
        procedure.setName("Oil change");
        procedure.setDescription("Routine maintenance");
        procedure.setSteps("Drain and refill");
        procedure.setRequiredTools("Wrench");
        procedure.setIntervalDays(90);
        equipment.getProcedures().add(procedure);

        Perform perform = new Perform();
        perform.setId(UUID.randomUUID());
        perform.setProcedure(procedure);
        perform.setDate(LocalDate.of(2024, 6, 1));
        perform.setNotes("Done");
        procedure.getHistory().add(perform);

        return equipment;
    }

    @Test
    void toTransfer_mapsFullGraphIncludingProceduresAndHistory() {
        Equipment equipment = sampleEquipmentGraph();

        EquipmentTransfer transfer = EquipmentMapper.toTransfer(equipment);

        assertThat(transfer.id()).isEqualTo(equipment.getId().toString());
        assertThat(transfer.manufacturer()).isEqualTo("Acme");
        assertThat(transfer.procedures()).hasSize(1);
        var procedureTransfer = transfer.procedures().getFirst();
        assertThat(procedureTransfer.name()).isEqualTo("Oil change");
        assertThat(procedureTransfer.history()).hasSize(1);
        assertThat(procedureTransfer.history().getFirst().notes()).isEqualTo("Done");
    }

    @Test
    void exportThenImport_roundTripsToAnEquivalentEntityGraph() {
        // Proves the invariant mechanically: whatever exportAll() would hand back
        // for an equipment graph, importEquipment can consume directly and get
        // back an equivalent graph (ids aside — those are re-generated on import).
        Equipment original = sampleEquipmentGraph();

        EquipmentTransfer exported = EquipmentMapper.toTransfer(original);
        Equipment reimported = EquipmentMapper.toEntity(exported);

        assertThat(reimported.getManufacturer()).isEqualTo(original.getManufacturer());
        assertThat(reimported.getModelNumber()).isEqualTo(original.getModelNumber());
        assertThat(reimported.getSerialNumber()).isEqualTo(original.getSerialNumber());
        assertThat(reimported.getAssetTag()).isEqualTo(original.getAssetTag());
        assertThat(reimported.getLocation()).isEqualTo(original.getLocation());
        assertThat(reimported.getStatus()).isEqualTo(original.getStatus());
        assertThat(reimported.getDescription()).isEqualTo(original.getDescription());
        assertThat(reimported.getPurchaseDate()).isEqualTo(original.getPurchaseDate());

        assertThat(reimported.getProcedures()).hasSize(original.getProcedures().size());
        Procedure originalProcedure = original.getProcedures().getFirst();
        Procedure reimportedProcedure = reimported.getProcedures().getFirst();
        assertThat(reimportedProcedure.getName()).isEqualTo(originalProcedure.getName());
        assertThat(reimportedProcedure.getDescription()).isEqualTo(originalProcedure.getDescription());
        assertThat(reimportedProcedure.getSteps()).isEqualTo(originalProcedure.getSteps());
        assertThat(reimportedProcedure.getRequiredTools()).isEqualTo(originalProcedure.getRequiredTools());
        assertThat(reimportedProcedure.getIntervalDays()).isEqualTo(originalProcedure.getIntervalDays());
        assertThat(reimportedProcedure.getEquipment()).isSameAs(reimported);

        assertThat(reimportedProcedure.getHistory()).hasSize(originalProcedure.getHistory().size());
        Perform originalPerform = originalProcedure.getHistory().getFirst();
        Perform reimportedPerform = reimportedProcedure.getHistory().getFirst();
        assertThat(reimportedPerform.getDate()).isEqualTo(originalPerform.getDate());
        assertThat(reimportedPerform.getNotes()).isEqualTo(originalPerform.getNotes());
        assertThat(reimportedPerform.getProcedure()).isSameAs(reimportedProcedure);
    }

    @Test
    void toEntity_fromTransfer_nullStatus_defaultsToActive() {
        var transfer = new EquipmentTransfer("old-id", "Dell", "G15", null, null, null,
                null, null, LocalDate.of(2020, 1, 1), null);

        Equipment entity = EquipmentMapper.toEntity(transfer);

        assertThat(entity.getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getProcedures()).isEmpty();
    }
}

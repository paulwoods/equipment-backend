package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    private Equipment sampleEquipment(UUID id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setSerialNumber("SN-001");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 15));
        return e;
    }

    private EquipmentRequest sampleRequest() {
        return new EquipmentRequest("Acme", "X100", "SN-001", null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2024, 1, 15));
    }

    // ─── getAll ───────────────────────────────────────────────────────────

    @Test
    void getAll_returnsPagedListResponse() {
        var id = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        given(equipmentRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(sampleEquipment(id)), pageable, 1));

        var result = equipmentService.getAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(id);
        assertThat(result.getContent().getFirst().manufacturer()).isEqualTo("Acme");
    }

    @Test
    void getAll_emptyRepository_returnsEmptyPage() {
        var pageable = PageRequest.of(0, 20);
        given(equipmentRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of()));

        var result = equipmentService.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ─── getById ──────────────────────────────────────────────────────────

    @Test
    void getById_existingId_returnsDetailResponse() {
        var id = UUID.randomUUID();
        given(equipmentRepository.findById(id)).willReturn(Optional.of(sampleEquipment(id)));

        var result = equipmentService.getById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.modelNumber()).isEqualTo("X100");
    }

    @Test
    void getById_nonExistingId_throwsEquipmentNotFoundException() {
        var id = UUID.randomUUID();
        given(equipmentRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.getById(id))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── create ───────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturnsCreateResponse() {
        var id = UUID.randomUUID();
        var request = sampleRequest();
        given(equipmentRepository.save(any())).willAnswer(inv -> {
            Equipment e = inv.getArgument(0);
            e.setId(id);
            return e;
        });

        var result = equipmentService.create(request);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.manufacturer()).isEqualTo("Acme");
        assertThat(result.status()).isEqualTo(EquipmentStatus.ACTIVE);
        then(equipmentRepository).should().save(any(Equipment.class));
    }

    @Test
    void create_mapsAllFieldsToEntity() {
        var request = new EquipmentRequest("Corp", "Y200", "SN-002", "AT-99", "Warehouse",
                EquipmentStatus.IN_USE, "A machine", LocalDate.of(2023, 6, 1));
        given(equipmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var captor = ArgumentCaptor.forClass(Equipment.class);
        equipmentService.create(request);

        then(equipmentRepository).should().save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getManufacturer()).isEqualTo("Corp");
        assertThat(saved.getModelNumber()).isEqualTo("Y200");
        assertThat(saved.getAssetTag()).isEqualTo("AT-99");
        assertThat(saved.getLocation()).isEqualTo("Warehouse");
        assertThat(saved.getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        assertThat(saved.getDescription()).isEqualTo("A machine");
        assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.of(2023, 6, 1));
    }

    // ─── update ───────────────────────────────────────────────────────────

    @Test
    void update_existingId_updatesFieldsAndReturnsResponse() {
        var id = UUID.randomUUID();
        var existing = sampleEquipment(id);
        given(equipmentRepository.findById(id)).willReturn(Optional.of(existing));
        given(equipmentRepository.save(existing)).willReturn(existing);

        var request = new EquipmentRequest("NewCorp", "Z300", null, null, null,
                EquipmentStatus.UNDER_REPAIR, null, LocalDate.of(2024, 3, 1));
        var result = equipmentService.update(id, request);

        assertThat(result.manufacturer()).isEqualTo("NewCorp");
        assertThat(result.modelNumber()).isEqualTo("Z300");
        assertThat(result.status()).isEqualTo(EquipmentStatus.UNDER_REPAIR);
    }

    @Test
    void update_nonExistingId_throwsEquipmentNotFoundException() {
        var id = UUID.randomUUID();
        given(equipmentRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.update(id, sampleRequest()))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining(id.toString());

        then(equipmentRepository).should(never()).save(any());
    }

    // ─── delete ───────────────────────────────────────────────────────────

    @Test
    void delete_existingId_deletesById() {
        var id = UUID.randomUUID();
        given(equipmentRepository.existsById(id)).willReturn(true);

        equipmentService.delete(id);

        then(equipmentRepository).should().deleteById(id);
    }

    @Test
    void delete_nonExistingId_throwsEquipmentNotFoundException() {
        var id = UUID.randomUUID();
        given(equipmentRepository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> equipmentService.delete(id))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining(id.toString());

        then(equipmentRepository).should(never()).deleteById(any());
    }
}

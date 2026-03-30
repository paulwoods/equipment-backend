package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.model.EquipmentStatus;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EquipmentControllerTest {

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private EquipmentController equipmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(equipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Equipment sampleEquipment() {
        Equipment e = new Equipment();
        e.setId("eq-1");
        e.setManufacturer("Acme");
        e.setModelNumber("X100");
        e.setSerialNumber("SN-001");
        e.setStatus(EquipmentStatus.ACTIVE);
        e.setPurchaseDate(LocalDate.of(2024, 1, 15));
        return e;
    }

    @Test
    void getAll_returnsEquipmentList() throws Exception {
        when(equipmentService.getAll()).thenReturn(List.of(sampleEquipment()));

        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("eq-1"))
                .andExpect(jsonPath("$[0].manufacturer").value("Acme"));
    }

    @Test
    void getById_whenFound_returnsEquipment() throws Exception {
        when(equipmentService.getById("eq-1")).thenReturn(sampleEquipment());

        mockMvc.perform(get("/api/equipment/eq-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("eq-1"))
                .andExpect(jsonPath("$.modelNumber").value("X100"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(equipmentService.getById("missing")).thenThrow(new EquipmentNotFoundException("missing"));

        mockMvc.perform(get("/api/equipment/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Equipment not found: missing"));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        when(equipmentService.create(any())).thenReturn(sampleEquipment());

        String body = """
                {
                  "manufacturer": "Acme",
                  "modelNumber": "X100",
                  "status": "Active",
                  "purchaseDate": "2024-01-15"
                }
                """;

        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("eq-1"));
    }

    @Test
    void create_withMissingRequiredFields_returns400() throws Exception {
        String body = """
                {
                  "modelNumber": "X100"
                }
                """;

        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void update_withValidBody_returnsUpdatedEquipment() throws Exception {
        Equipment updated = sampleEquipment();
        updated.setManufacturer("NewCorp");
        when(equipmentService.update(eq("eq-1"), any())).thenReturn(updated);

        String body = """
                {
                  "manufacturer": "NewCorp",
                  "modelNumber": "X100",
                  "status": "Active",
                  "purchaseDate": "2024-01-15"
                }
                """;

        mockMvc.perform(put("/api/equipment/eq-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("NewCorp"));
    }

    @Test
    void delete_whenFound_returns204() throws Exception {
        doNothing().when(equipmentService).delete("eq-1");

        mockMvc.perform(delete("/api/equipment/eq-1"))
                .andExpect(status().isNoContent());

        verify(equipmentService).delete("eq-1");
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        doThrow(new EquipmentNotFoundException("missing")).when(equipmentService).delete("missing");

        mockMvc.perform(delete("/api/equipment/missing"))
                .andExpect(status().isNotFound());
    }
}

package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import com.mrpaulwoods.equipment.backend.service.ExportService;
import com.mrpaulwoods.equipment.backend.service.ImportService;
import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    @Mock
    private ExportService exportService;

    @Mock
    private ImportService importService;

    @InjectMocks
    private EquipmentController equipmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final UUID EQ_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Inject real ObjectMapper into the controller (it's a final field via @RequiredArgsConstructor)
        equipmentController = new EquipmentController(equipmentService, exportService, importService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(equipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Equipment sampleEquipment() {
        Equipment e = new Equipment();
        e.setId(EQ_ID);
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
                .andExpect(jsonPath("$[0].id").value(EQ_ID.toString()))
                .andExpect(jsonPath("$[0].manufacturer").value("Acme"));
    }

    @Test
    void getById_whenFound_returnsEquipment() throws Exception {
        when(equipmentService.getById(EQ_ID)).thenReturn(sampleEquipment());

        mockMvc.perform(get("/api/equipment/{id}", EQ_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EQ_ID.toString()))
                .andExpect(jsonPath("$.modelNumber").value("X100"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        when(equipmentService.getById(missing)).thenThrow(new EquipmentNotFoundException(missing.toString()));

        mockMvc.perform(get("/api/equipment/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Equipment not found: " + missing));
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
                .andExpect(jsonPath("$.id").value(EQ_ID.toString()));
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
        when(equipmentService.update(eq(EQ_ID), any())).thenReturn(updated);

        String body = """
                {
                  "manufacturer": "NewCorp",
                  "modelNumber": "X100",
                  "status": "Active",
                  "purchaseDate": "2024-01-15"
                }
                """;

        mockMvc.perform(put("/api/equipment/{id}", EQ_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("NewCorp"));
    }

    @Test
    void delete_whenFound_returns204() throws Exception {
        doNothing().when(equipmentService).delete(EQ_ID);

        mockMvc.perform(delete("/api/equipment/{id}", EQ_ID))
                .andExpect(status().isNoContent());

        verify(equipmentService).delete(EQ_ID);
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(new EquipmentNotFoundException(missing.toString())).when(equipmentService).delete(missing);

        mockMvc.perform(delete("/api/equipment/{id}", missing))
                .andExpect(status().isNotFound());
    }

    @Test
    void importEquipment_withValidJson_returns200WithCounts() throws Exception {
        ImportResult result = new ImportResult(2, 3, 5);
        when(importService.importEquipment(any())).thenReturn(result);

        String json = """
                [
                  {
                    "manufacturer": "Dell",
                    "modelNumber": "G15",
                    "status": "Active",
                    "purchaseDate": "2020-12-28",
                    "procedures": []
                  }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile("file", "equipment.json", "application/json", json.getBytes());

        mockMvc.perform(multipart("/api/equipment/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentImported").value(2))
                .andExpect(jsonPath("$.proceduresImported").value(3))
                .andExpect(jsonPath("$.historyImported").value(5));
    }

    @Test
    void importEquipment_withEmptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "equipment.json", "application/json", new byte[0]);

        mockMvc.perform(multipart("/api/equipment/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Import file is empty"));
    }

    @Test
    void importEquipment_withInvalidJson_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "equipment.json", "application/json", "not json".getBytes());

        mockMvc.perform(multipart("/api/equipment/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void exportEquipment_returnsJsonFile() throws Exception {
        when(exportService.exportAll()).thenReturn(List.of(sampleEquipment()));

        mockMvc.perform(get("/api/equipment/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EQ_ID.toString()))
                .andExpect(jsonPath("$[0].manufacturer").value("Acme"))
                .andExpect(result -> {
                    String contentDisposition = result.getResponse().getHeader("Content-Disposition");
                    assert contentDisposition != null && contentDisposition.contains("equipment-export-");
                    assert contentDisposition.endsWith(".json\"");
                });
    }

    @Test
    void exportEquipment_whenEmpty_returnsEmptyArray() throws Exception {
        when(exportService.exportAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/equipment/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void importEquipment_whenServiceThrowsValidationError_returns400() throws Exception {
        when(importService.importEquipment(any()))
                .thenThrow(new ImportEquipmentException("Equipment[0]: manufacturer is required"));

        String json = """
                [
                  {
                    "modelNumber": "G15",
                    "status": "Active",
                    "purchaseDate": "2020-12-28"
                  }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile("file", "equipment.json", "application/json", json.getBytes());

        mockMvc.perform(multipart("/api/equipment/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Equipment[0]: manufacturer is required"));
    }
}

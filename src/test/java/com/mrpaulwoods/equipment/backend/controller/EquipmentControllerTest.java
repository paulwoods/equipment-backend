package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.dto.EquipmentTransfer;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
        equipmentController = new EquipmentController(equipmentService, exportService, importService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(equipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private EquipmentResponse sampleResponse() {
        return new EquipmentResponse(EQ_ID, "Acme", "X100", "SN-001", null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2024, 1, 15));
    }

    private EquipmentTransfer sampleEquipmentExport() {
        return new EquipmentTransfer(
                EQ_ID.toString(), "Acme", "X100", "SN-001", null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2024, 1, 15), List.of());
    }

    @Test
    void getAll_returnsPagedEquipmentList() throws Exception {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(sampleResponse()), pageable, 1);
        when(equipmentService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(EQ_ID.toString()))
                .andExpect(jsonPath("$.content[0].manufacturer").value("Acme"));
    }

    @Test
    void getById_whenFound_returnsEquipment() throws Exception {
        when(equipmentService.getById(EQ_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/equipment/{id}", EQ_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EQ_ID.toString()))
                .andExpect(jsonPath("$.modelNumber").value("X100"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        when(equipmentService.getById(missing)).thenThrow(new NotFoundException("Equipment", missing.toString()));

        mockMvc.perform(get("/api/v1/equipment/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Equipment not found: " + missing));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        when(equipmentService.create(any())).thenReturn(sampleResponse());

        String body = """
                {
                  "manufacturer": "Acme",
                  "modelNumber": "X100",
                  "status": "Active",
                  "purchaseDate": "2024-01-15"
                }
                """;

        mockMvc.perform(post("/api/v1/equipment")
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

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void update_withValidBody_returnsUpdatedEquipment() throws Exception {
        var updated = new EquipmentResponse(EQ_ID, "NewCorp", "X100", "SN-001", null, null,
                EquipmentStatus.ACTIVE, null, LocalDate.of(2024, 1, 15));
        when(equipmentService.update(eq(EQ_ID), any())).thenReturn(updated);

        String body = """
                {
                  "manufacturer": "NewCorp",
                  "modelNumber": "X100",
                  "status": "Active",
                  "purchaseDate": "2024-01-15"
                }
                """;

        mockMvc.perform(put("/api/v1/equipment/{id}", EQ_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("NewCorp"));
    }

    @Test
    void delete_whenFound_returns204() throws Exception {
        doNothing().when(equipmentService).delete(EQ_ID);

        mockMvc.perform(delete("/api/v1/equipment/{id}", EQ_ID))
                .andExpect(status().isNoContent());

        verify(equipmentService).delete(EQ_ID);
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(new NotFoundException("Equipment", missing.toString())).when(equipmentService).delete(missing);

        mockMvc.perform(delete("/api/v1/equipment/{id}", missing))
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

        mockMvc.perform(multipart("/api/v1/equipment/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentImported").value(2))
                .andExpect(jsonPath("$.proceduresImported").value(3))
                .andExpect(jsonPath("$.historyImported").value(5));
    }

    @Test
    void exportEquipment_returnsJsonFile() throws Exception {
        when(exportService.exportAll()).thenReturn(List.of(sampleEquipmentExport()));

        mockMvc.perform(get("/api/v1/equipment/export"))
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

        mockMvc.perform(get("/api/v1/equipment/export"))
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

        mockMvc.perform(multipart("/api/v1/equipment/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Import Error"))
                .andExpect(jsonPath("$.detail").value("Equipment[0]: manufacturer is required"));
    }
}

package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.entity.Procedure;
import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
import com.mrpaulwoods.equipment.backend.exception.ProcedureNotFoundException;
import com.mrpaulwoods.equipment.backend.service.ProcedureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProcedureControllerTest {

    @Mock
    private ProcedureService procedureService;

    @InjectMocks
    private ProcedureController procedureController;

    private MockMvc mockMvc;

    private static final UUID EQ_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(procedureController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Procedure sampleProcedure() {
        Procedure p = new Procedure();
        p.setId(PROC_ID);
        p.setName("Oil Change");
        p.setDescription("Change engine oil");
        p.setSteps("1. Drain oil\n2. Replace filter\n3. Fill new oil");
        p.setIntervalDays(90);
        return p;
    }

    @Test
    void getAll_returnsProcedureList() throws Exception {
        when(procedureService.getAllForEquipment(EQ_ID)).thenReturn(List.of(sampleProcedure()));

        mockMvc.perform(get("/api/equipment/{eq}/procedures", EQ_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROC_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Oil Change"));
    }

    @Test
    void getById_whenFound_returnsProcedure() throws Exception {
        when(procedureService.getById(EQ_ID, PROC_ID)).thenReturn(sampleProcedure());

        mockMvc.perform(get("/api/equipment/{eq}/procedures/{proc}", EQ_ID, PROC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROC_ID.toString()))
                .andExpect(jsonPath("$.intervalDays").value(90));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        when(procedureService.getById(EQ_ID, missing))
                .thenThrow(new ProcedureNotFoundException(missing.toString()));

        mockMvc.perform(get("/api/equipment/{eq}/procedures/{proc}", EQ_ID, missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Procedure not found: " + missing));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        when(procedureService.create(eq(EQ_ID), any())).thenReturn(sampleProcedure());

        String body = """
                {
                  "name": "Oil Change",
                  "steps": "1. Drain oil",
                  "intervalDays": 90
                }
                """;

        mockMvc.perform(post("/api/equipment/{eq}/procedures", EQ_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROC_ID.toString()));
    }

    @Test
    void create_withMissingRequiredFields_returns400() throws Exception {
        String body = """
                {
                  "intervalDays": 90
                }
                """;

        mockMvc.perform(post("/api/equipment/{eq}/procedures", EQ_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void update_withValidBody_returnsUpdatedProcedure() throws Exception {
        Procedure updated = sampleProcedure();
        updated.setName("Full Oil Change");
        when(procedureService.update(eq(EQ_ID), eq(PROC_ID), any())).thenReturn(updated);

        String body = """
                {
                  "name": "Full Oil Change",
                  "steps": "1. Drain oil",
                  "intervalDays": 90
                }
                """;

        mockMvc.perform(put("/api/equipment/{eq}/procedures/{proc}", EQ_ID, PROC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Full Oil Change"));
    }

    @Test
    void delete_whenFound_returns204() throws Exception {
        doNothing().when(procedureService).delete(EQ_ID, PROC_ID);

        mockMvc.perform(delete("/api/equipment/{eq}/procedures/{proc}", EQ_ID, PROC_ID))
                .andExpect(status().isNoContent());

        verify(procedureService).delete(EQ_ID, PROC_ID);
    }

    @Test
    void delete_whenNotFound_returns404() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(new ProcedureNotFoundException(missing.toString())).when(procedureService).delete(EQ_ID, missing);

        mockMvc.perform(delete("/api/equipment/{eq}/procedures/{proc}", EQ_ID, missing))
                .andExpect(status().isNotFound());
    }
}

package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.service.PerformHistoryService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerformHistoryControllerTest {

    @Mock
    private PerformHistoryService performHistoryService;

    @InjectMocks
    private PerformHistoryController performHistoryController;

    private MockMvc mockMvc;

    private static final UUID EQ_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PERF_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(performHistoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Perform samplePerform() {
        Perform p = new Perform();
        p.setId(PERF_ID);
        p.setDate(LocalDate.of(2024, 6, 1));
        p.setNotes("Completed without issues");
        return p;
    }

    @Test
    void getHistory_returnsPerformList() throws Exception {
        when(performHistoryService.getHistory(EQ_ID, PROC_ID)).thenReturn(List.of(samplePerform()));

        mockMvc.perform(get("/api/equipment/{eq}/procedures/{proc}/history", EQ_ID, PROC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PERF_ID.toString()))
                .andExpect(jsonPath("$[0].notes").value("Completed without issues"));
    }

    @Test
    void record_withValidBody_returns201() throws Exception {
        when(performHistoryService.record(eq(EQ_ID), eq(PROC_ID), any())).thenReturn(samplePerform());

        String body = """
                {
                  "date": "2024-06-01",
                  "notes": "Completed without issues"
                }
                """;

        mockMvc.perform(post("/api/equipment/{eq}/procedures/{proc}/history", EQ_ID, PROC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PERF_ID.toString()))
                .andExpect(jsonPath("$.notes").value("Completed without issues"));
    }

    @Test
    void record_withMissingDate_returns400() throws Exception {
        String body = """
                {
                  "notes": "No date provided"
                }
                """;

        mockMvc.perform(post("/api/equipment/{eq}/procedures/{proc}/history", EQ_ID, PROC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }
}

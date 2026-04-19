package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    void getDashboard_returnsDashboardItems() throws Exception {
        DashboardItem item = new DashboardItem(
                "eq-1", "Pump A", "proc-1", "Oil Change",
                "Change oil", 90, 5, "2024-06-15", "DUE_SOON"
        );
        when(dashboardService.getDashboardItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentId").value("eq-1"))
                .andExpect(jsonPath("$[0].procedureName").value("Oil Change"))
                .andExpect(jsonPath("$[0].daysTillDue").value(5))
                .andExpect(jsonPath("$[0].status").value("DUE_SOON"));
    }

    @Test
    void getDashboard_whenEmpty_returnsEmptyList() throws Exception {
        when(dashboardService.getDashboardItems()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}

package com.mrpaulwoods.equipment.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void equipmentNotFound_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/equipment/eq-99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Equipment not found: eq-99"));
    }

    @Test
    void procedureNotFound_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/procedure/proc-5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Procedure not found: proc-5"));
    }

    @Test
    void validation_blankAndNull_returns400WithFieldErrors() throws Exception {
        String body = """
                { "name": "", "count": null }
                """;

        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.count").exists());
    }

    @Test
    void validation_missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    // Minimal controller that throws the exceptions under test
    @RestController
    static class StubController {

        @GetMapping("/equipment/{id}")
        public void throwEquipmentNotFound(@PathVariable String id) {
            throw new EquipmentNotFoundException(id);
        }

        @GetMapping("/procedure/{id}")
        public void throwProcedureNotFound(@PathVariable String id) {
            throw new ProcedureNotFoundException(id);
        }

        @PostMapping("/validated")
        public void validated(@RequestBody @jakarta.validation.Valid ValidatedBody body) {
        }

        record ValidatedBody(
                @jakarta.validation.constraints.NotBlank String name,
                @jakarta.validation.constraints.NotNull Integer count
        ) {
        }
    }
}

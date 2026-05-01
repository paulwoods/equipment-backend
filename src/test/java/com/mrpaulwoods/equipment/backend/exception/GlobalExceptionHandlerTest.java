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
    void equipmentNotFound_returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/equipment/eq-99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Equipment not found: eq-99"));
    }

    @Test
    void procedureNotFound_returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/procedure/proc-5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Procedure not found: proc-5"));
    }

    @Test
    void validation_blankAndNull_returns400WithProblemDetail() throws Exception {
        String body = """
                { "name": "", "count": null }
                """;

        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.count").exists());
    }

    @Test
    void validation_missingFields_returns400WithProblemDetail() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").exists());
    }

    // Minimal controller that throws the exceptions under test
    @RestController
    static class StubController {

        @GetMapping("/equipment/{id}")
        public void throwEquipmentNotFound(@PathVariable String id) {
            throw new NotFoundException("Equipment", id);
        }

        @GetMapping("/procedure/{id}")
        public void throwProcedureNotFound(@PathVariable String id) {
            throw new NotFoundException("Procedure", id);
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

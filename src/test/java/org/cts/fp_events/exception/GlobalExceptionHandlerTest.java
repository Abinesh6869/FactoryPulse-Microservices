package org.cts.fp_events.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    @RestController
    @RequestMapping("/test-exception")
    static class TestController {
        @GetMapping("/not-found")
        String notFound() { throw new ResourceNotFoundException("Item not found"); }

        @GetMapping("/bad-request")
        String badRequest() { throw new BadRequestException("Invalid input"); }

        @GetMapping("/unauthorized")
        String unauthorized() { throw new UnauthorizedException("Access denied"); }

        @GetMapping("/generic")
        String generic() { throw new RuntimeException("Unexpected error"); }

        @PostMapping("/validation")
        String validation(@RequestBody @jakarta.validation.Valid ValidBody body) { return "ok"; }
    }

    static class ValidBody {
        @jakarta.validation.constraints.NotBlank(message = "name is required")
        public String name;
    }

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleResourceNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test-exception/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Item not found"));
    }

    @Test
    void handleBadRequest_returns400() throws Exception {
        mockMvc.perform(get("/test-exception/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid input"));
    }

    @Test
    void handleUnauthorized_returns401() throws Exception {
        mockMvc.perform(get("/test-exception/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void handleGenericException_returns500() throws Exception {
        mockMvc.perform(get("/test-exception/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void handleValidationErrors_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test-exception/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("name is required"));
    }
}

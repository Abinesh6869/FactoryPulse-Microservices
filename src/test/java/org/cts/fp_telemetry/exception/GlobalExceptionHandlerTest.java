package org.cts.fp_telemetry.exception;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GlobalExceptionHandlerTest {

    private static GlobalExceptionHandler handler;

    @BeforeAll
    static void init() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @Order(1)
    void resourceNotFound_returns404() {
        ResourceNotFoundException thrown = assertThrows(
                ResourceNotFoundException.class,
                () -> { throw new ResourceNotFoundException("TelemetryEvent not found: 1"); });

        var response = handler.handleResourceNotFound(thrown);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(2)
    void resourceNotFound_messageIsCorrect() {
        ResourceNotFoundException thrown = assertThrows(
                ResourceNotFoundException.class,
                () -> { throw new ResourceNotFoundException("TelemetryEvent not found: 1"); });

        var response = handler.handleResourceNotFound(thrown);

        assertEquals("TelemetryEvent not found: 1", response.getBody().getMessage());
    }

    @Test
    @Order(3)
    void badRequest_returns400() {
        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> { throw new BadRequestException("Machine is INACTIVE"); });

        var response = handler.handleBadRequest(thrown);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(4)
    void badRequest_messageIsCorrect() {
        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> { throw new BadRequestException("Machine is INACTIVE"); });

        var response = handler.handleBadRequest(thrown);

        assertEquals("Machine is INACTIVE", response.getBody().getMessage());
    }

    @Test
    @Order(5)
    void unauthorized_returns401() {
        UnauthorizedException thrown = assertThrows(
                UnauthorizedException.class,
                () -> { throw new UnauthorizedException("Access denied"); });

        var response = handler.handleUnauthorized(thrown);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(6)
    void unauthorized_messageIsCorrect() {
        UnauthorizedException thrown = assertThrows(
                UnauthorizedException.class,
                () -> { throw new UnauthorizedException("Access denied"); });

        var response = handler.handleUnauthorized(thrown);

        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    @Order(7)
    void genericException_returns500() {
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> { throw new RuntimeException("Something went wrong"); });

        var response = handler.handleGenericException(thrown);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @Order(8)
    void genericException_messageIsCorrect() {
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> { throw new RuntimeException("Something went wrong"); });

        var response = handler.handleGenericException(thrown);

        assertTrue(response.getBody().getMessage().contains("Something went wrong"));
    }

    @Test
    @Order(9)
    void validationError_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(
                List.of(new FieldError("req", "pointId", "pointId is required")));

        var response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(10)
    void validationError_fieldMessageIsCorrect() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(
                List.of(new FieldError("req", "pointId", "pointId is required")));

        var response = handler.handleValidationErrors(ex);

        assertEquals("pointId is required", response.getBody().getData().get("pointId"));
    }
}
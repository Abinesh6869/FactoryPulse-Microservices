package org.cts.fp_telemetry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cts.fp_telemetry.dto.request.ProductionCountRequest;
import org.cts.fp_telemetry.dto.request.TelemetryEventRequest;
import org.cts.fp_telemetry.dto.response.ProductionCountResponse;
import org.cts.fp_telemetry.dto.response.TelemetryEventResponse;
import org.cts.fp_telemetry.exception.ResourceNotFoundException;
import org.cts.fp_telemetry.security.JwtUtil;
import org.cts.fp_telemetry.service.TelemetryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Order(4)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TelemetryService telemetryService;
    @MockitoBean private JwtUtil jwtUtil;

    private static TelemetryEventResponse eventResponse;
    private static ProductionCountResponse countResponse;

    @BeforeAll
    static void init() {
        eventResponse = TelemetryEventResponse.builder()
                .eventId(1L).machineId(5L).machineName("Machine A")
                .pointId(10L).pointName("Temp Sensor")
                .value("75.5").source("SENSOR").status("OK")
                .timestamp(LocalDateTime.now()).build();

        countResponse = ProductionCountResponse.builder()
                .countId(1L).lineId(2L).lineName("Line 1")
                .shiftId(3L).shiftName("Morning Shift")
                .goodCount(100).rejectCount(5).totalCount(105)
                .timestamp(LocalDateTime.now()).build();
    }

    // GET /api/telemetry

    @Test
    @Order(1)
    @WithMockUser(roles = "OPERATOR")
    void getTelemetry_byMachineId_statusIsOk() throws Exception {
        when(telemetryService.getEventsByMachine(eq(5L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventResponse)));

        mockMvc.perform(get("/api/telemetry").param("machineId", "5"))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).getEventsByMachine(eq(5L), any(), any(), any(Pageable.class));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "OPERATOR")
    void getTelemetry_byMachineId_messageIsCorrect() throws Exception {
        when(telemetryService.getEventsByMachine(eq(5L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventResponse)));

        mockMvc.perform(get("/api/telemetry").param("machineId", "5"))
                .andExpect(jsonPath("$.message").value("Events fetched successfully"));

        verify(telemetryService, times(1)).getEventsByMachine(eq(5L), any(), any(), any(Pageable.class));
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    void getTelemetry_byMachineIdWithLatest_callsLatestService() throws Exception {
        when(telemetryService.getLatestEventsByMachine(5L))
                .thenReturn(List.of(eventResponse));

        mockMvc.perform(get("/api/telemetry").param("machineId", "5").param("latest", "true"))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).getLatestEventsByMachine(5L);
        verify(telemetryService, never()).getEventsByMachine(any(), any(), any(), any());
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "TECHNICIAN")
    void getTelemetry_byPointId_statusIsOk() throws Exception {
        when(telemetryService.getEventsByPoint(eq(10L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventResponse)));

        mockMvc.perform(get("/api/telemetry").param("pointId", "10"))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).getEventsByPoint(eq(10L), any(), any(), any(Pageable.class));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "MANAGER")
    void getTelemetry_noParams_neverCallsService() throws Exception {
        mockMvc.perform(get("/api/telemetry"))
                .andExpect(jsonPath("$.message").value("Please provide machineId or pointId"));

        verify(telemetryService, never()).getEventsByMachine(any(), any(), any(), any());
        verify(telemetryService, never()).getEventsByPoint(any(), any(), any(), any());
    }

    @Test
    @Order(6)
    void getTelemetry_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/telemetry").param("machineId", "5"))
                .andExpect(status().is4xxClientError());

        verify(telemetryService, never()).getEventsByMachine(any(), any(), any(), any());
    }

    //  GET /api/telemetry/production

    @ParameterizedTest
    @MethodSource("data")
    @Order(7)
    @WithMockUser(roles = "SUPERVISOR")
    void getProduction_byLineOrShift_statusIsOk(String param, String value) throws Exception {
        when(telemetryService.getProductionCountsByLine(eq(2L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(countResponse)));
        when(telemetryService.getProductionCountsByShift(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(countResponse)));

        mockMvc.perform(get("/api/telemetry/production").param(param, value))
                .andExpect(status().isOk());
    }

    public static Stream<org.junit.jupiter.params.provider.Arguments> data() {
        return Stream.of(
                of("lineId",  "2"),
                of("shiftId", "3")
        );
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "ADMIN")
    void getProduction_noParams_neverCallsService() throws Exception {
        mockMvc.perform(get("/api/telemetry/production"))
                .andExpect(jsonPath("$.message").value("Please provide lineId or shiftId"));

        verify(telemetryService, never()).getProductionCountsByLine(any(), any(), any(), any());
        verify(telemetryService, never()).getProductionCountsByShift(any(), any());
    }

    // GET /api/telemetry/production/{countId}

    @Test
    @Order(9)
    @WithMockUser(roles = "ADMIN")
    void getProductionCountById_found_statusIsOk() throws Exception {
        when(telemetryService.getProductionCountById(1L)).thenReturn(countResponse);

        mockMvc.perform(get("/api/telemetry/production/1"))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).getProductionCountById(1L);
    }

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    void getProductionCountById_found_totalCountIsCorrect() throws Exception {
        when(telemetryService.getProductionCountById(1L)).thenReturn(countResponse);

        mockMvc.perform(get("/api/telemetry/production/1"))
                .andExpect(jsonPath("$.data.totalCount").value(105));

        verify(telemetryService, times(1)).getProductionCountById(1L);
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "MANAGER")
    void getProductionCountById_notFound_returns404() throws Exception {
        when(telemetryService.getProductionCountById(99L))
                .thenThrow(new ResourceNotFoundException("ProductionCount not found with id: 99"));

        mockMvc.perform(get("/api/telemetry/production/99"))
                .andExpect(status().isNotFound());

        verify(telemetryService, times(1)).getProductionCountById(99L);
    }

    // PATCH /api/telemetry/production/updateCount/{countId}

    @Test
    @Order(12)
    @WithMockUser(roles = "ADMIN")
    void updateProductionCount_statusIsOk() throws Exception {
        when(telemetryService.updateProductionCount(1L, 120, 10)).thenReturn(countResponse);

        mockMvc.perform(patch("/api/telemetry/production/updateCount/1")
                        .param("goodCount", "120").param("rejectCount", "10")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(telemetryService, times(1)).updateProductionCount(1L, 120, 10);
    }

    @Test
    @Order(13)
    @WithMockUser(roles = "ADMIN")
    void updateProductionCount_messageIsCorrect() throws Exception {
        when(telemetryService.updateProductionCount(1L, 120, 10)).thenReturn(countResponse);

        mockMvc.perform(patch("/api/telemetry/production/updateCount/1")
                        .param("goodCount", "120").param("rejectCount", "10")
                        .with(csrf()))
                .andExpect(jsonPath("$.message").value("Production count updated successfully"));

        verify(telemetryService, times(1)).updateProductionCount(1L, 120, 10);
    }

    // POST /api/telemetry

    @Test
    @Order(14)
    @WithMockUser(roles = "OPERATOR")
    void createEvent_validRequest_returns201() throws Exception {
        when(telemetryService.createEvent(any())).thenReturn(eventResponse);

        TelemetryEventRequest req = new TelemetryEventRequest();
        req.setPointId(10L); req.setPointName("Temp Sensor");
        req.setMachineId(5L); req.setMachineName("Machine A");
        req.setValue("75.5");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(telemetryService, times(1)).createEvent(any());
    }

    @Test
    @Order(15)
    @WithMockUser(roles = "OPERATOR")
    void createEvent_missingFields_returns400_neverCallsService() throws Exception {
        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TelemetryEventRequest()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(telemetryService, never()).createEvent(any());
    }

    // POST /api/telemetry/production

    @Test
    @Order(16)
    @WithMockUser(roles = "SUPERVISOR")
    void createProductionCount_validRequest_returns201() throws Exception {
        when(telemetryService.createProductionCount(any())).thenReturn(countResponse);

        ProductionCountRequest req = new ProductionCountRequest();
        req.setLineId(2L); req.setLineName("Line 1");
        req.setGoodCount(100); req.setRejectCount(5);

        mockMvc.perform(post("/api/telemetry/production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(telemetryService, times(1)).createProductionCount(any());
    }

    @Test
    @Order(17)
    @WithMockUser(roles = "SUPERVISOR")
    void createProductionCount_negativeCount_returns400_neverCallsService() throws Exception {
        ProductionCountRequest req = new ProductionCountRequest();
        req.setLineId(2L); req.setLineName("Line 1");
        req.setGoodCount(-1); req.setRejectCount(5);

        mockMvc.perform(post("/api/telemetry/production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(telemetryService, never()).createProductionCount(any());
    }
}
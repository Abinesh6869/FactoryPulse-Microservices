package org.cts.fp_events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cts.fp_events.dto.request.CorrectiveActionRequest;
import org.cts.fp_events.dto.request.DowntimeEventRequest;
import org.cts.fp_events.dto.response.CorrectiveActionResponse;
import org.cts.fp_events.dto.response.DowntimeEventResponse;
import org.cts.fp_events.exception.BadRequestException;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.security.JwtUtil;
import org.cts.fp_events.service.DowntimeEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DowntimeEventController.class)
class DowntimeEventControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DowntimeEventService downtimeService;
    @MockitoBean JwtUtil jwtUtil;

    private static final String TOKEN = "Bearer mock-token";

    @BeforeEach
    void setUpJwtMock() {
        when(jwtUtil.validateToken("mock-token")).thenReturn(true);
        when(jwtUtil.extractEmail("mock-token")).thenReturn("operator@test.com");
        when(jwtUtil.extractRole("mock-token")).thenReturn("OPERATOR");
        when(jwtUtil.extractUserId("mock-token")).thenReturn(2L);
        when(jwtUtil.extractUserName("mock-token")).thenReturn("Operator User");
        when(jwtUtil.extractEmployeeId("mock-token")).thenReturn("EMP002");
    }

    // ── POST /api/downtimes ───────────────────────────────────────────────────

    @Test
    void createDowntime_validRequest_returns201() throws Exception {
        DowntimeEventRequest req = downtimeRequest();
        DowntimeEventResponse resp = downtimeResponse(1L);
        when(downtimeService.createDowntime(any(), eq(2L), anyString(), anyString()))
                .thenReturn(resp);

        mockMvc.perform(post("/api/downtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Downtime created successfully"))
                .andExpect(jsonPath("$.data.downtimeId").value(1));
    }

    @Test
    void createDowntime_machineAlreadyDown_returns400() throws Exception {
        when(downtimeService.createDowntime(any(), anyLong(), anyString(), anyString()))
                .thenThrow(new BadRequestException("Machine already has an active downtime."));

        mockMvc.perform(post("/api/downtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(downtimeRequest()))
                        .header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/downtimes ────────────────────────────────────────────────────

    @Test
    void getDowntimes_noParams_returnsPage() throws Exception {
        when(downtimeService.getAllDowntimes(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(downtimeResponse(1L))));

        mockMvc.perform(get("/api/downtimes").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Downtimes fetched successfully"));
    }

    @Test
    void getDowntimes_byId_returnsSingleDowntime() throws Exception {
        when(downtimeService.getDowntimeById(1L)).thenReturn(downtimeResponse(1L));

        mockMvc.perform(get("/api/downtimes")
                        .param("id", "1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downtimeId").value(1));
    }

    @Test
    void getDowntimes_activeTrue_returnsActiveDowntimes() throws Exception {
        when(downtimeService.getActiveDowntimes()).thenReturn(List.of(downtimeResponse(1L)));

        mockMvc.perform(get("/api/downtimes")
                        .param("active", "true")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Active downtimes fetched successfully"));
    }

    @Test
    void getDowntimes_notFound_returns404() throws Exception {
        when(downtimeService.getDowntimeById(99L))
                .thenThrow(new ResourceNotFoundException("Downtime not found with id: 99"));

        mockMvc.perform(get("/api/downtimes")
                        .param("id", "99")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/downtimes/{id}/close ───────────────────────────────────────

    @Test
    void closeDowntime_returns200() throws Exception {
        DowntimeEventResponse closed = downtimeResponse(1L);
        closed.setEndAt(LocalDateTime.now());
        when(downtimeService.closeDowntime(eq(1L), any())).thenReturn(closed);

        mockMvc.perform(patch("/api/downtimes/1/close")
                        .param("endAt", "2026-05-01T10:00:00")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Downtime closed successfully"));
    }

    @Test
    void closeDowntime_alreadyClosed_returns400() throws Exception {
        when(downtimeService.closeDowntime(eq(1L), any()))
                .thenThrow(new BadRequestException("Downtime ID 1 is already closed."));

        mockMvc.perform(patch("/api/downtimes/1/close")
                        .param("endAt", "2026-05-01T10:00:00")
                        .header("Authorization", TOKEN))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/downtimes/{id}/rootcause ───────────────────────────────────

    @Test
    void tagRootCause_returns200() throws Exception {
        DowntimeEventResponse tagged = downtimeResponse(1L);
        tagged.setRootCauseId(5L);
        when(downtimeService.tagRootCause(eq(1L), eq(5L), anyString(), anyString()))
                .thenReturn(tagged);

        mockMvc.perform(patch("/api/downtimes/1/rootcause")
                        .param("rootCauseId", "5")
                        .param("rootCauseCode", "RC-005")
                        .param("rootCauseDescription", "Electrical fault")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Root cause tagged successfully"));
    }

    // ── POST /api/downtimes/actions ───────────────────────────────────────────

    @Test
    void createCorrectiveAction_validRequest_returns201() throws Exception {
        CorrectiveActionRequest req = actionRequest();
        CorrectiveActionResponse resp = actionResponse(1L);
        when(downtimeService.createCorrectiveAction(any())).thenReturn(resp);

        mockMvc.perform(post("/api/downtimes/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Corrective action created successfully"))
                .andExpect(jsonPath("$.data.actionId").value(1));
    }

    @Test
    void createCorrectiveAction_openDowntime_returns400() throws Exception {
        when(downtimeService.createCorrectiveAction(any()))
                .thenThrow(new BadRequestException("Cannot create corrective action for an open downtime."));

        mockMvc.perform(post("/api/downtimes/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionRequest()))
                        .header("Authorization", TOKEN))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/downtimes/{id}/actions ───────────────────────────────────────

    @Test
    void getActionsByDowntime_returns200() throws Exception {
        when(downtimeService.getActionsByDowntime(1L)).thenReturn(List.of(actionResponse(1L)));

        mockMvc.perform(get("/api/downtimes/1/actions").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].actionId").value(1));
    }

    // ── GET /api/downtimes/my-actions ─────────────────────────────────────────

    @Test
    void getMyActions_returns200WithCurrentUserActions() throws Exception {
        when(downtimeService.getMyActions(2L)).thenReturn(List.of(actionResponse(1L)));

        mockMvc.perform(get("/api/downtimes/my-actions").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My corrective actions fetched successfully"));
    }

    // ── PATCH /api/downtimes/actions/{id}/complete ────────────────────────────

    @Test
    void completeAction_returns200() throws Exception {
        CorrectiveActionResponse completed = actionResponse(1L);
        completed.setStatus("COMPLETED");
        when(downtimeService.completeAction(1L)).thenReturn(completed);

        mockMvc.perform(patch("/api/downtimes/actions/1/complete").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ── No token → 403 ───────────────────────────────────────────────────────

    @Test
    void getDowntimes_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/downtimes"))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DowntimeEventRequest downtimeRequest() {
        DowntimeEventRequest req = new DowntimeEventRequest();
        req.setLineId(1L);
        req.setMachineId(1L);
        req.setLineName("Line A");
        req.setMachineName("Machine 1");
        req.setStartAt(LocalDateTime.now().minusHours(1));
        req.setCategory("MECHANICAL");
        return req;
    }

    private DowntimeEventResponse downtimeResponse(Long id) {
        return DowntimeEventResponse.builder()
                .downtimeId(id)
                .lineId(1L)
                .lineName("Line A")
                .machineId(1L)
                .machineName("Machine 1")
                .startAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private CorrectiveActionRequest actionRequest() {
        CorrectiveActionRequest req = new CorrectiveActionRequest();
        req.setDowntimeId(1L);
        req.setAssignedTo(5L);
        req.setAssignedToName("Tech User");
        req.setAssignedToEmployeeId("EMP005");
        req.setDescription("Fix the wiring");
        req.setDueDate(LocalDate.now().plusDays(3));
        return req;
    }

    private CorrectiveActionResponse actionResponse(Long id) {
        return CorrectiveActionResponse.builder()
                .actionId(id)
                .downtimeId(1L)
                .description("Fix the wiring")
                .status("OPEN")
                .build();
    }
}

package org.cts.fp_events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cts.fp_events.dto.request.InternalNotificationRequest;
import org.cts.fp_events.dto.response.AlertResponse;
import org.cts.fp_events.dto.response.NotificationResponse;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.security.JwtUtil;
import org.cts.fp_events.security.SecurityConfig;
import org.cts.fp_events.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@Import(SecurityConfig.class)
class AlertControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AlertService alertService;
    @MockitoBean JwtUtil jwtUtil;

    private static final String TOKEN = "Bearer mock-token";

    @BeforeEach
    void setUpJwtMock() {
        when(jwtUtil.validateToken("mock-token")).thenReturn(true);
        when(jwtUtil.extractEmail("mock-token")).thenReturn("admin@test.com");
        when(jwtUtil.extractRole("mock-token")).thenReturn("ADMIN");
        when(jwtUtil.extractUserId("mock-token")).thenReturn(1L);
        when(jwtUtil.extractUserName("mock-token")).thenReturn("Admin User");
        when(jwtUtil.extractEmployeeId("mock-token")).thenReturn("EMP001");
    }

    // ── GET /api/alerts ───────────────────────────────────────────────────────

    @Test
    void getAlerts_noParams_returnsPagedAlerts() throws Exception {
        when(alertService.getAllAlerts(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/alerts").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alerts fetched successfully"));
    }

    @Test
    void getAlerts_openTrue_returnsOpenAlerts() throws Exception {
        AlertResponse alert = alertResponse(1L, "OPEN");
        when(alertService.getOpenAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts")
                        .param("open", "true")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Open alerts fetched successfully"))
                .andExpect(jsonPath("$.data[0].alertId").value(1));
    }

    @Test
    void getAlerts_byRuleId_returnsFilteredAlerts() throws Exception {
        AlertResponse alert = alertResponse(2L, "OPEN");
        when(alertService.getAllByRule(10L)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts")
                        .param("ruleId", "10")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].alertId").value(2));
    }

    // ── PATCH /api/alerts/{id}/resolve ────────────────────────────────────────

    @Test
    void resolveAlert_returns200() throws Exception {
        AlertResponse resolved = alertResponse(1L, "RESOLVED");
        when(alertService.resolveAlert(1L)).thenReturn(resolved);

        mockMvc.perform(patch("/api/alerts/1/resolve").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Alert resolved successfully"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void resolveAlert_notFound_returns404() throws Exception {
        when(alertService.resolveAlert(99L))
                .thenThrow(new ResourceNotFoundException("Alert not found with id: 99"));

        mockMvc.perform(patch("/api/alerts/99/resolve").header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/alerts/notifications/me ─────────────────────────────────────

    @Test
    void getNotificationsByUser_returns200() throws Exception {
        when(alertService.getNotificationsByUser(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(notificationResponse(1L))));

        mockMvc.perform(get("/api/alerts/notifications/me").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications fetched successfully"));
    }

    // ── GET /api/alerts/notifications/me/unread ───────────────────────────────

    @Test
    void getUnreadNotifications_returns200() throws Exception {
        when(alertService.getUnreadNotificationsByUser(1L))
                .thenReturn(List.of(notificationResponse(1L)));

        mockMvc.perform(get("/api/alerts/notifications/me/unread").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notificationId").value(1));
    }

    // ── PATCH /api/alerts/notifications/{id}/read ─────────────────────────────

    @Test
    void markNotificationRead_returns200() throws Exception {
        NotificationResponse resp = notificationResponse(5L);
        resp.setStatus("READ");
        when(alertService.markNotificationRead(5L)).thenReturn(resp);

        mockMvc.perform(patch("/api/alerts/notifications/5/read").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"))
                .andExpect(jsonPath("$.data.status").value("READ"));
    }

    // ── POST /api/alerts/notifications/internal ───────────────────────────────

    @Test
    void createInternalNotification_returns200() throws Exception {
        InternalNotificationRequest req = new InternalNotificationRequest();
        req.setUserId(5L);
        req.setEmployeeId("EMP005");
        req.setUserName("Tech");
        req.setMessage("Work order ready");

        doNothing().when(alertService)
                .createInternalNotification(anyLong(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/alerts/notifications/internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification created"));
    }

    // ── No token → 403 / 401 ─────────────────────────────────────────────────

    @Test
    void getAlerts_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AlertResponse alertResponse(Long id, String status) {
        return AlertResponse.builder()
                .alertId(id)
                .ruleId(10L)
                .ruleName("TEST-RULE")
                .severity("HIGH")
                .status(status)
                .triggeredAt(LocalDateTime.now())
                .build();
    }

    private NotificationResponse notificationResponse(Long id) {
        return NotificationResponse.builder()
                .notificationId(id)
                .userId(1L)
                .message("Test notification")
                .status("SENT")
                .channel("IN_APP")
                .sentAt(LocalDateTime.now())
                .build();
    }
}

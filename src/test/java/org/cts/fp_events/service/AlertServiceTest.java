package org.cts.fp_events.service;

import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.response.AlertResponse;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.NotificationResponse;
import org.cts.fp_events.dto.response.UserInfo;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.model.Alert;
import org.cts.fp_events.model.Notification;
import org.cts.fp_events.repository.AlertRepository;
import org.cts.fp_events.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock AlertRepository alertRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock IdentityClient identityClient;

    @InjectMocks AlertService alertService;

    // ── triggerAlert ──────────────────────────────────────────────────────────

    @Test
    void triggerAlert_savesAndReturnsAlert() {
        Alert saved = alertWith(1L, "RULE-1", "HIGH", "OPEN");
        when(alertRepository.save(any())).thenReturn(saved);

        Alert result = alertService.triggerAlert(10L, "RULE-1", "HIGH", 5L, "MACHINE", "notes");

        assertThat(result.getAlertId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        verify(alertRepository).save(any());
    }

    @Test
    void triggerAlert_setsCorrectFields() {
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = alertService.triggerAlert(10L, "TEMP_HIGH", "CRITICAL", 99L, "SENSOR", "too hot");

        assertThat(result.getRuleId()).isEqualTo(10L);
        assertThat(result.getRuleName()).isEqualTo("TEMP_HIGH");
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getRelatedEntityId()).isEqualTo(99L);
        assertThat(result.getEntityType()).isEqualTo("SENSOR");
        assertThat(result.getNotes()).isEqualTo("too hot");
        assertThat(result.getStatus()).isEqualTo("OPEN");
    }

    // ── resolveAlert ──────────────────────────────────────────────────────────

    @Test
    void resolveAlert_setsResolvedStatus() {
        Alert alert = alertWith(1L, "RULE-1", "HIGH", "OPEN");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertResponse result = alertService.resolveAlert(1L);

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void resolveAlert_notFound_throwsResourceNotFoundException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.resolveAlert(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getAllAlerts ───────────────────────────────────────────────────────────

    @Test
    void getAllAlerts_returnsPage() {
        Alert alert = alertWith(1L, "RULE-1", "HIGH", "OPEN");
        Page<Alert> page = new PageImpl<>(List.of(alert));
        when(alertRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<AlertResponse> result = alertService.getAllAlerts(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAlertId()).isEqualTo(1L);
    }

    // ── getAllByRule ───────────────────────────────────────────────────────────

    @Test
    void getAllByRule_returnsAlertsForGivenRuleId() {
        Alert a1 = alertWith(1L, "RULE-X", "LOW", "OPEN");
        Alert a2 = alertWith(2L, "RULE-X", "LOW", "RESOLVED");
        when(alertRepository.findByRuleId(10L)).thenReturn(List.of(a1, a2));

        List<AlertResponse> result = alertService.getAllByRule(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRuleName()).isEqualTo("RULE-X");
    }

    // ── getOpenAlerts ─────────────────────────────────────────────────────────

    @Test
    void getOpenAlerts_returnsOnlyOpenAlerts() {
        Alert open = alertWith(1L, "RULE-1", "HIGH", "OPEN");
        when(alertRepository.findByStatus("OPEN")).thenReturn(List.of(open));

        List<AlertResponse> result = alertService.getOpenAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("OPEN");
    }

    // ── getNotificationsByUser ────────────────────────────────────────────────

    @Test
    void getNotificationsByUser_userExists_returnsPage() {
        IdentityApiResponse<UserInfo> userResp = new IdentityApiResponse<>();
        UserInfo user = new UserInfo();
        userResp.setData(user);
        when(identityClient.getUserById(1L)).thenReturn(userResp);

        Notification n = notificationWith(1L, 1L, "SENT");
        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notificationRepository.findByUserId(eq(1L), any())).thenReturn(page);

        Page<NotificationResponse> result = alertService.getNotificationsByUser(1L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getNotificationsByUser_userNotFound_throwsResourceNotFoundException() {
        IdentityApiResponse<UserInfo> emptyResp = new IdentityApiResponse<>();
        emptyResp.setData(null);
        when(identityClient.getUserById(99L)).thenReturn(emptyResp);

        assertThatThrownBy(() -> alertService.getNotificationsByUser(99L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getUnreadNotificationsByUser ──────────────────────────────────────────

    @Test
    void getUnreadNotificationsByUser_returnsUnreadNotifications() {
        IdentityApiResponse<UserInfo> userResp = new IdentityApiResponse<>();
        userResp.setData(new UserInfo());
        when(identityClient.getUserById(1L)).thenReturn(userResp);

        Notification n = notificationWith(1L, 1L, "SENT");
        when(notificationRepository.findByUserIdAndStatus(1L, "SENT")).thenReturn(List.of(n));

        List<NotificationResponse> result = alertService.getUnreadNotificationsByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("SENT");
    }

    // ── createInternalNotification ────────────────────────────────────────────

    @Test
    void createInternalNotification_savesNotification() {
        alertService.createInternalNotification(5L, "EMP005", "Jane", "Work order assigned");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(5L);
        assertThat(saved.getEmployeeId()).isEqualTo("EMP005");
        assertThat(saved.getUserName()).isEqualTo("Jane");
        assertThat(saved.getMessage()).isEqualTo("Work order assigned");
        assertThat(saved.getStatus()).isEqualTo("SENT");
        assertThat(saved.getChannel()).isEqualTo("IN_APP");
    }

    // ── markNotificationRead ──────────────────────────────────────────────────

    @Test
    void markNotificationRead_setsReadStatus() {
        Notification n = notificationWith(1L, 1L, "SENT");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse result = alertService.markNotificationRead(1L);

        assertThat(result.getStatus()).isEqualTo("READ");
    }

    @Test
    void markNotificationRead_notFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.markNotificationRead(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── sendNotification ──────────────────────────────────────────────────────

    @Test
    void sendNotification_savesNotificationWithCorrectFields() {
        Alert alert = alertWith(10L, "RULE-1", "HIGH", "OPEN");

        alertService.sendNotification(alert, 5L, "EMP005", "Bob", "Alert triggered");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getAlertId()).isEqualTo(10L);
        assertThat(saved.getUserId()).isEqualTo(5L);
        assertThat(saved.getMessage()).isEqualTo("Alert triggered");
        assertThat(saved.getStatus()).isEqualTo("SENT");
        assertThat(saved.getChannel()).isEqualTo("IN_APP");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Alert alertWith(Long id, String ruleName, String severity, String status) {
        Alert a = new Alert();
        a.setAlertId(id);
        a.setRuleId(10L);
        a.setRuleName(ruleName);
        a.setSeverity(severity);
        a.setStatus(status);
        a.setTriggeredAt(LocalDateTime.now());
        return a;
    }

    private Notification notificationWith(Long id, Long userId, String status) {
        Notification n = new Notification();
        n.setNotificationId(id);
        n.setUserId(userId);
        n.setStatus(status);
        n.setChannel("IN_APP");
        n.setSentAt(LocalDateTime.now());
        return n;
    }
}

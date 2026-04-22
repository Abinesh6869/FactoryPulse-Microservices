package org.cts.fp_events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.response.AlertResponse;
import org.cts.fp_events.dto.response.NotificationResponse;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.model.Alert;
import org.cts.fp_events.model.Notification;
import org.cts.fp_events.repository.AlertRepository;
import org.cts.fp_events.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final IdentityClient identityClient;

    public AlertResponse resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));
        alert.setResolvedAt(LocalDateTime.now());
        alert.setStatus("RESOLVED");
        Alert saved = alertRepository.save(alert);
        try { identityClient.recordAuditLog(new AuditLogRequest("RESOLVE_ALERT", "Alert", "Resolved alert ID: " + alertId)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toAlertResponse(saved);
    }

    public Page<AlertResponse> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable).map(this::toAlertResponse);
    }

    public List<AlertResponse> getAllByRule(Long ruleId) {
        return alertRepository.findByRuleId(ruleId).stream()
                .map(this::toAlertResponse).collect(Collectors.toList());
    }

    public List<AlertResponse> getOpenAlerts() {
        return alertRepository.findByStatus("OPEN").stream()
                .map(this::toAlertResponse).collect(Collectors.toList());
    }

    public Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(this::toNotificationResponse);
    }

    public List<NotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, "SENT")
                .stream().map(this::toNotificationResponse).collect(Collectors.toList());
    }

    public NotificationResponse markNotificationRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        n.setStatus("READ");
        return toNotificationResponse(notificationRepository.save(n));
    }

    private AlertResponse toAlertResponse(Alert a) {
        return AlertResponse.builder()
                .alertId(a.getAlertId())
                .ruleId(a.getRuleId())
                .ruleName(a.getRuleName())
                .severity(a.getSeverity())
                .relatedEntityId(a.getRelatedEntityId())
                .entityType(a.getEntityType())
                .triggeredAt(a.getTriggeredAt())
                .resolvedAt(a.getResolvedAt())
                .status(a.getStatus())
                .notes(a.getNotes())
                .build();
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .employeeId(n.getEmployeeId())
                .userName(n.getUserName())
                .alertId(n.getAlertId())
                .channel(n.getChannel())
                .message(n.getMessage())
                .sentAt(n.getSentAt())
                .status(n.getStatus())
                .build();
    }
}

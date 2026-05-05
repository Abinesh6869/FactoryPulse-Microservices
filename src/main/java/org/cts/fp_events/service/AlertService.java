package org.cts.fp_events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.response.AlertResponse;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.NotificationResponse;
import org.cts.fp_events.dto.response.UserInfo;
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

    // ── Used by AlertWorker ─────────────────────────────────────────────────
    public Alert triggerAlert(Long ruleId, String ruleName, String severity,
                              Long relatedEntityId, String entityType, String notes) {
        Alert alert = new Alert();
        alert.setRuleId(ruleId);
        alert.setRuleName(ruleName);
        alert.setSeverity(severity);
        alert.setRelatedEntityId(relatedEntityId);
        alert.setEntityType(entityType);
        alert.setStatus("OPEN");
        alert.setTriggeredAt(java.time.LocalDateTime.now());
        alert.setNotes(notes);
        Alert saved = alertRepository.save(alert);
        log.info("Alert triggered: rule='{}' entity={}", ruleName, relatedEntityId);
        return saved;
    }

    public void sendNotification(Alert alert, Long userId, String employeeId,
                                 String userName, String message) {
        Notification n = new Notification();
        n.setAlertId(alert.getAlertId());
        n.setUserId(userId);
        n.setEmployeeId(employeeId);
        n.setUserName(userName);
        n.setChannel("IN_APP");
        n.setMessage(message);
        n.setStatus("SENT");
        n.setSentAt(java.time.LocalDateTime.now());
        notificationRepository.save(n);
        log.info("Notification sent to userId={}", userId);
    }

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
        // Validate user exists (matches monolith behaviour)
        try {
            IdentityApiResponse<UserInfo> resp = identityClient.getUserById(userId);
            if (resp == null || resp.getData() == null)
                throw new ResourceNotFoundException("User not found with id: " + userId);
        } catch (ResourceNotFoundException ex) { throw ex; }
        catch (Exception e) { log.warn("Could not validate userId={} with identity service: {}", userId, e.getMessage()); }
        return notificationRepository.findByUserId(userId, pageable)
                .map(this::toNotificationResponse);
    }

    public List<NotificationResponse> getUnreadNotificationsByUser(Long userId) {
        // Validate user exists (matches monolith behaviour)
        try {
            IdentityApiResponse<UserInfo> resp = identityClient.getUserById(userId);
            if (resp == null || resp.getData() == null)
                throw new ResourceNotFoundException("User not found with id: " + userId);
        } catch (ResourceNotFoundException ex) { throw ex; }
        catch (Exception e) { log.warn("Could not validate userId={} with identity service: {}", userId, e.getMessage()); }
        return notificationRepository.findByUserIdAndStatus(userId, "SENT")
                .stream().map(this::toNotificationResponse).collect(Collectors.toList());
    }

    // Called by fp_maintenance to notify technicians about work orders
    public void createInternalNotification(Long userId, String employeeId, String userName, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setEmployeeId(employeeId);
        n.setUserName(userName);
        n.setAlertId(null);
        n.setChannel("IN_APP");
        n.setMessage(message);
        n.setStatus("SENT");
        n.setSentAt(LocalDateTime.now());
        notificationRepository.save(n);
        log.info("Internal notification created for userId={}", userId);
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

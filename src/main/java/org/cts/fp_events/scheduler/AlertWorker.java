package org.cts.fp_events.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.response.AlertRuleInfo;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.ShiftAllocationResponse;
import org.cts.fp_events.dto.response.UserInfo;
import org.cts.fp_events.model.Alert;
import org.cts.fp_events.model.DowntimeEvent;
import org.cts.fp_events.repository.AlertRepository;
import org.cts.fp_events.repository.DowntimeEventRepository;
import org.cts.fp_events.service.AlertService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Runs every 30 seconds — mirrors the monolith's AlertWorker exactly.
 * 1. Fetches active alert rules from fp_identity (via Feign service token).
 * 2. Evaluates each rule against local downtime data.
 * 3. Creates an Alert + Notifications when the condition is first met.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlertWorker {

    private final IdentityClient          identityClient;
    private final DowntimeEventRepository downtimeEventRepository;
    private final AlertRepository         alertRepository;
    private final AlertService            alertService;

    @Scheduled(fixedRate = 30000)
    public void evaluateAlertRules() {
        List<AlertRuleInfo> activeRules = fetchActiveRules();
        for (AlertRuleInfo rule : activeRules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("Alert rule evaluation failed for rule '{}': {}", rule.getName(), e.getMessage());
            }
        }
    }

    private List<AlertRuleInfo> fetchActiveRules() {
        try {
            IdentityApiResponse<List<AlertRuleInfo>> resp = identityClient.getActiveAlertRules(true);
            if (resp != null && resp.getData() != null) return resp.getData();
        } catch (Exception e) {
            log.warn("AlertWorker: could not fetch alert rules — {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private void evaluateRule(AlertRuleInfo rule) {
        String expr = rule.getTriggerExpression().toLowerCase().trim();

        if (expr.startsWith("downtime >")) {
            int thresholdMinutes = Integer.parseInt(expr.replace("downtime >", "").trim());
            List<DowntimeEvent> activeDowntimes = downtimeEventRepository.findByEndAtIsNull();

            for (DowntimeEvent dt : activeDowntimes) {
                long durationMin = Duration.between(dt.getStartAt(), LocalDateTime.now()).toMinutes();

                if (durationMin > thresholdMinutes) {
                    boolean alreadyAlerted = alertRepository
                            .existsByRuleIdAndRelatedEntityIdAndStatus(
                                    rule.getRuleId(), dt.getDowntimeId(), "OPEN");
                    if (!alreadyAlerted) {
                        String notes = String.format(
                                "Machine '%s' has been down for %d minutes",
                                dt.getMachineName(), durationMin);
                        Alert alert = alertService.triggerAlert(
                                rule.getRuleId(), rule.getName(), rule.getSeverity(),
                                dt.getDowntimeId(), "DOWNTIME", notes);

                        String message = String.format(
                                "ALERT: Machine '%s' on Line '%s' has been down for %d minutes.",
                                dt.getMachineName(), dt.getLineName(), durationMin);
                        notifyRecipients(rule, alert, message);
                    }
                }
            }
        }
    }

    private void notifyRecipients(AlertRuleInfo rule, Alert alert, String message) {
        if (rule.getRecipientsJson() == null || rule.getRecipientsJson().isEmpty()) return;

        for (String roleName : rule.getRecipientsJson()) {
            // 1) Try on-duty users for this role
            List<ShiftAllocationResponse> onDuty = fetchOnDuty(roleName);

            if (onDuty.isEmpty()) {
                // 2) Fallback — notify all active users with this role
                log.warn("AlertWorker: no on-duty {} found. Falling back to all active users.", roleName);
                List<UserInfo> allUsers = fetchUsersByRole(roleName);
                for (UserInfo u : allUsers) {
                    if ("ACTIVE".equalsIgnoreCase(u.getStatus())) {
                        alertService.sendNotification(alert, u.getUserId(),
                                u.getEmployeeId(), u.getName(), message);
                    }
                }
            } else {
                for (ShiftAllocationResponse u : onDuty) {
                    alertService.sendNotification(alert, u.getUserId(),
                            u.getEmployeeId(), u.getUserName(), message);
                }
            }
        }
    }

    private List<ShiftAllocationResponse> fetchOnDuty(String role) {
        try {
            IdentityApiResponse<List<ShiftAllocationResponse>> resp =
                    identityClient.getOnDuty(role);
            if (resp != null && resp.getData() != null) return resp.getData();
        } catch (Exception e) {
            log.warn("AlertWorker: could not fetch on-duty {} — {}", role, e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<UserInfo> fetchUsersByRole(String role) {
        try {
            IdentityApiResponse<List<UserInfo>> resp = identityClient.getUsersByRole(role);
            if (resp != null && resp.getData() != null) return resp.getData();
        } catch (Exception e) {
            log.warn("AlertWorker: could not fetch users with role {} — {}", role, e.getMessage());
        }
        return Collections.emptyList();
    }
}

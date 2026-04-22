package org.cts.fp_identity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.AlertRuleRequest;
import org.cts.fp_identity.dto.response.AlertRuleResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.AlertRule;
import org.cts.fp_identity.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AlertRuleResponse createAlertRule(AlertRuleRequest request) throws Exception {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setTriggerExpression(request.getTriggerExpression());
        rule.setSeverity(request.getSeverity());
        rule.setRecipientsJson(objectMapper.writeValueAsString(request.getRecipientsJson()));
        rule.setActive(request.getActive());
        AlertRule saved = alertRuleRepository.save(rule);
        auditLogService.log("CREATE_ALERT_RULE", "AlertRule", "Created alert rule: " + saved.getName());
        return toResponse(saved);
    }

    public List<AlertRuleResponse> getAllAlertRules() {
        return alertRuleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AlertRuleResponse getAlertRuleById(Long id) {
        return toResponse(alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule not found: " + id)));
    }

    public List<AlertRuleResponse> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AlertRuleResponse updateAlertRule(Long id, AlertRuleRequest request) throws Exception {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule not found: " + id));
        rule.setName(request.getName());
        rule.setTriggerExpression(request.getTriggerExpression());
        rule.setSeverity(request.getSeverity());
        rule.setRecipientsJson(objectMapper.writeValueAsString(request.getRecipientsJson()));
        rule.setActive(request.getActive());
        AlertRule saved = alertRuleRepository.save(rule);
        auditLogService.log("UPDATE_ALERT_RULE", "AlertRule", "Updated alert rule ID: " + id);
        return toResponse(saved);
    }

    public AlertRuleResponse toggleActive(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule not found: " + id));
        rule.setActive(!rule.getActive());
        AlertRule saved = alertRuleRepository.save(rule);
        auditLogService.log("TOGGLE_ALERT_RULE", "AlertRule", "Toggled alert rule ID: " + id + ", active=" + saved.getActive());
        return toResponse(saved);
    }

    public void deleteAlertRule(Long id) {
        alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule not found: " + id));
        alertRuleRepository.deleteById(id);
        auditLogService.log("DELETE_ALERT_RULE", "AlertRule", "Deleted alert rule ID: " + id);
    }

    private AlertRuleResponse toResponse(AlertRule r) {
        List<String> recipients = new ArrayList<>();
        try {
            if (r.getRecipientsJson() != null && !r.getRecipientsJson().isBlank()) {
                recipients = objectMapper.readValue(r.getRecipientsJson(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            // log or ignore parse error
        }
        return AlertRuleResponse.builder()
                .ruleId(r.getRuleId())
                .name(r.getName())
                .triggerExpression(r.getTriggerExpression())
                .severity(r.getSeverity())
                .recipientsJson(recipients)
                .active(r.getActive())
                .build();
    }
}

package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.AlertRuleRequest;
import org.cts.fp_identity.dto.response.AlertRuleResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.AlertRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alertrules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createAlertRule(@Valid @RequestBody AlertRuleRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Alert rule created", alertRuleService.createAlertRule(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAlertRules(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Boolean active) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Alert rule fetched", alertRuleService.getAlertRuleById(id)));
        if (Boolean.TRUE.equals(active))
            return ResponseEntity.ok(ApiResponse.success("Active rules fetched", alertRuleService.getActiveAlertRules()));
        return ResponseEntity.ok(ApiResponse.success("Alert rules fetched", alertRuleService.getAllAlertRules()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateAlertRule(@PathVariable Long id,
            @Valid @RequestBody AlertRuleRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.success("Alert rule updated", alertRuleService.updateAlertRule(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Alert rule toggled", alertRuleService.toggleActive(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlertRule(@PathVariable Long id) {
        alertRuleService.deleteAlertRule(id);
        return ResponseEntity.ok(ApiResponse.success("Alert rule deleted", null));
    }
}

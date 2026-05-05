package org.cts.fp_events.client;

import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.response.AlertRuleInfo;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.LineInfo;
import org.cts.fp_events.dto.response.MachineInfo;
import org.cts.fp_events.dto.response.RootCauseInfo;
import org.cts.fp_events.dto.response.ShiftAllocationResponse;
import org.cts.fp_events.dto.response.UserInfo;
import org.cts.fp_events.client.fallback.IdentityClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "fp-identity", fallback = IdentityClientFallback.class)
public interface IdentityClient {

    // ── Audit Logs ──────────────────────────────────────────────────────────
    @PostMapping("/api/audit-logs/record")
    void recordAuditLog(@RequestBody AuditLogRequest request);

    // ── Line / Machine lookup (for denormalising downtime records) ───────────
    @GetMapping("/api/lines")
    IdentityApiResponse<LineInfo> getLineById(@RequestParam Long id);

    @GetMapping("/api/machines")
    IdentityApiResponse<MachineInfo> getMachineById(@RequestParam Long id);

    @PatchMapping("/api/machines/{id}/status")
    IdentityApiResponse<MachineInfo> updateMachineStatus(@PathVariable Long id, @RequestParam String status);

    // ── Shift Allocations (read-only, owned by fp_identity) ─────────────────
    @GetMapping("/api/shift-allocations")
    IdentityApiResponse<List<ShiftAllocationResponse>> getAllocations(
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search);

    @GetMapping("/api/shift-allocations/on-duty")
    IdentityApiResponse<List<ShiftAllocationResponse>> getOnDuty(
            @RequestParam(required = false) String role);

    @GetMapping("/api/shift-allocations/at-time")
    IdentityApiResponse<List<ShiftAllocationResponse>> getAllocationsAtTime(
            @RequestParam String dateTime);

    // ── Alert Rules (owned by fp_identity) ─────────────────────────────────
    @GetMapping("/api/alertrules")
    IdentityApiResponse<List<AlertRuleInfo>> getActiveAlertRules(
            @RequestParam(defaultValue = "true") boolean active);

    // ── Users by role (fallback when no on-duty staff found) ────────────────
    @GetMapping("/api/users")
    IdentityApiResponse<List<UserInfo>> getUsersByRole(@RequestParam String role);

    // ── User by ID (for resolving assignedToName / employeeId) ──────────────
    @GetMapping("/api/users")
    IdentityApiResponse<UserInfo> getUserById(@RequestParam Long id);

    // ── Root cause by ID (for resolving code / description) ─────────────────
    @GetMapping("/api/rootcauses")
    IdentityApiResponse<RootCauseInfo> getRootCauseById(@RequestParam Long id);
}

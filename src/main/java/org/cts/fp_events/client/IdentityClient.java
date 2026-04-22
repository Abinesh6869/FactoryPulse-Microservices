package org.cts.fp_events.client;

import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.ShiftAllocationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "identity-client", url = "${fp.identity-base-url:http://localhost:9090}")
public interface IdentityClient {

    // ── Audit Logs ──────────────────────────────────────────────────────────
    @PostMapping("/api/audit-logs/record")
    void recordAuditLog(@RequestBody AuditLogRequest request);

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
}

package org.cts.fp_maintenance.client;

import org.cts.fp_maintenance.dto.request.AuditLogRequest;
import org.cts.fp_maintenance.dto.response.IdentityApiResponse;
import org.cts.fp_maintenance.dto.response.MachineInfo;
import org.cts.fp_maintenance.dto.response.UserInfo;
import org.cts.fp_maintenance.client.fallback.IdentityClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "fp-identity", fallback = IdentityClientFallback.class)
public interface IdentityClient {

    @PostMapping("/api/audit-logs/record")
    void recordAuditLog(@RequestBody AuditLogRequest request);

    // ── Lookup for denormalization ────────────────────────────────────────────
    @GetMapping("/api/machines")
    IdentityApiResponse<MachineInfo> getMachineById(@RequestParam Long id);

    @GetMapping("/api/users")
    IdentityApiResponse<UserInfo> getUserById(@RequestParam Long id);

    // ── Machine status update (called after maintenance completes) ────────────
    @PatchMapping("/api/machines/{id}/status")
    IdentityApiResponse<MachineInfo> updateMachineStatus(@PathVariable Long id, @RequestParam String status);
}

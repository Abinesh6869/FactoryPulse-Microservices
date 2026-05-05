package org.cts.fp_telemetry.client;

import org.cts.fp_telemetry.dto.request.AuditLogRequest;
import org.cts.fp_telemetry.dto.response.IdentityApiResponse;
import org.cts.fp_telemetry.dto.response.LineInfo;
import org.cts.fp_telemetry.dto.response.MachineInfo;
import org.cts.fp_telemetry.dto.response.ShiftInfo;
import org.cts.fp_telemetry.dto.response.TelemetryPointInfo;
import org.cts.fp_telemetry.client.fallback.IdentityClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "fp-identity", fallback = IdentityClientFallback.class)
public interface IdentityClient {

    @PostMapping("/api/audit-logs/record")
    void recordAuditLog(@RequestBody AuditLogRequest request);

    // ── Simulator data ───────────────────────────────────────────────────────

    @GetMapping("/api/machines")
    IdentityApiResponse<List<MachineInfo>> getAllMachines();

    @GetMapping("/api/machines")
    IdentityApiResponse<MachineInfo> getMachineById(@RequestParam Long id);

    @GetMapping("/api/telemetry-points")
    IdentityApiResponse<List<TelemetryPointInfo>> getTelemetryPointsByMachine(@RequestParam Long machineId);

    @GetMapping("/api/shifts")
    IdentityApiResponse<List<ShiftInfo>> getShiftsByDate(@RequestParam String date);

    // ── Existence checks (404 guards) ────────────────────────────────────────

    @GetMapping("/api/lines")
    IdentityApiResponse<LineInfo> getLineById(@RequestParam Long id);

    @GetMapping("/api/shifts")
    IdentityApiResponse<ShiftInfo> getShiftById(@RequestParam Long id);

    @GetMapping("/api/telemetry-points")
    IdentityApiResponse<TelemetryPointInfo> getPointById(@RequestParam Long id);
}

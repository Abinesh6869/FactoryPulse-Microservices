package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.response.LineInfo;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ShiftInfo;
import org.cts.fp_reporting.client.fallback.IdentityClientFallback;
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

    @GetMapping("/api/lines")
    ServiceApiResponse<List<LineInfo>> getAllLines();

    @GetMapping("/api/lines")
    ServiceApiResponse<LineInfo> getLineById(@RequestParam Long id);

    @GetMapping("/api/shifts")
    ServiceApiResponse<List<ShiftInfo>> getShiftsByDate(@RequestParam String date);

    @GetMapping("/api/shifts")
    ServiceApiResponse<ShiftInfo> getShiftById(@RequestParam Long id);
}

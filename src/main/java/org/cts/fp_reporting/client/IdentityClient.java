package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "identity-client", url = "${fp.identity-base-url:http://localhost:9090}")
public interface IdentityClient {

    @PostMapping("/api/audit-logs/record")
    void recordAuditLog(@RequestBody AuditLogRequest request);
}

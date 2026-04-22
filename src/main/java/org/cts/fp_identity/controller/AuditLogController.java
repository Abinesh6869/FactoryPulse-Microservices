package org.cts.fp_identity.controller;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.AuditLogRequest;
import org.cts.fp_identity.dto.response.AuditLogResponse;
import org.cts.fp_identity.dto.response.PageResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/record")
    public ResponseEntity<Void> recordAuditLog(@RequestBody AuditLogRequest request) {
        auditLogService.log(request.getAction(), request.getResource(), request.getDetails());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched successfully",
                new PageResponse<>(auditLogService.getAllAuditLogs(search, pageable))));
    }
}

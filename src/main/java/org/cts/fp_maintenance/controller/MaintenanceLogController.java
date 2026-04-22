package org.cts.fp_maintenance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_maintenance.dto.request.MaintenanceLogRequest;
import org.cts.fp_maintenance.dto.response.MaintenanceLogResponse;
import org.cts.fp_maintenance.dto.response.PageResponse;
import org.cts.fp_maintenance.exception.ApiResponse;
import org.cts.fp_maintenance.security.UserPrincipal;
import org.cts.fp_maintenance.service.MaintenanceLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MaintenanceLogController {
    private final MaintenanceLogService maintenanceLogService;

    // POST /api/maintenance-logs
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceLogResponse>> createLog(
            @Valid @RequestBody MaintenanceLogRequest request, Authentication authentication) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Maintenance log created successfully",
                        maintenanceLogService.createLog(request,
                                principal.getUserId(),
                                principal.getUsername(),
                                principal.getEmployeeId())));
    }

    // GET /api/maintenance-logs
    // GET /api/maintenance-logs?id=1
    // GET /api/maintenance-logs?workOrderId=1
    // GET /api/maintenance-logs?machineId=1
    // GET /api/maintenance-logs
    // GET /api/maintenance-logs?id=1
    // GET /api/maintenance-logs?workOrderId=1
    // GET /api/maintenance-logs?machineId=1
    // GET /api/maintenance-logs?search=belt
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getLogs(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "logId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Maintenance log fetched successfully",
                    maintenanceLogService.getLogById(id)));
        if (workOrderId != null)
            return ResponseEntity.ok(ApiResponse.success("Maintenance logs fetched successfully",
                    maintenanceLogService.getLogsByWorkOrder(workOrderId)));
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("Maintenance logs fetched successfully",
                    maintenanceLogService.getLogsByMachine(machineId)));
        return ResponseEntity.ok(ApiResponse.success("Maintenance logs fetched successfully",
                new PageResponse<>(maintenanceLogService.getAllLogs(search, pageable))));
    }
}

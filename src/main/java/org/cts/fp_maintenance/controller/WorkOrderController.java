package org.cts.fp_maintenance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_maintenance.dto.request.WorkOrderRequest;
import org.cts.fp_maintenance.dto.response.PageResponse;
import org.cts.fp_maintenance.dto.response.WorkOrderResponse;
import org.cts.fp_maintenance.exception.ApiResponse;
import org.cts.fp_maintenance.security.UserPrincipal;
import org.cts.fp_maintenance.service.WorkOrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(
            @Valid @RequestBody WorkOrderRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work order created successfully",
                        workOrderService.createWorkOrder(request,
                                principal.getUserId(),
                                principal.getUsername(),
                                principal.getEmployeeId())));
    }

    // GET /api/workorders
    // GET /api/workorders?id=1
    // GET /api/workorders?status=OPEN
    // GET /api/workorders?machineId=1
    // GET /api/workorders
    // GET /api/workorders?id=1
    // GET /api/workorders?status=OPEN
    // GET /api/workorders?machineId=1
    // GET /api/workorders?search=conveyor
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "workOrderId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Work order fetched successfully",
                    workOrderService.getWorkOrderById(id)));
        if (status != null)
            return ResponseEntity.ok(ApiResponse.success("Work orders fetched successfully",
                    workOrderService.getWorkOrdersByStatus(status)));
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("Work orders fetched successfully",
                    workOrderService.getWorkOrdersByMachine(machineId)));
        return ResponseEntity.ok(ApiResponse.success("Work orders fetched successfully",
                new PageResponse<>(workOrderService.getAllWorkOrders(search, pageable))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Work order status updated successfully",
                workOrderService.updateStatus(id, status)));
    }

    // GET /api/workorders/my → Technician sees their own assigned work orders
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getMyWorkOrders(Authentication authentication) {
        Long technicianId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Your work orders fetched successfully",
                workOrderService.getMyWorkOrders(technicianId)));
    }

    // PATCH /api/workorders/{id}/reassign?technicianId=5
    @PatchMapping("/{id}/reassign")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> reassign(
            @PathVariable Long id,
            @RequestParam Long technicianId,
            @RequestParam(required = false) String technicianName,
            @RequestParam(required = false) String technicianEmployeeId) {
        return ResponseEntity.ok(ApiResponse.success("Work order reassigned successfully",
                workOrderService.reassign(id, technicianId, technicianName, technicianEmployeeId)));
    }
}

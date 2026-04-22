package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.ShiftAllocationRequest;
import org.cts.fp_identity.dto.response.ShiftAllocationResponse;
import org.cts.fp_identity.dto.response.ShiftAllocationSummaryResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.security.UserPrincipal;
import org.cts.fp_identity.service.ShiftAllocationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shift-allocations")
@RequiredArgsConstructor
public class ShiftAllocationController {

    private final ShiftAllocationService shiftAllocationService;

    @PostMapping("/{shiftId}/allocate")
    public ResponseEntity<ApiResponse<ShiftAllocationSummaryResponse>> allocate(
            @PathVariable Long shiftId,
            @Valid @RequestBody ShiftAllocationRequest request,
            Authentication authentication) {
        Long allocatedById = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        ShiftAllocationSummaryResponse result = shiftAllocationService.allocate(shiftId, request, allocatedById);
        String message = result.getAllocated().isEmpty()
                ? "No users were allocated to the shift."
                : result.getAllocated().size() + " user(s) allocated to shift successfully.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getAllocations(
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (shiftId != null && role != null)
            return ResponseEntity.ok(ApiResponse.success("Allocations fetched successfully",
                    shiftAllocationService.getAllocationsByShiftAndRole(shiftId, role)));
        if (shiftId != null)
            return ResponseEntity.ok(ApiResponse.success("Allocations fetched successfully",
                    shiftAllocationService.getAllocationsByShift(shiftId)));
        if (userId != null)
            return ResponseEntity.ok(ApiResponse.success("Allocations fetched successfully",
                    shiftAllocationService.getAllocationsByUser(userId)));
        return ResponseEntity.ok(ApiResponse.success("Allocations fetched successfully",
                shiftAllocationService.getAllAllocations(search)));
    }

    @DeleteMapping("/{allocationId}")
    public ResponseEntity<ApiResponse<Void>> removeAllocation(@PathVariable Long allocationId) {
        shiftAllocationService.removeAllocation(allocationId);
        return ResponseEntity.ok(ApiResponse.success("Allocation removed successfully", null));
    }

    @GetMapping("/on-duty")
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getOnDuty(
            @RequestParam(required = false) String role) {
        if (role != null)
            return ResponseEntity.ok(ApiResponse.success("On-duty staff fetched successfully",
                    shiftAllocationService.getOnDutyByRole(role)));
        return ResponseEntity.ok(ApiResponse.success("On-duty staff fetched successfully",
                shiftAllocationService.getAllOnDutyAllocations()));
    }

    @GetMapping("/at-time")
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getAllocationsAtTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        return ResponseEntity.ok(ApiResponse.success("Allocations at given time fetched successfully",
                shiftAllocationService.getAllocationsAtTime(dateTime)));
    }
}

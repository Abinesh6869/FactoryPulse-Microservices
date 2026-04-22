package org.cts.fp_events.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.response.ShiftAllocationResponse;
import org.cts.fp_events.exception.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/shift-allocations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ShiftAllocationController {

    private final IdentityClient identityClient;

    // GET /api/shift-allocations
    // GET /api/shift-allocations?shiftId=1
    // GET /api/shift-allocations?shiftId=1&role=OPERATOR
    // GET /api/shift-allocations?userId=1
    // GET /api/shift-allocations?search=john
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getAllocations(
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search) {
        List<ShiftAllocationResponse> data = identityClient
                .getAllocations(shiftId, role, userId, search)
                .getData();
        return ResponseEntity.ok(ApiResponse.success("Allocations fetched successfully", data));
    }

    // GET /api/shift-allocations/on-duty
    // GET /api/shift-allocations/on-duty?role=TECHNICIAN
    @GetMapping("/on-duty")
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getOnDuty(
            @RequestParam(required = false) String role) {
        List<ShiftAllocationResponse> data = identityClient.getOnDuty(role).getData();
        return ResponseEntity.ok(ApiResponse.success("On-duty staff fetched successfully", data));
    }

    // GET /api/shift-allocations/at-time?dateTime=2026-03-30T14:30:00
    @GetMapping("/at-time")
    public ResponseEntity<ApiResponse<List<ShiftAllocationResponse>>> getAllocationsAtTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        List<ShiftAllocationResponse> data = identityClient
                .getAllocationsAtTime(dateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .getData();
        return ResponseEntity.ok(ApiResponse.success("Allocations at given time fetched successfully", data));
    }
}

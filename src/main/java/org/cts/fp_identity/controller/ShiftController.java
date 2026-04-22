package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.ShiftRequest;
import org.cts.fp_identity.dto.response.PageResponse;
import org.cts.fp_identity.dto.response.ShiftResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.ShiftService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftResponse>> createShift(@Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift created successfully", shiftService.createShift(request)));
    }

    // GET /api/shifts
    // GET /api/shifts?id=1
    // GET /api/shifts?date=2026-03-24
    // GET /api/shifts?from=2026-03-01&to=2026-03-31
    // GET /api/shifts?search=morning
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getShifts(
            @PageableDefault(size = 10, sort = "shiftId", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Shift fetched successfully", shiftService.getShiftById(id)));
        if (date != null)
            return ResponseEntity.ok(ApiResponse.success("Shifts fetched successfully", shiftService.getShiftsByDate(date)));
        if (from != null && to != null)
            return ResponseEntity.ok(ApiResponse.success("Shifts fetched successfully", shiftService.getShiftsByDateRange(from, to)));
        return ResponseEntity.ok(ApiResponse.success("Shifts fetched successfully",
                new PageResponse<>(shiftService.getAllShifts(search, pageable))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftResponse>> updateShift(@PathVariable Long id,
            @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shift updated successfully", shiftService.updateShift(id, request)));
    }
}

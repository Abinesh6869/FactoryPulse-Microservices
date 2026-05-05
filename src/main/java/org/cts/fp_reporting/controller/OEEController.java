package org.cts.fp_reporting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.cts.fp_reporting.dto.response.OEERecordResponse;
import org.cts.fp_reporting.dto.response.PageResponse;
import org.cts.fp_reporting.exception.ApiResponse;
import org.cts.fp_reporting.service.OEEService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/oee")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OEEController {

    private final OEEService oeeService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<OEERecordResponse>> calculateOEE(
            @RequestParam Long lineId,
            @RequestParam Long shiftId) {
        return ResponseEntity.ok(ApiResponse.success("OEE calculated successfully",
                oeeService.calculateAndSaveOEE(lineId, shiftId, null)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getOEE(
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) Boolean latest,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 10) Pageable pageable) {
        if (lineId != null && Boolean.TRUE.equals(latest))
            return ResponseEntity.ok(ApiResponse.success("Latest OEE fetched successfully",
                    oeeService.getLatestOEEByLine(lineId)));
        if (lineId != null && from != null && to != null)
            return ResponseEntity.ok(ApiResponse.success("OEE records fetched successfully",
                    new PageResponse<>(oeeService.getOEEByLineAndDateRange(lineId, from, to, pageable))));
        if (shiftId != null)
            return ResponseEntity.ok(ApiResponse.success("OEE records fetched successfully",
                    new PageResponse<>(oeeService.getOEEByShift(shiftId, pageable))));
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("OEE records fetched successfully",
                    new PageResponse<>(oeeService.getOEEByLine(lineId, pageable))));
        return ResponseEntity.ok(ApiResponse.success("Please provide lineId or shiftId", null));
    }
}

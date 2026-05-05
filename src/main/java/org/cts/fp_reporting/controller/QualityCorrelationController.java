package org.cts.fp_reporting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_reporting.dto.request.QualityCorrelationRequest;
import org.cts.fp_reporting.dto.response.PageResponse;
import org.cts.fp_reporting.dto.response.QualityCorrelationResponse;
import org.cts.fp_reporting.dto.response.QualitySummaryResponse;
import org.cts.fp_reporting.exception.ApiResponse;
import org.cts.fp_reporting.service.QualityCorrelationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quality-correlation")
@SecurityRequirement(name = "bearerAuth")
public class QualityCorrelationController {

    private final QualityCorrelationService qualityCorrelationService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<QualitySummaryResponse>> getSummaryByLine(
            @RequestParam Long lineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success("Quality summary fetched successfully",
                qualityCorrelationService.getSummaryByLine(lineId, from, to)));
    }

    @PostMapping("/records")
    public ResponseEntity<ApiResponse<QualityCorrelationResponse>> createQualityRecord(
            @Valid @RequestBody QualityCorrelationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quality record created successfully",
                        qualityCorrelationService.createQualityRecord(request)));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<PageResponse<QualityCorrelationResponse>>> getQualityRecords(
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Boolean my,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (Boolean.TRUE.equals(my))
            return ResponseEntity.ok(ApiResponse.success("Your quality records fetched successfully",
                    new PageResponse<>(qualityCorrelationService.getMyQualityRecords(pageable))));
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("Quality records fetched successfully",
                    new PageResponse<>(qualityCorrelationService.getQualityRecordsByLine(lineId, pageable))));
        return ResponseEntity.ok(ApiResponse.success("Quality records fetched successfully",
                new PageResponse<>(qualityCorrelationService.getAllQualityRecords(pageable))));
    }

    @PatchMapping("/records/{id}")
    public ResponseEntity<ApiResponse<QualityCorrelationResponse>> updateQualityRecord(
            @PathVariable Long id, @RequestBody QualityCorrelationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Quality record updated successfully",
                qualityCorrelationService.updateQualityRecord(id, request)));
    }
}

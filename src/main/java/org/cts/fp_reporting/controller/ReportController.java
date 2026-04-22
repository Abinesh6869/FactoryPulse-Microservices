package org.cts.fp_reporting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_reporting.dto.request.ReportRequest;
import org.cts.fp_reporting.dto.response.PageResponse;
import org.cts.fp_reporting.dto.response.ReportResponse;
import org.cts.fp_reporting.exception.ApiResponse;
import org.cts.fp_reporting.security.UserPrincipal;
import org.cts.fp_reporting.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    // POST /api/reports/generate
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) throws Exception {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated successfully",
                        reportService.generateReport(request,
                                principal.getUserId(),
                                principal.getUsername())));
    }

    // GET /api/reports
    // GET /api/reports?id=1
    // GET /api/reports?search=daily
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getReports(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "reportId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Report fetched successfully",
                    reportService.getReportById(id)));
        return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully",
                new PageResponse<>(reportService.getAllReports(search, pageable))));
    }
}

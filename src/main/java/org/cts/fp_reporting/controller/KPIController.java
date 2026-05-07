package org.cts.fp_reporting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_reporting.dto.request.KPIRequest;
import org.cts.fp_reporting.dto.response.KPIResponse;
import org.cts.fp_reporting.exception.ApiResponse;
import org.cts.fp_reporting.scheduler.KpiWorker;
import org.cts.fp_reporting.service.KPIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kpis")
@SecurityRequirement(name = "bearerAuth")
public class KPIController {

    private final KPIService kpiService;
    private final KpiWorker  kpiWorker;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Void>> triggerKpiCalculation() {
        kpiWorker.calculateCurrentMonthKPIs();
        return ResponseEntity.ok(ApiResponse.success("KPI calculation triggered for current month", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KPIResponse>> createKPI(@RequestBody @Valid KPIRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("KPI created successfully", kpiService.createKPI(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KPIResponse>> updateKPI(
            @PathVariable Long id, @RequestBody @Valid KPIRequest request) {
        return ResponseEntity.ok(ApiResponse.success("KPI updated successfully", kpiService.updateKPI(id, request)));
    }

    @PatchMapping("/{id}/value")
    public ResponseEntity<ApiResponse<KPIResponse>> updateKPIValue(
            @PathVariable Long id, @RequestParam Double value) {
        return ResponseEntity.ok(ApiResponse.success("KPI value updated successfully",
                kpiService.updateCurrentValue(id, value)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getKPIs(@RequestParam(required = false) Long id) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("KPI fetched successfully", kpiService.getKPIById(id)));
        return ResponseEntity.ok(ApiResponse.success("KPIs fetched successfully", kpiService.getAllKPIs()));
    }
}

package org.cts.fp_reporting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_reporting.dto.request.ThroughputForecastRequest;
import org.cts.fp_reporting.dto.response.PageResponse;
import org.cts.fp_reporting.dto.response.ThroughputCompareResponse;
import org.cts.fp_reporting.dto.response.ThroughputForecastResponse;
import org.cts.fp_reporting.exception.ApiResponse;
import org.cts.fp_reporting.service.ThroughputForecastService;
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
@RequestMapping("/api/throughput-forecasts")
@SecurityRequirement(name = "bearerAuth")
public class ThroughputForecastController {

    private final ThroughputForecastService throughputForecastService;

    @PostMapping
    public ResponseEntity<ApiResponse<ThroughputForecastResponse>> create(
            @RequestBody @Valid ThroughputForecastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Forecast report created successfully",
                        throughputForecastService.createThroughputForecast(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getForecasts(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 10, sort = "forecastId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Forecast fetched successfully",
                    throughputForecastService.getById(id)));
        if (lineId != null && Boolean.TRUE.equals(active))
            return ResponseEntity.ok(ApiResponse.success("Forecast fetched successfully",
                    throughputForecastService.getActiveByLine(lineId)));
        if (lineId != null && start != null && end != null)
            return ResponseEntity.ok(ApiResponse.success("Forecast fetched successfully",
                    throughputForecastService.getByLineAndPeriod(lineId, start, end)));
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("Forecast fetched successfully",
                    throughputForecastService.getByLine(lineId)));
        return ResponseEntity.ok(ApiResponse.success("Forecast fetched successfully",
                new PageResponse<>(throughputForecastService.getAll(pageable))));
    }

    @GetMapping("/{forecastId}/compare")
    public ResponseEntity<ApiResponse<ThroughputCompareResponse>> compare(@PathVariable Long forecastId) {
        return ResponseEntity.ok(ApiResponse.success("Forecast comparison fetched successfully",
                throughputForecastService.compare(forecastId)));
    }
}

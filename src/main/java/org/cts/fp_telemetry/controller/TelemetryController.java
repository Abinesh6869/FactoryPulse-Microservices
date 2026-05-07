package org.cts.fp_telemetry.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_telemetry.dto.request.ProductionCountRequest;
import org.cts.fp_telemetry.dto.request.TelemetryEventRequest;
import org.cts.fp_telemetry.dto.response.PageResponse;
import org.cts.fp_telemetry.dto.response.ProductionCountResponse;
import org.cts.fp_telemetry.dto.response.TelemetryEventResponse;
import org.cts.fp_telemetry.exception.ApiResponse;
import org.cts.fp_telemetry.service.TelemetryService;
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
@RequestMapping("/api/telemetry")
@SecurityRequirement(name = "bearerAuth")
public class TelemetryController {
    private final TelemetryService telemetryService;

    // GET /api/telemetry?machineId=1&from=...&to=...
    // GET /api/telemetry?machineId=1&latest=true
    // GET /api/telemetry?pointId=1&from=...&to=...
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getTelemetry(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) Long pointId,
            @RequestParam(required = false) Boolean latest,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "eventId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (machineId != null && Boolean.TRUE.equals(latest))
            return ResponseEntity.ok(ApiResponse.success("Latest events fetched successfully",
                    telemetryService.getLatestEventsByMachine(machineId)));
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("Events fetched successfully",
                    new PageResponse<>(telemetryService.getEventsByMachine(machineId, from, to, pageable))));
        if (pointId != null)
            return ResponseEntity.ok(ApiResponse.success("Events fetched successfully",
                    new PageResponse<>(telemetryService.getEventsByPoint(pointId, from, to, pageable))));
        return ResponseEntity.ok(ApiResponse.success("Please provide machineId or pointId", null));
    }

    // GET /api/telemetry/production?lineId=1&from=...&to=...
    // GET /api/telemetry/production?shiftId=1
    @GetMapping("/production")
    public ResponseEntity<ApiResponse<?>> getProduction(
            @PageableDefault(size = 10, sort = "countId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("Production counts fetched successfully",
                    new PageResponse<>(telemetryService.getProductionCountsByLine(lineId, from, to, pageable))));
        if (shiftId != null)
            return ResponseEntity.ok(ApiResponse.success("Production counts fetched successfully",
                    telemetryService.getProductionCountsByShift(shiftId, pageable)));
        return ResponseEntity.ok(ApiResponse.success("Please provide lineId or shiftId", null));
    }

    @GetMapping("/production/{countId}")
    public ResponseEntity<ApiResponse<ProductionCountResponse>> getProductionCountById(
            @PathVariable Long countId) {
        return ResponseEntity.ok(ApiResponse.success("Production count fetched successfully",
                telemetryService.getProductionCountById(countId)));
    }

    @PatchMapping("/production/updateCount/{countId}")
    public ResponseEntity<ApiResponse<ProductionCountResponse>> updateProductionCount(
            @PathVariable Long countId,
            @RequestParam(required = false) Integer goodCount,
            @RequestParam(required = false) Integer rejectCount) {
        return ResponseEntity.ok(ApiResponse.success("Production count updated successfully",
                telemetryService.updateProductionCount(countId, goodCount, rejectCount)));
    }

    // POST /api/telemetry  — manual telemetry event submission
    @PostMapping
    public ResponseEntity<ApiResponse<TelemetryEventResponse>> createEvent(
            @Valid @RequestBody TelemetryEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Telemetry event created successfully",
                        telemetryService.createEvent(request)));
    }

    // POST /api/telemetry/production — manual production count submission
    @PostMapping("/production")
    public ResponseEntity<ApiResponse<ProductionCountResponse>> createProductionCount(
            @Valid @RequestBody ProductionCountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Production count created successfully",
                        telemetryService.createProductionCount(request)));
    }
}

package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.TelemetryPointRequest;
import org.cts.fp_identity.dto.response.TelemetryPointResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.TelemetryPointService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry-points")
@RequiredArgsConstructor
public class TelemetryPointController {

    private final TelemetryPointService telemetryPointService;

    @PostMapping
    public ResponseEntity<ApiResponse<TelemetryPointResponse>> createPoint(@Valid @RequestBody TelemetryPointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Telemetry point created", telemetryPointService.createPoint(request)));
    }

    // GET /api/telemetry-points
    // GET /api/telemetry-points?id=1
    // GET /api/telemetry-points?machineId=1
    // GET /api/telemetry-points?search=temperature
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getPoints(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Telemetry point fetched successfully",
                    telemetryPointService.getTelemetryPointById(id)));
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("Telemetry points fetched successfully",
                    telemetryPointService.getPointsByMachine(machineId)));
        return ResponseEntity.ok(ApiResponse.success("Telemetry points fetched successfully",
                telemetryPointService.getAllPoints(search)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TelemetryPointResponse>> updatePoint(@PathVariable Long id,
            @Valid @RequestBody TelemetryPointRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Point updated", telemetryPointService.updatePoint(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePoint(@PathVariable Long id) {
        telemetryPointService.deletePoint(id);
        return ResponseEntity.ok(ApiResponse.success("Point deleted", null));
    }
}

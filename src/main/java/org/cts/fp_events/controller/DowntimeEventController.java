package org.cts.fp_events.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_events.dto.request.CorrectiveActionRequest;
import org.cts.fp_events.dto.request.DowntimeEventRequest;
import org.cts.fp_events.dto.response.CorrectiveActionResponse;
import org.cts.fp_events.dto.response.DowntimeEventResponse;
import org.cts.fp_events.dto.response.PageResponse;
import org.cts.fp_events.exception.ApiResponse;
import org.cts.fp_events.security.UserPrincipal;
import org.cts.fp_events.service.DowntimeEventService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/downtimes")
@SecurityRequirement(name = "bearerAuth")
public class DowntimeEventController {
    private final DowntimeEventService downtimeService;

    @PostMapping
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> createDowntime(
            @Valid @RequestBody DowntimeEventRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Downtime created successfully",
                        downtimeService.createDowntime(request,
                                principal.getUserId(),
                                principal.getUsername(),
                                principal.getEmployeeId(),
                                principal.getRole())));
    }

    // GET /api/downtimes
    // GET /api/downtimes?id=1
    // GET /api/downtimes?active=true
    // GET /api/downtimes?lineId=1&from=...&to=...
    // GET /api/downtimes?machineId=1&from=...&to=...
    // GET /api/downtimes
    // GET /api/downtimes?id=1
    // GET /api/downtimes?active=true
    // GET /api/downtimes?lineId=1&from=...&to=...
    // GET /api/downtimes?machineId=1&from=...&to=...
    // GET /api/downtimes?search=electrical
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDowntimes(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false, defaultValue = "") String search,
            Pageable pageable) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Downtime fetched successfully",
                    downtimeService.getDowntimeById(id)));
        if (Boolean.TRUE.equals(active))
            return ResponseEntity.ok(ApiResponse.success("Active downtimes fetched successfully",
                    downtimeService.getActiveDowntimes()));
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("Downtimes fetched successfully",
                    downtimeService.getDowntimesByLine(lineId, from, to)));
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("Downtimes fetched successfully",
                    downtimeService.getDowntimesByMachine(machineId, from, to)));
        if (from != null && to != null)
            return ResponseEntity.ok(ApiResponse.success("Downtimes fetched successfully",
                    downtimeService.getDowntimesByDateRange(from, to)));
        return ResponseEntity.ok(ApiResponse.success("Downtimes fetched successfully",
                new PageResponse<>(downtimeService.getAllDowntimes(search, pageable))));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> closeDowntime(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return ResponseEntity.ok(ApiResponse.success("Downtime closed successfully",
                downtimeService.closeDowntime(id, endAt)));
    }

    @PatchMapping("/{id}/rootcause")
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> tagRootCause(
            @PathVariable Long id,
            @RequestParam Long rootCauseId,
            @RequestParam(required = false) String rootCauseCode,
            @RequestParam(required = false) String rootCauseDescription) {
        return ResponseEntity.ok(ApiResponse.success("Root cause tagged successfully",
                downtimeService.tagRootCause(id, rootCauseId, rootCauseCode, rootCauseDescription)));
    }

    @PostMapping("/actions")
    public ResponseEntity<ApiResponse<CorrectiveActionResponse>> createCorrectiveAction(
            @Valid @RequestBody CorrectiveActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Corrective action created successfully",
                        downtimeService.createCorrectiveAction(request)));
    }

    @GetMapping("/{downtimeId}/actions")
    public ResponseEntity<ApiResponse<List<CorrectiveActionResponse>>> getActionsByDowntime(
            @PathVariable Long downtimeId) {
        return ResponseEntity.ok(ApiResponse.success("Corrective actions fetched successfully",
                downtimeService.getActionsByDowntime(downtimeId)));
    }

    @GetMapping("/my-actions")
    public ResponseEntity<ApiResponse<List<CorrectiveActionResponse>>> getMyActions(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("My corrective actions fetched successfully",
                downtimeService.getMyActions(userId)));
    }

    @PatchMapping("/actions/{actionId}/complete")
    public ResponseEntity<ApiResponse<CorrectiveActionResponse>> completeAction(@PathVariable Long actionId) {
        return ResponseEntity.ok(ApiResponse.success("Corrective action completed successfully",
                downtimeService.completeAction(actionId)));
    }
}

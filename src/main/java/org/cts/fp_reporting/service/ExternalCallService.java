package org.cts.fp_reporting.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.EventsClient;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.response.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalCallService {

    private final TelemetryClient telemetryClient;
    private final EventsClient eventsClient;
    private final IdentityClient identityClient;

    /* ── fp-telemetry ─────────────────────────────────────────────────── */

    @CircuitBreaker(name = "fp-telemetry", fallbackMethod = "productionFallback")
    public List<ProductionCountResponse> fetchProduction(Long lineId, String from, String to) {
        var resp = telemetryClient.getProductionByLine(lineId, from, to, 10000, 0);
        if (resp != null && resp.getData() != null && resp.getData().getContent() != null)
            return resp.getData().getContent();
        return Collections.emptyList();
    }

    public List<ProductionCountResponse> productionFallback(Long lineId, String from, String to, Throwable t) {
        log.warn("CB OPEN — fp-telemetry fallback for line {}: {}", lineId, t.getMessage());
        return Collections.emptyList();
    }

    /* ── fp-events ────────────────────────────────────────────────────── */

    @CircuitBreaker(name = "fp-events", fallbackMethod = "downtimeFallback")
    public List<DowntimeEventResponse> fetchDowntimes(String from, String to) {
        var resp = eventsClient.getAllDowntimesByDateRange(from, to);
        if (resp != null && resp.getData() != null) return resp.getData();
        return Collections.emptyList();
    }

    public List<DowntimeEventResponse> downtimeFallback(String from, String to, Throwable t) {
        log.warn("CB OPEN — fp-events fallback: {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "fp-events", fallbackMethod = "downtimeByLineFallback")
    public List<DowntimeEventResponse> fetchDowntimesByLine(Long lineId, String from, String to) {
        var resp = eventsClient.getDowntimesByLine(lineId, from, to);
        if (resp != null && resp.getData() != null) return resp.getData();
        return Collections.emptyList();
    }

    public List<DowntimeEventResponse> downtimeByLineFallback(Long lineId, String from, String to, Throwable t) {
        log.warn("CB OPEN — fp-events fallback for line {}: {}", lineId, t.getMessage());
        return Collections.emptyList();
    }

    /* ── fp-identity ──────────────────────────────────────────────────── */

    @CircuitBreaker(name = "fp-identity", fallbackMethod = "auditLogFallback")
    public void recordAuditLog(AuditLogRequest request) {
        identityClient.recordAuditLog(request);
    }

    public void auditLogFallback(AuditLogRequest request, Throwable t) {
        log.warn("CB OPEN — fp-identity fallback (audit log skipped): {}", t.getMessage());
    }

    @CircuitBreaker(name = "fp-identity", fallbackMethod = "shiftsByDateFallback")
    public List<ShiftInfo> fetchShiftsByDate(String date) {
        var resp = identityClient.getShiftsByDate(date);
        if (resp != null && resp.getData() != null) return resp.getData();
        return Collections.emptyList();
    }

    public List<ShiftInfo> shiftsByDateFallback(String date, Throwable t) {
        log.warn("CB OPEN — fp-identity fallback (shifts skipped): {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "fp-identity", fallbackMethod = "allLinesFallback")
    public List<LineInfo> fetchAllLines() {
        var resp = identityClient.getAllLines();
        if (resp != null && resp.getData() != null) return resp.getData();
        return Collections.emptyList();
    }

    public List<LineInfo> allLinesFallback(Throwable t) {
        log.warn("CB OPEN — fp-identity fallback (lines skipped): {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "fp-telemetry", fallbackMethod = "productionByShiftFallback")
    public List<ProductionCountResponse> fetchProductionByShift(Long shiftId) {
        var resp = telemetryClient.getProductionByShift(shiftId, 2000, 0);
        if (resp != null && resp.getData() != null && resp.getData().getContent() != null)
            return resp.getData().getContent();
        return Collections.emptyList();
    }

    public List<ProductionCountResponse> productionByShiftFallback(Long shiftId, Throwable t) {
        log.warn("CB OPEN — fp-telemetry fallback (production by shift skipped): {}", t.getMessage());
        return Collections.emptyList();
    }
}

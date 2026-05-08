package org.cts.fp_reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.request.ReportRequest;
import org.cts.fp_reporting.dto.response.*;
import org.cts.fp_reporting.exception.ResourceNotFoundException;
import org.cts.fp_reporting.model.Report;
import org.cts.fp_reporting.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final OEEService oeeService;
    private final ExternalCallService externalCallService;

    public ReportResponse generateReport(ReportRequest request,
                                         Long userId,
                                         String generatedByName) throws Exception {
        Report report = new Report();
        report.setScope(request.getScope());
        report.setGeneratedById(userId);
        report.setGeneratedByName(generatedByName);

        try {
            report.setParametersJson(objectMapper.writeValueAsString(
                    request.getParametersJson() != null ? request.getParametersJson() : Map.of()));
        } catch (Exception e) {
            log.warn("Failed to serialize parametersJson: {}", e.getMessage());
            report.setParametersJson("{}");
        }

        try {
            Map<String, Object> metrics = buildMetrics(request.getParametersJson(), request.getScope());
            report.setMetricsJson(objectMapper.writeValueAsString(metrics));
        } catch (Exception e) {
            log.warn("Failed to build metrics: {}", e.getMessage());
            report.setMetricsJson("{}");
        }

        Report saved = reportRepository.save(report);
        try {
            externalCallService.recordAuditLog(new AuditLogRequest("GENERATE_REPORT", "Report",
                    "Generated report ID: " + saved.getReportId() + ", scope: " + report.getScope()));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toReportResponse(saved);
    }

    public Page<ReportResponse> getAllReports(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return reportRepository.findAll(pageable).map(this::toReportResponse);
        return reportRepository.search(search, pageable).map(this::toReportResponse);
    }

    public ReportResponse getReportById(Long id) {
        return toReportResponse(reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id)));
    }

    private Map<String, Object> buildMetrics(Map<String, Object> parameters, String scope) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (parameters == null || parameters.isEmpty()) return metrics;

        // ── Resolve common parameters ────────────────────────────────────────────
        LocalDate from = parameters.containsKey("from")
                ? LocalDate.parse(parameters.get("from").toString()) : LocalDate.now().minusDays(30);
        // If "to" not provided, use "from" (single-day report)
        LocalDate to   = parameters.containsKey("to")
                ? LocalDate.parse(parameters.get("to").toString())   : from;
        String fromDT  = from.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME);
        String toDT    = to.atTime(23, 59, 59).format(DateTimeFormatter.ISO_DATE_TIME);
        String shiftName = parameters.containsKey("shiftName")
                ? parameters.get("shiftName").toString() : null;

        // ── SHIFT scope ──────────────────────────────────────────────────────────
        if ("SHIFT".equalsIgnoreCase(scope) && shiftName != null) {

            // OEE — by shift name across date range (covers all instances of that shift type)
            try {
                List<OEERecordResponse> oeeList = oeeService.getOEEByShiftNameAndDateRange(shiftName, from, to);
                metrics.put("shiftRecords", oeeList.size());
                if (!oeeList.isEmpty()) {
                    double avgOEE = oeeList.stream()
                            .mapToDouble(r -> r.getOeePct() != null ? r.getOeePct() : 0.0)
                            .average().orElse(0.0);
                    metrics.put("avgOEE", Math.round(avgOEE * 100.0) / 100.0);
                }
                // Derive line IDs from OEE records to fetch production
                List<Long> lineIds = oeeList.stream()
                        .map(OEERecordResponse::getLineId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (!lineIds.isEmpty()) {
                    long totalGood = 0, totalBad = 0;
                    for (Long lid : lineIds) {
                        List<ProductionCountResponse> prod = externalCallService.fetchProduction(lid, fromDT, toDT);
                        totalGood += prod.stream().mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0).sum();
                        totalBad  += prod.stream().mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0).sum();
                    }
                    if (totalGood + totalBad > 0) {
                        metrics.put("totalGood",   totalGood);
                        metrics.put("totalBad",    totalBad);
                        metrics.put("qualityRate", Math.round((double) totalGood / (totalGood + totalBad) * 10000.0) / 100.0);
                    }
                }
            } catch (Exception e) {
                log.warn("OEE/production for SHIFT report: {}", e.getMessage());
            }

            // Downtime — date range only (no shift FK on DowntimeEvent)
            try {
                List<DowntimeEventResponse> list = externalCallService.fetchDowntimes(fromDT, toDT);
                List<DowntimeEventResponse> closed = list.stream()
                        .filter(d -> d.getDurationSec() != null && d.getDurationSec() > 0)
                        .collect(Collectors.toList());
                long totalDowntimeSec = closed.stream().mapToLong(DowntimeEventResponse::getDurationSec).sum();
                metrics.put("downtimeEventCount", list.size());
                metrics.put("totalDowntimeSec",   totalDowntimeSec);
                metrics.put("mttrMinutes", !closed.isEmpty()
                        ? Math.round(totalDowntimeSec / 60.0 / closed.size() * 100.0) / 100.0 : 0.0);
            } catch (Exception e) {
                log.warn("Downtime for SHIFT report: {}", e.getMessage());
            }
        }

        // ── ALL scope (LINE with no lineId — aggregate all lines) ───────────────
        else if ("LINE".equalsIgnoreCase(scope) && !parameters.containsKey("lineId")) {

            // OEE — all lines across date range; also derive line IDs for production
            try {
                List<OEERecordResponse> allOeeList = oeeService.getAllOEEByDateRange(from, to);
                metrics.put("shiftRecords", allOeeList.size());
                if (!allOeeList.isEmpty()) {
                    double avgOEE = allOeeList.stream()
                            .mapToDouble(r -> r.getOeePct() != null ? r.getOeePct() : 0.0)
                            .average().orElse(0.0);
                    metrics.put("avgOEE", Math.round(avgOEE * 100.0) / 100.0);
                }

                // Production — derive line IDs from OEE records, fetch per line via CB
                List<Long> lineIds = allOeeList.stream()
                        .map(OEERecordResponse::getLineId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (!lineIds.isEmpty()) {
                    long totalGood = 0, totalBad = 0;
                    for (Long lid : lineIds) {
                        List<ProductionCountResponse> prod = externalCallService.fetchProduction(lid, fromDT, toDT);
                        totalGood += prod.stream().mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0).sum();
                        totalBad  += prod.stream().mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0).sum();
                    }
                    if (totalGood + totalBad > 0) {
                        metrics.put("totalGood",   totalGood);
                        metrics.put("totalBad",    totalBad);
                        metrics.put("qualityRate", Math.round((double) totalGood / (totalGood + totalBad) * 10000.0) / 100.0);
                    }
                }
            } catch (Exception e) {
                log.warn("OEE/production for ALL scope: {}", e.getMessage());
            }

            // Downtime — all lines, date range
            try {
                List<DowntimeEventResponse> list = externalCallService.fetchDowntimes(fromDT, toDT);
                List<DowntimeEventResponse> closed = list.stream()
                        .filter(d -> d.getDurationSec() != null && d.getDurationSec() > 0)
                        .collect(Collectors.toList());
                long totalDowntimeSec = closed.stream().mapToLong(DowntimeEventResponse::getDurationSec).sum();
                metrics.put("downtimeEventCount", list.size());
                metrics.put("totalDowntimeSec",   totalDowntimeSec);
                metrics.put("mttrMinutes", !closed.isEmpty()
                        ? Math.round(totalDowntimeSec / 60.0 / closed.size() * 100.0) / 100.0 : 0.0);
            } catch (Exception e) {
                log.warn("Downtime for ALL scope: {}", e.getMessage());
            }
        }

        // ── LINE scope ───────────────────────────────────────────────────────────
        else if ("LINE".equalsIgnoreCase(scope)) {
            Object lineIdObj = parameters.get("lineId");
            if (lineIdObj == null) return metrics;
            Long lineId = Long.valueOf(lineIdObj.toString());

            // OEE — by line + optional shiftName + date range
            try {
                List<OEERecordResponse> oeeList = (shiftName != null)
                        ? oeeService.getOEEByLineAndShiftNameAndDateRange(lineId, shiftName, from, to)
                        : oeeService.getOEEByLineAndDateRange(lineId, from, to, PageRequest.of(0, 10000)).getContent();
                metrics.put("shiftRecords", oeeList.size());
                if (!oeeList.isEmpty()) {
                    double avgOEE = oeeList.stream()
                            .mapToDouble(r -> r.getOeePct() != null ? r.getOeePct() : 0.0)
                            .average().orElse(0.0);
                    metrics.put("avgOEE", Math.round(avgOEE * 100.0) / 100.0);
                }
            } catch (Exception e) {
                log.warn("OEE for LINE report: {}", e.getMessage());
            }

            // Downtime — line + date range via CB
            try {
                List<DowntimeEventResponse> list = externalCallService.fetchDowntimesByLine(lineId, fromDT, toDT);
                List<DowntimeEventResponse> closed = list.stream()
                        .filter(d -> d.getDurationSec() != null && d.getDurationSec() > 0)
                        .collect(Collectors.toList());
                long totalDowntimeSec = closed.stream().mapToLong(DowntimeEventResponse::getDurationSec).sum();
                metrics.put("downtimeEventCount", list.size());
                metrics.put("totalDowntimeSec",   totalDowntimeSec);
                metrics.put("mttrMinutes", !closed.isEmpty()
                        ? Math.round(totalDowntimeSec / 60.0 / closed.size() * 100.0) / 100.0 : 0.0);
            } catch (Exception e) {
                log.warn("Downtime for LINE report: {}", e.getMessage());
            }

            // Production — line + date range via CB
            try {
                List<ProductionCountResponse> list = externalCallService.fetchProduction(lineId, fromDT, toDT);
                long totalGood = list.stream().mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0).sum();
                long totalBad  = list.stream().mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0).sum();
                metrics.put("totalGood",   totalGood);
                metrics.put("totalBad",    totalBad);
                metrics.put("qualityRate", (totalGood + totalBad) > 0
                        ? Math.round((double) totalGood / (totalGood + totalBad) * 10000.0) / 100.0 : 0.0);
            } catch (Exception e) {
                log.warn("Production for LINE report: {}", e.getMessage());
            }
        }

        return metrics;
    }

    private ReportResponse toReportResponse(Report r) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Map<String, Object> metricsMap = new LinkedHashMap<>();
        try {
            if (r.getParametersJson() != null && !r.getParametersJson().isBlank())
                parameters = objectMapper.readValue(r.getParametersJson(), new TypeReference<>() {});
            if (r.getMetricsJson() != null && !r.getMetricsJson().isBlank())
                metricsMap = objectMapper.readValue(r.getMetricsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Error deserializing report JSON: {}", e.getMessage());
        }
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .scope(r.getScope())
                .parametersJson(parameters)
                .metricsJson(metricsMap)
                .generatedBy(r.getGeneratedById())
                .generatedByName(r.getGeneratedByName())
                .generatedAt(r.getGeneratedAt())
                .reportUrl(r.getReportUrl())
                .build();
    }
}

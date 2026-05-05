package org.cts.fp_reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.EventsClient;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.TelemetryClient;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final IdentityClient identityClient;
    private final EventsClient eventsClient;
    private final TelemetryClient telemetryClient;
    private final OEEService oeeService;   // direct call — analytics is now in-process

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
            identityClient.recordAuditLog(new AuditLogRequest("GENERATE_REPORT", "Report",
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

        if ("LINE".equalsIgnoreCase(scope)) {
            Object lineIdObj = parameters.get("lineId");
            Object fromObj   = parameters.get("from");
            Object toObj     = parameters.get("to");
            if (lineIdObj == null || fromObj == null || toObj == null) return metrics;

            Long      lineId = Long.valueOf(lineIdObj.toString());
            LocalDate from   = LocalDate.parse(fromObj.toString());
            LocalDate to     = LocalDate.parse(toObj.toString());
            String    fromDT = from.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME);
            String    toDT   = to.atTime(23, 59, 59).format(DateTimeFormatter.ISO_DATE_TIME);

            // OEE — now in-process (analytics merged into this service)
            try {
                Page<OEERecordResponse> page = oeeService.getOEEByLineAndDateRange(
                        lineId, from, to, PageRequest.of(0, 10000));
                if (page != null) {
                    List<OEERecordResponse> list = page.getContent();
                    double avgOEE = list.stream()
                            .mapToDouble(r -> r.getOeePct() != null ? r.getOeePct() : 0.0)
                            .average().orElse(0.0);
                    metrics.put("avgOEE", Math.round(avgOEE * 100.0) / 100.0);
                }
            } catch (Exception e) {
                log.warn("Could not fetch OEE data for report metrics: {}", e.getMessage());
            }

            // Downtime — from fp_events
            try {
                var resp = eventsClient.getDowntimesByLine(lineId, fromDT, toDT);
                if (resp != null && resp.getData() != null) {
                    List<DowntimeEventResponse> list = resp.getData();
                    long totalDowntimeSec = list.stream()
                            .mapToLong(d -> d.getDurationSec() != null ? d.getDurationSec() : 0L).sum();
                    int  count            = list.size();
                    metrics.put("totalDowntimeSec",   totalDowntimeSec);
                    metrics.put("downtimeEventCount",  count);
                    metrics.put("mttrMinutes", count > 0
                            ? Math.round(totalDowntimeSec / 60.0 / count * 100.0) / 100.0 : 0.0);
                }
            } catch (Exception e) {
                log.warn("Could not fetch downtime data for report metrics: {}", e.getMessage());
            }

            // Production — from fp_telemetry
            try {
                var resp = telemetryClient.getProductionByLine(lineId, fromDT, toDT, 10000, 0);
                if (resp != null && resp.getData() != null && resp.getData().getContent() != null) {
                    List<ProductionCountResponse> list = resp.getData().getContent();
                    long totalGood = list.stream().mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0).sum();
                    long totalBad  = list.stream().mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0).sum();
                    double quality = (totalGood + totalBad) > 0
                            ? (double) totalGood / (totalGood + totalBad) * 100 : 0.0;
                    metrics.put("totalGood",   totalGood);
                    metrics.put("totalBad",    totalBad);
                    metrics.put("qualityRate", Math.round(quality * 100.0) / 100.0);
                }
            } catch (Exception e) {
                log.warn("Could not fetch production data for report metrics: {}", e.getMessage());
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

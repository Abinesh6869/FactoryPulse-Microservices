package org.cts.fp_reporting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.dto.request.ReportRequest;
import org.cts.fp_reporting.dto.response.LineInfo;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.service.ReportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors the monolith's ReportWorker exactly.
 * ① Midnight every day — generates one LINE report for each production line (previous day).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportWorker {

    private final ReportService  reportService;
    private final IdentityClient identityClient;

    @Scheduled(cron = "0 0 0 * * *")   // 00:00 every day
    public void generateDailyReports() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<LineInfo> lines;
        try {
            ServiceApiResponse<List<LineInfo>> resp = identityClient.getAllLines();
            if (resp == null || resp.getData() == null) {
                log.warn("ReportWorker: no lines returned from fp_identity");
                return;
            }
            lines = resp.getData();
        } catch (Exception e) {
            log.error("ReportWorker: could not fetch lines — {}", e.getMessage());
            return;
        }

        for (LineInfo line : lines) {
            try {
                ReportRequest request = new ReportRequest();
                request.setScope("LINE");
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("lineId", line.getLineId());
                params.put("from", yesterday.toString());
                params.put("to", yesterday.toString());
                request.setParametersJson(params);

                // System-generated report — userId 0L = system user
                reportService.generateReport(request, 0L, "System");
                log.info("ReportWorker: daily report generated for Line '{}'", line.getName());
            } catch (Exception e) {
                log.error("ReportWorker: failed for line '{}' — {}", line.getName(), e.getMessage());
            }
        }
    }
}

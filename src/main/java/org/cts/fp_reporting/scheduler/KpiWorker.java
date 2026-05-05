package org.cts.fp_reporting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.EventsClient;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.MaintenanceClient;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.response.*;
import org.cts.fp_reporting.model.Kpi;
import org.cts.fp_reporting.model.OEERecord;
import org.cts.fp_reporting.repository.KpiRepository;
import org.cts.fp_reporting.repository.OEERecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Runs at 00:05 on the 1st of every month.
 * Calculates 5 KPIs for the previous calendar month and upserts them into the kpi table.
 *
 * 1. Average OEE %           — from OEERecordRepository (in-process, analytics merged here)
 * 2. Total Downtime Hours     — from fp_events via EventsClient
 * 3. Quality Rate %           — from fp_telemetry via TelemetryClient (per-line, then aggregate)
 * 4. MTTR (minutes)          — from fp_events (same downtime list as #2)
 * 5. Work Order Completion %  — from fp_maintenance via MaintenanceClient
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KpiWorker {

    private final OEERecordRepository oeeRecordRepository;
    private final KpiRepository       kpiRepository;
    private final EventsClient        eventsClient;
    private final TelemetryClient     telemetryClient;
    private final MaintenanceClient   maintenanceClient;
    private final IdentityClient      identityClient;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Scheduled(cron = "0 5 0 1 * *")   // 00:05 on 1st of every month
    public void calculateMonthlyKPIs() {
        LocalDate today      = LocalDate.now();
        LocalDate monthStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate monthEnd   = today.withDayOfMonth(1).minusDays(1);

        String fromStr = monthStart.atStartOfDay().format(ISO);
        String toStr   = monthEnd.atTime(23, 59, 59).format(ISO);
        String period  = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());

        log.info("KpiWorker: calculating KPIs for period {} ({} → {})", period, fromStr, toStr);

        // ── 1. Average OEE % (in-process — analytics merged into this service) ─
        double avgOEE = 0.0;
        try {
            List<OEERecord> oeeList = oeeRecordRepository.findByDateBetween(monthStart, monthEnd);
            if (!oeeList.isEmpty()) {
                avgOEE = oeeList.stream()
                        .mapToDouble(r -> r.getOeePct() != null ? r.getOeePct() : 0.0)
                        .average().orElse(0.0);
                avgOEE = Math.round(avgOEE * 100.0) / 100.0;
            }
        } catch (Exception e) {
            log.warn("KpiWorker: OEE fetch failed — {}", e.getMessage());
        }

        // ── 2 & 4. Downtime data (Total Hours + MTTR) ────────────────────────
        double totalDowntimeHours = 0.0;
        double mttrMinutes        = 0.0;
        try {
            ServiceApiResponse<List<DowntimeEventResponse>> dtResp =
                    eventsClient.getAllDowntimesByDateRange(fromStr, toStr);
            if (dtResp != null && dtResp.getData() != null) {
                List<DowntimeEventResponse> dtList = dtResp.getData();
                long totalSec = dtList.stream()
                        .mapToLong(d -> d.getDurationSec() != null ? d.getDurationSec() : 0L).sum();
                int  count    = dtList.size();
                totalDowntimeHours = Math.round(totalSec / 3600.0 * 100.0) / 100.0;
                mttrMinutes        = count > 0
                        ? Math.round(totalSec / 60.0 / count * 100.0) / 100.0 : 0.0;
            }
        } catch (Exception e) {
            log.warn("KpiWorker: downtime fetch failed — {}", e.getMessage());
        }

        // ── 3. Quality Rate % ────────────────────────────────────────────────
        double qualityRate = 0.0;
        try {
            ServiceApiResponse<List<LineInfo>> lineResp = identityClient.getAllLines();
            if (lineResp != null && lineResp.getData() != null) {
                long totalGood = 0, totalAll = 0;
                for (LineInfo line : lineResp.getData()) {
                    try {
                        ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> prodResp =
                                telemetryClient.getProductionByLine(line.getLineId(), fromStr, toStr, 10000, 0);
                        if (prodResp != null && prodResp.getData() != null
                                && prodResp.getData().getContent() != null) {
                            for (ProductionCountResponse p : prodResp.getData().getContent()) {
                                long g = p.getGoodCount()   != null ? p.getGoodCount()   : 0;
                                long b = p.getRejectCount() != null ? p.getRejectCount() : 0;
                                totalGood += g;
                                totalAll  += g + b;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("KpiWorker: production fetch failed for line {} — {}", line.getLineId(), e.getMessage());
                    }
                }
                qualityRate = totalAll > 0
                        ? Math.round((double) totalGood / totalAll * 100.0 * 100.0) / 100.0 : 0.0;
            }
        } catch (Exception e) {
            log.warn("KpiWorker: quality rate calc failed — {}", e.getMessage());
        }

        // ── 5. Work Order Completion Rate % ──────────────────────────────────
        double woCompletionRate = 0.0;
        try {
            ServiceApiResponse<List<WorkOrderResponse>> woResp =
                    maintenanceClient.getWorkOrdersByDateRange(fromStr, toStr);
            if (woResp != null && woResp.getData() != null) {
                List<WorkOrderResponse> woList = woResp.getData();
                long total     = woList.size();
                long completed = woList.stream()
                        .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus())).count();
                woCompletionRate = total > 0
                        ? Math.round((double) completed / total * 100.0 * 100.0) / 100.0 : 0.0;
            }
        } catch (Exception e) {
            log.warn("KpiWorker: work order fetch failed — {}", e.getMessage());
        }

        // ── Upsert all 5 KPIs ────────────────────────────────────────────────
        upsert("Average OEE",                   "Overall Equipment Effectiveness (%)",          85.0,  avgOEE,           period);
        upsert("Total Downtime Hours",           "Total unplanned downtime hours in the month",   20.0,  totalDowntimeHours, period);
        upsert("Quality Rate",                   "Good units / total units produced (%)",         98.0,  qualityRate,      period);
        upsert("MTTR",                           "Mean Time To Repair (minutes)",                 30.0,  mttrMinutes,      period);
        upsert("Work Order Completion Rate",     "Completed work orders / total created (%)",     90.0,  woCompletionRate, period);

        log.info("KpiWorker: KPIs saved — OEE={}%, Downtime={}h, Quality={}%, MTTR={}min, WO={}%",
                avgOEE, totalDowntimeHours, qualityRate, mttrMinutes, woCompletionRate);
    }

    private void upsert(String name, String definition, double target, double value, String period) {
        try {
            Kpi kpi = kpiRepository.findByName(name).orElseGet(Kpi::new);
            kpi.setName(name);
            kpi.setDefinition(definition);
            kpi.setTarget(target);
            kpi.setCurrentValue(value);
            kpi.setReportingPeriod(period);
            kpiRepository.save(kpi);
        } catch (Exception e) {
            log.warn("KpiWorker: upsert failed for '{}' — {}", name, e.getMessage());
        }
    }
}

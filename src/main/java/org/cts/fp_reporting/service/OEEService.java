package org.cts.fp_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.EventsClient;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.request.OEERecordRequest;
import org.cts.fp_reporting.dto.response.*;
import org.cts.fp_reporting.exception.ResourceNotFoundException;
import org.cts.fp_reporting.model.OEERecord;
import org.cts.fp_reporting.repository.OEERecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OEEService {

    private final OEERecordRepository oeeRecordRepository;
    private final IdentityClient identityClient;
    private final EventsClient eventsClient;
    private final TelemetryClient telemetryClient;

    public OEERecordResponse calculateAndSaveOEE(Long lineId, Long shiftId, OEERecordRequest body) {

        // 1. Fetch shift info from fp_identity
        ShiftInfo shift = null;
        try {
            ServiceApiResponse<ShiftInfo> sr = identityClient.getShiftById(shiftId);
            if (sr != null) shift = sr.getData();
        } catch (Exception e) { log.warn("Could not fetch shift {}: {}", shiftId, e.getMessage()); }

        // 2. Fetch line info from fp_identity
        LineInfo line = null;
        try {
            ServiceApiResponse<LineInfo> lr = identityClient.getLineById(lineId);
            if (lr != null) line = lr.getData();
        } catch (Exception e) { log.warn("Could not fetch line {}: {}", lineId, e.getMessage()); }

        // 3. Resolve names, date, planned seconds
        String lineName  = line  != null && line.getName()  != null ? line.getName()  : "Unknown Line";
        String shiftName = shift != null && shift.getName() != null ? shift.getName() : "Unknown Shift";
        LocalDate shiftDate = shift != null && shift.getDate() != null ? shift.getDate() : LocalDate.now();

        long plannedSec = 28800L; // default 8 hrs
        if (shift != null && shift.getStartTime() != null && shift.getEndTime() != null) {
            long s = Duration.between(shift.getStartTime(), shift.getEndTime()).getSeconds();
            if (s > 0) plannedSec = s;
        }

        // 4. Downtime from fp_events
        long totalDowntimeSec = 0L;
        if (shift != null && shift.getStartTime() != null && shift.getEndTime() != null) {
            try {
                String from = shiftDate.atTime(shift.getStartTime()).toString();
                String to   = shiftDate.atTime(shift.getEndTime()).toString();
                ServiceApiResponse<List<DowntimeEventResponse>> dr = eventsClient.getDowntimesByLine(lineId, from, to);
                if (dr != null && dr.getData() != null) {
                    totalDowntimeSec = dr.getData().stream()
                            .mapToLong(d -> d.getDurationSec() != null ? d.getDurationSec() : 0L)
                            .sum();
                }
            } catch (Exception e) { log.warn("Could not fetch downtimes: {}", e.getMessage()); }
        }

        // 5. Production counts from fp_telemetry
        long totalGood = 0L, totalReject = 0L;
        try {
            ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> pr =
                    telemetryClient.getProductionByShift(shiftId, 10000, 0);
            if (pr != null && pr.getData() != null && pr.getData().getContent() != null) {
                totalGood   = pr.getData().getContent().stream()
                        .mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0).sum();
                totalReject = pr.getData().getContent().stream()
                        .mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0).sum();
            }
        } catch (Exception e) { log.warn("Could not fetch production counts: {}", e.getMessage()); }

        // 6. OEE calculation (same formula as monolith)
        double availability = Math.max(0.0, Math.min(1.0,
                (plannedSec - totalDowntimeSec) / (double) plannedSec));

        long totalProduced    = totalGood + totalReject;
        long productionTickSec = 20L;
        long goodCountPerTick  = 20L;
        long idealCount        = (plannedSec / productionTickSec) * goodCountPerTick;
        double performance     = idealCount > 0 ? Math.min(1.0, totalProduced / (double) idealCount) : 0.0;
        double quality         = totalProduced > 0 ? totalGood / (double) totalProduced : 1.0;
        double oee             = availability * performance * quality;

        // 7. Save or update
        OEERecord record = oeeRecordRepository.findByLineIdAndShiftId(lineId, shiftId)
                .orElse(new OEERecord());
        record.setLineId(lineId);
        record.setLineName(lineName);
        record.setShiftId(shiftId);
        record.setShiftName(shiftName);
        record.setDate(shiftDate);
        record.setAvailabilityPct(round(availability * 100));
        record.setPerformancePct(round(performance * 100));
        record.setQualityPct(round(quality * 100));
        record.setOeePct(round(oee * 100));

        OEERecord saved = oeeRecordRepository.save(record);
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CALCULATE_OEE", "OEERecord",
                    "Calculated OEE for line ID: " + lineId + ", shift ID: " + shiftId +
                    ", OEE: " + saved.getOeePct() + "%"));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }

        return toResponse(saved);
    }

    public Page<OEERecordResponse> getOEEByLine(Long lineId, Pageable pageable) {
        return oeeRecordRepository.findByLineIdOrderByDateDesc(lineId, pageable).map(this::toResponse);
    }

    public Page<OEERecordResponse> getOEEByLineAndDateRange(Long lineId, LocalDate from, LocalDate to, Pageable pageable) {
        return oeeRecordRepository.findByLineIdAndDateBetweenOrderByDateDesc(lineId, from, to, pageable).map(this::toResponse);
    }

    public Page<OEERecordResponse> getOEEByShift(Long shiftId, Pageable pageable) {
        return oeeRecordRepository.findByShiftId(shiftId, pageable).map(this::toResponse);
    }

    public OEERecordResponse getLatestOEEByLine(Long lineId) {
        return oeeRecordRepository.findTop1ByLineIdOrderByDateDesc(lineId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No OEE record found for line: " + lineId));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private OEERecordResponse toResponse(OEERecord r) {
        return OEERecordResponse.builder()
                .oeeId(r.getOeeId())
                .lineId(r.getLineId())
                .lineName(r.getLineName())
                .shiftId(r.getShiftId())
                .shiftName(r.getShiftName())
                .date(r.getDate())
                .availabilityPct(r.getAvailabilityPct())
                .performancePct(r.getPerformancePct())
                .qualityPct(r.getQualityPct())
                .oeePct(r.getOeePct())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

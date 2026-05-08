package org.cts.fp_reporting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.dto.request.OEERecordRequest;
import org.cts.fp_reporting.dto.response.*;
import org.cts.fp_reporting.service.ExternalCallService;
import org.cts.fp_reporting.service.OEEService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OEEScheduler {

    private final ExternalCallService externalCallService;
    private final OEEService oeeService;

    @Scheduled(fixedDelayString = "${oee.auto-calculate-interval-ms:60000}")
    public void autoCalculateOEE() {
        try {
            LocalTime now  = LocalTime.now();
            String today   = LocalDate.now().toString();

            List<ShiftInfo> shifts = externalCallService.fetchShiftsByDate(today);
            if (shifts.isEmpty()) return;

            List<ShiftInfo> activeShifts = shifts.stream()
                    .filter(s -> s.getStartTime() != null && s.getEndTime() != null
                            && !now.isBefore(s.getStartTime())
                            && !now.isAfter(s.getEndTime()))
                    .toList();

            if (activeShifts.isEmpty()) return;

            List<LineInfo> allLines = externalCallService.fetchAllLines();
            if (allLines.isEmpty()) return;

            for (ShiftInfo shift : activeShifts) {
                List<LineInfo> shiftLines = allLines.stream()
                        .filter(l -> shift.getPlantId() != null && shift.getPlantId().equals(l.getPlantId()))
                        .toList();
                for (LineInfo line : shiftLines) {
                    try {
                        calculateOEEFor(line, shift);
                    } catch (Exception e) {
                        log.warn("OEE auto-calc failed for line {} shift {}: {}",
                                line.getLineId(), shift.getShiftId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("OEE scheduler error: {}", e.getMessage());
        }
    }

    private void calculateOEEFor(LineInfo line, ShiftInfo shift) {
        long totalGood = 0, totalReject = 0;
        try {
            List<ProductionCountResponse> prod = externalCallService.fetchProductionByShift(shift.getShiftId());
            for (ProductionCountResponse p : prod) {
                if (line.getLineId().equals(p.getLineId())) {
                    totalGood   += p.getGoodCount()   != null ? p.getGoodCount()   : 0;
                    totalReject += p.getRejectCount() != null ? p.getRejectCount() : 0;
                }
            }
        } catch (Exception e) {
            log.warn("OEE scheduler: failed to fetch production for shift {} — {}", shift.getShiftId(), e.getMessage());
        }

        long plannedSec = 28800L;
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            plannedSec = ChronoUnit.SECONDS.between(shift.getStartTime(), shift.getEndTime());
        }

        OEERecordRequest req = new OEERecordRequest();
        req.setLineName(line.getName());
        req.setShiftName(shift.getName());
        req.setShiftDate(shift.getDate() != null ? shift.getDate() : LocalDate.now());
        req.setPlannedSec(plannedSec);
        req.setTotalDowntimeSec(0L);
        req.setTotalGoodCount(totalGood);
        req.setTotalRejectCount(totalReject);
        req.setProductionTickSec(20L);
        req.setGoodCountPerTick(20L);

        oeeService.calculateAndSaveOEE(line.getLineId(), shift.getShiftId(), req);
        log.info("OEE auto-calculated — line '{}' ({}), shift '{}' ({}): good={}, reject={}, planned={}s",
                line.getName(), line.getLineId(), shift.getName(), shift.getShiftId(),
                totalGood, totalReject, plannedSec);
    }
}

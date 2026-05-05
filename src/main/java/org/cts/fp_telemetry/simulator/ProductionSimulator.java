package org.cts.fp_telemetry.simulator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.response.IdentityApiResponse;
import org.cts.fp_telemetry.dto.response.MachineInfo;
import org.cts.fp_telemetry.dto.response.ShiftInfo;
import org.cts.fp_telemetry.model.ProductionCount;
import org.cts.fp_telemetry.repository.ProductionCountRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductionSimulator {

    private final IdentityClient identityClient;
    private final ProductionCountRepository productionCountRepository;
    private final SimulationConfig config;
    private final Random random = new Random();

    public void recordProductionCount(Long lineId, String lineName, List<MachineInfo> machines) {
        int totalMachines = machines.size();
        int activeMachines = (int) machines.stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .count();

        // All machines down — no production
        if (activeMachines == 0) {
            log.info("Simulator: line '{}' — all {} machine(s) are down, skipping production tick", lineName, totalMachines);
            return;
        }

        ShiftInfo activeShift = findActiveShift();
        if (activeShift == null) {
            log.info("Simulator: line '{}' — no active shift right now, skipping production tick", lineName);
            return;
        }

        // Reduce production proportionally to active machines
        double capacityRatio = (double) activeMachines / totalMachines;
        int good   = (int) Math.round(config.getGoodCountPerTick() * capacityRatio);
        int reject = Math.max((int) Math.round(good * config.getRejectRate() + (random.nextDouble() * 2 - 1)), 0);

        ProductionCount count = new ProductionCount();
        count.setLineId(lineId);
        count.setLineName(lineName);
        count.setShiftId(activeShift.getShiftId());
        count.setShiftName(activeShift.getName());
        count.setGoodCount(good);
        count.setRejectCount(reject);

        productionCountRepository.save(count);
        log.info("Simulator: recorded production for line '{}' — good={}, reject={}, shiftId={}",
                lineName, good, reject, activeShift.getShiftId());
    }

    private ShiftInfo findActiveShift() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        try {
            IdentityApiResponse<List<ShiftInfo>> response =
                    identityClient.getShiftsByDate(today.toString());
            List<ShiftInfo> shifts = (response != null && response.getData() != null)
                    ? response.getData() : List.of();
            return shifts.stream()
                    .filter(s -> s.getStartTime() != null && s.getEndTime() != null
                            && !now.isBefore(s.getStartTime())
                            && !now.isAfter(s.getEndTime()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Simulator: failed to fetch shifts from identity service — {}", e.getMessage());
            return null;
        }
    }
}

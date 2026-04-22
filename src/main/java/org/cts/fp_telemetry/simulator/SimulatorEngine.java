package org.cts.fp_telemetry.simulator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.response.IdentityApiResponse;
import org.cts.fp_telemetry.dto.response.MachineInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SimulatorEngine {

    private final IdentityClient identityClient;
    private final MachineSimulator machineSimulator;
    private final ProductionSimulator productionSimulator;
    private final SimulationConfig config;

    @PostConstruct
    public void init() {
        log.info("SimulatorEngine initialized. Enabled: {}", config.isEnabled());
    }

    // Telemetry tick — every 5 seconds
    @Scheduled(fixedDelayString = "${simulator.telemetry-tick-ms:5000}")
    public void telemetryTick() {
        if (!config.isEnabled()) return;
        List<MachineInfo> machines = fetchMachines();
        if (machines.isEmpty()) {
            log.warn("Simulator telemetryTick: no machines found in fp_identity — check that machines are configured in the DB");
            return;
        }
        log.info("Simulator telemetryTick: processing {} machine(s)", machines.size());
        for (MachineInfo machine : machines) {
            boolean isRunning = "ACTIVE".equalsIgnoreCase(machine.getStatus());
            machineSimulator.emitTelemetry(machine, isRunning);
        }
    }

    // Production tick — every 20 seconds
    @Scheduled(fixedDelayString = "${simulator.production-tick-ms:20000}")
    public void productionTick() {
        if (!config.isEnabled()) return;
        List<MachineInfo> machines = fetchMachines();
        if (machines.isEmpty()) {
            log.warn("Simulator productionTick: no machines found in fp_identity — check that machines are configured in the DB");
            return;
        }
        machines.stream()
                .filter(m -> m.getLineId() != null)
                .collect(Collectors.groupingBy(MachineInfo::getLineId))
                .forEach((lineId, lineMachines) -> {
                    String lineName = lineMachines.get(0).getLineName();
                    productionSimulator.recordProductionCount(lineId, lineName, lineMachines);
                });
    }

    private List<MachineInfo> fetchMachines() {
        try {
            IdentityApiResponse<List<MachineInfo>> response = identityClient.getAllMachines();
            return (response != null && response.getData() != null) ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Simulator: failed to fetch machines from identity service — {}", e.getMessage());
            return List.of();
        }
    }
}

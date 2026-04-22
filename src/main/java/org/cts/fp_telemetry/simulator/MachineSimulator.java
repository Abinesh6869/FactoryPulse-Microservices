package org.cts.fp_telemetry.simulator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.response.IdentityApiResponse;
import org.cts.fp_telemetry.dto.response.MachineInfo;
import org.cts.fp_telemetry.dto.response.TelemetryPointInfo;
import org.cts.fp_telemetry.model.TelemetryEvent;
import org.cts.fp_telemetry.repository.TelemetryEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class MachineSimulator {

    private final IdentityClient identityClient;
    private final TelemetryEventRepository telemetryEventRepository;
    private final Random random = new Random();

    public void emitTelemetry(MachineInfo machine, boolean isRunning) {
        List<TelemetryPointInfo> points;
        try {
            IdentityApiResponse<List<TelemetryPointInfo>> response =
                    identityClient.getTelemetryPointsByMachine(machine.getMachineId());
            points = (response != null && response.getData() != null) ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Simulator: failed to fetch telemetry points for machine {} — {}", machine.getMachineId(), e.getMessage());
            return;
        }
        if (points.isEmpty()) {
            log.warn("Simulator: machine '{}' ({}) has no telemetry points configured — skipping",
                    machine.getName(), machine.getMachineId());
            return;
        }

        for (TelemetryPointInfo tp : points) {
            String value = generateValue(tp.getName(), isRunning);
            TelemetryEvent event = new TelemetryEvent();
            event.setPointId(tp.getPointId());
            event.setPointName(tp.getName());
            event.setMachineId(machine.getMachineId());
            event.setMachineName(machine.getName());
            event.setValue(value);
            event.setSource("SIMULATOR");
            event.setStatus("OK");
            telemetryEventRepository.save(event);
        }
        log.info("Simulator: emitted {} telemetry event(s) for machine '{}' (running={})",
                points.size(), machine.getName(), isRunning);
    }

    private String generateValue(String pointName, boolean isRunning) {
        return switch (pointName.toLowerCase()) {
            case "run_status"  -> isRunning ? "true" : "false";
            case "temperature" -> isRunning ? String.valueOf(60 + random.nextInt(40)) : "stopped";
            case "speed"       -> isRunning ? String.valueOf(800 + random.nextInt(400)) : "0";
            case "vibration"   -> isRunning ? String.format("%.2f", random.nextDouble() * 5) : "stopped";
            case "pressure"    -> isRunning ? String.format("%.1f", 4.0 + random.nextDouble() * 2) : "stopped";
            default            -> isRunning ? "1" : "0";
        };
    }
}

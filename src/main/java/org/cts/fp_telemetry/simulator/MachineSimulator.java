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

    public void emitTelemetry(MachineInfo machine) {
        // Stop emission if the parent line or plant is not ACTIVE
        if (machine.getLineStatus() != null && !"ACTIVE".equalsIgnoreCase(machine.getLineStatus())) return;
        if (machine.getPlantStatus() != null && !"ACTIVE".equalsIgnoreCase(machine.getPlantStatus())) return;
        boolean active = "ACTIVE".equalsIgnoreCase(machine.getStatus());
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
            String value = generateValue(tp.getName(), active);
            TelemetryEvent event = new TelemetryEvent();
            event.setPointId(tp.getPointId());
            event.setPointName(tp.getName());
            event.setMachineId(machine.getMachineId());
            event.setMachineName(machine.getName());
            event.setLineId(machine.getLineId());
            event.setLineName(machine.getLineName());
            event.setValue(value);
            event.setUnit(tp.getUnit());
            event.setSource("SIMULATOR");
            event.setStatus("OK");
            telemetryEventRepository.save(event);
        }
        log.info("Simulator: emitted {} telemetry event(s) for machine '{}'", points.size(), machine.getName());
    }

    private String generateValue(String pointName, boolean active) {
        if (!active) return "run_status".equalsIgnoreCase(pointName) ? "false" : "0";
        return switch (pointName.toLowerCase()) {
            case "run_status"  -> "true";
            // mean 75°C, stddev 2 — stays in realistic band e.g. 70–80
            case "temperature" -> String.valueOf((int) Math.round(clamp(gauss(75, 2), 60, 100)));
            // mean 950 RPM, stddev 15
            case "speed"       -> String.valueOf((int) Math.round(clamp(gauss(950, 15), 800, 1200)));
            // mean 1.5 mm/s, stddev 0.15
            case "vibration"   -> String.format("%.2f", clamp(gauss(1.5, 0.15), 0.0, 5.0));
            // mean 5.0 bar, stddev 0.15
            case "pressure"    -> String.format("%.1f", clamp(gauss(5.0, 0.15), 3.0, 8.0));
            default            -> "1";
        };
    }

    private double gauss(double mean, double stddev) {
        return mean + random.nextGaussian() * stddev;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

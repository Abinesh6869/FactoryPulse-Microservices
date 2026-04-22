package org.cts.fp_telemetry.simulator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "simulator")
public class SimulationConfig {
    private boolean enabled = true;
    private int telemetryTickMs = 5000;
    private int productionTickMs = 20000;
    private int goodCountPerTick = 20;
    private double rejectRate = 0.03;
}

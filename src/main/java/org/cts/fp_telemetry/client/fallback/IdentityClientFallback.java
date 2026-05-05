package org.cts.fp_telemetry.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.request.AuditLogRequest;
import org.cts.fp_telemetry.dto.response.IdentityApiResponse;
import org.cts.fp_telemetry.dto.response.LineInfo;
import org.cts.fp_telemetry.dto.response.MachineInfo;
import org.cts.fp_telemetry.dto.response.ShiftInfo;
import org.cts.fp_telemetry.dto.response.TelemetryPointInfo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class IdentityClientFallback implements IdentityClient {

    private static final String MSG = "fp-identity is unavailable — using fallback";

    @Override
    public void recordAuditLog(AuditLogRequest request) {
        log.warn("FALLBACK: recordAuditLog skipped — {}", MSG);
    }

    @Override
    public IdentityApiResponse<List<MachineInfo>> getAllMachines() {
        log.warn("FALLBACK: getAllMachines — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<MachineInfo> getMachineById(Long id) {
        log.warn("FALLBACK: getMachineById({}) — {}", id, MSG);
        MachineInfo m = new MachineInfo();
        m.setName("Unknown Machine");
        return fallback(m);
    }

    @Override
    public IdentityApiResponse<List<TelemetryPointInfo>> getTelemetryPointsByMachine(Long machineId) {
        log.warn("FALLBACK: getTelemetryPointsByMachine({}) — {}", machineId, MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<List<ShiftInfo>> getShiftsByDate(String date) {
        log.warn("FALLBACK: getShiftsByDate({}) — {}", date, MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<LineInfo> getLineById(Long id) {
        log.warn("FALLBACK: getLineById({}) — {}", id, MSG);
        return fallback(null);
    }

    @Override
    public IdentityApiResponse<ShiftInfo> getShiftById(Long id) {
        log.warn("FALLBACK: getShiftById({}) — {}", id, MSG);
        return fallback(null);
    }

    @Override
    public IdentityApiResponse<TelemetryPointInfo> getPointById(Long id) {
        log.warn("FALLBACK: getPointById({}) — {}", id, MSG);
        return fallback(null);
    }

    private <T> IdentityApiResponse<T> fallback(T data) {
        IdentityApiResponse<T> resp = new IdentityApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(data);
        return resp;
    }
}

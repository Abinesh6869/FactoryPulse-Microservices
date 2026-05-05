package org.cts.fp_events.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.response.AlertRuleInfo;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.LineInfo;
import org.cts.fp_events.dto.response.MachineInfo;
import org.cts.fp_events.dto.response.RootCauseInfo;
import org.cts.fp_events.dto.response.ShiftAllocationResponse;
import org.cts.fp_events.dto.response.UserInfo;
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
    public IdentityApiResponse<LineInfo> getLineById(Long id) {
        log.warn("FALLBACK: getLineById({}) — {}", id, MSG);
        return fallback(null);
    }

    @Override
    public IdentityApiResponse<MachineInfo> getMachineById(Long id) {
        log.warn("FALLBACK: getMachineById({}) — {}", id, MSG);
        MachineInfo m = new MachineInfo();
        m.setName("Unknown Machine");
        return fallback(m);
    }

    @Override
    public IdentityApiResponse<MachineInfo> updateMachineStatus(Long id, String status) {
        log.warn("FALLBACK: updateMachineStatus({}, {}) — {}", id, status, MSG);
        return fallback(null);
    }

    @Override
    public IdentityApiResponse<List<ShiftAllocationResponse>> getAllocations(
            Long shiftId, String role, Long userId, String search) {
        log.warn("FALLBACK: getAllocations — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<List<ShiftAllocationResponse>> getOnDuty(String role) {
        log.warn("FALLBACK: getOnDuty — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<List<ShiftAllocationResponse>> getAllocationsAtTime(String dateTime) {
        log.warn("FALLBACK: getAllocationsAtTime — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<List<AlertRuleInfo>> getActiveAlertRules(boolean active) {
        log.warn("FALLBACK: getActiveAlertRules — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<List<UserInfo>> getUsersByRole(String role) {
        log.warn("FALLBACK: getUsersByRole({}) — {}", role, MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public IdentityApiResponse<UserInfo> getUserById(Long id) {
        log.warn("FALLBACK: getUserById({}) — {}", id, MSG);
        return fallback(null);
    }

    @Override
    public IdentityApiResponse<RootCauseInfo> getRootCauseById(Long id) {
        log.warn("FALLBACK: getRootCauseById({}) — {}", id, MSG);
        return fallback(null);
    }

    private <T> IdentityApiResponse<T> fallback(T data) {
        return new IdentityApiResponse<>(false, MSG, data);
    }
}

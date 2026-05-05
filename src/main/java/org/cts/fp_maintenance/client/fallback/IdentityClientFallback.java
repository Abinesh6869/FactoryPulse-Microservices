package org.cts.fp_maintenance.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_maintenance.client.IdentityClient;
import org.cts.fp_maintenance.dto.request.AuditLogRequest;
import org.cts.fp_maintenance.dto.response.IdentityApiResponse;
import org.cts.fp_maintenance.dto.response.MachineInfo;
import org.cts.fp_maintenance.dto.response.UserInfo;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IdentityClientFallback implements IdentityClient {

    private static final String MSG = "fp-identity is unavailable — using fallback";

    @Override
    public void recordAuditLog(AuditLogRequest request) {
        log.warn("FALLBACK: recordAuditLog skipped — {}", MSG);
    }

    @Override
    public IdentityApiResponse<MachineInfo> getMachineById(Long id) {
        log.warn("FALLBACK: getMachineById({}) — {}", id, MSG);
        IdentityApiResponse<MachineInfo> resp = new IdentityApiResponse<>();
        MachineInfo m = new MachineInfo();
        m.setName("Unknown Machine");
        resp.setData(m);
        resp.setMessage(MSG);
        return resp;
    }

    @Override
    public IdentityApiResponse<UserInfo> getUserById(Long id) {
        log.warn("FALLBACK: getUserById({}) — {}", id, MSG);
        IdentityApiResponse<UserInfo> resp = new IdentityApiResponse<>();
        resp.setMessage(MSG);
        return resp;
    }

    @Override
    public IdentityApiResponse<MachineInfo> updateMachineStatus(Long id, String status) {
        log.warn("FALLBACK: updateMachineStatus({}, {}) — {}", id, status, MSG);
        IdentityApiResponse<MachineInfo> resp = new IdentityApiResponse<>();
        resp.setMessage(MSG);
        return resp;
    }
}

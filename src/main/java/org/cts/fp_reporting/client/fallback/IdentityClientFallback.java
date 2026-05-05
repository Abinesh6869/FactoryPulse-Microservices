package org.cts.fp_reporting.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.response.LineInfo;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ShiftInfo;
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
    public ServiceApiResponse<List<LineInfo>> getAllLines() {
        log.warn("FALLBACK: getAllLines — {}", MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public ServiceApiResponse<LineInfo> getLineById(Long id) {
        log.warn("FALLBACK: getLineById({}) — {}", id, MSG);
        return fallback(null);
    }

    @Override
    public ServiceApiResponse<List<ShiftInfo>> getShiftsByDate(String date) {
        log.warn("FALLBACK: getShiftsByDate({}) — {}", date, MSG);
        return fallback(Collections.emptyList());
    }

    @Override
    public ServiceApiResponse<ShiftInfo> getShiftById(Long id) {
        log.warn("FALLBACK: getShiftById({}) — {}", id, MSG);
        return fallback(null);
    }

    private <T> ServiceApiResponse<T> fallback(T data) {
        ServiceApiResponse<T> resp = new ServiceApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(data);
        return resp;
    }
}

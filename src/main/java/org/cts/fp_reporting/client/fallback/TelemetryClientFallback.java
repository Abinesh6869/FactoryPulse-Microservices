package org.cts.fp_reporting.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.response.ProductionCountResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ServicePageResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class TelemetryClientFallback implements TelemetryClient {

    private static final String MSG = "fp-telemetry is unavailable — using fallback";

    @Override
    public ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> getProductionByLine(
            Long lineId, String from, String to, int size, int page) {
        log.warn("FALLBACK: getProductionByLine(lineId={}) — {}", lineId, MSG);
        return fallback();
    }

    @Override
    public ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> getProductionByShift(
            Long shiftId, int size, int page) {
        log.warn("FALLBACK: getProductionByShift(shiftId={}) — {}", shiftId, MSG);
        return fallback();
    }

    @Override
    public ServiceApiResponse<ProductionCountResponse> getProductionCountById(Long countId) {
        log.warn("FALLBACK: getProductionCountById(countId={}) — {}", countId, MSG);
        ServiceApiResponse<ProductionCountResponse> resp = new ServiceApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(null);
        return resp;
    }

    private ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> fallback() {
        ServicePageResponse<ProductionCountResponse> emptyPage = new ServicePageResponse<>();
        emptyPage.setContent(Collections.emptyList());
        ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> resp = new ServiceApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(emptyPage);
        return resp;
    }
}

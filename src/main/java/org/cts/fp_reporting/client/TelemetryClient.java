package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.response.ProductionCountResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ServicePageResponse;
import org.cts.fp_reporting.client.fallback.TelemetryClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "fp-telemetry", fallback = TelemetryClientFallback.class)
public interface TelemetryClient {

    // GET /api/telemetry/production?lineId=X&from=Y&to=Z&size=N&page=0
    @GetMapping("/api/telemetry/production")
    ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> getProductionByLine(
            @RequestParam Long lineId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int size,
            @RequestParam int page);

    // GET /api/telemetry/production?shiftId=X&size=N&page=0
    @GetMapping("/api/telemetry/production")
    ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> getProductionByShift(
            @RequestParam Long shiftId,
            @RequestParam int size,
            @RequestParam int page);
}

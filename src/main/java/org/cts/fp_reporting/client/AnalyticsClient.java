package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.response.OEERecordResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ServicePageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "analytics-client", url = "${fp.analytics-base-url:http://localhost:9095}")
public interface AnalyticsClient {

    // GET /api/oee?lineId=X&from=Y&to=Z&size=N&page=0
    @GetMapping("/api/oee")
    ServiceApiResponse<ServicePageResponse<OEERecordResponse>> getOEEByLineAndDateRange(
            @RequestParam Long lineId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int size,
            @RequestParam int page);
}

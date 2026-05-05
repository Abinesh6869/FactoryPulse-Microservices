package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.response.DowntimeEventResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.client.fallback.EventsClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "fp-events", fallback = EventsClientFallback.class)
public interface EventsClient {

    // GET /api/downtimes?lineId=X&from=Y&to=Z
    @GetMapping("/api/downtimes")
    ServiceApiResponse<List<DowntimeEventResponse>> getDowntimesByLine(
            @RequestParam Long lineId,
            @RequestParam String from,
            @RequestParam String to);

    // GET /api/downtimes?from=Y&to=Z  (all lines, date-range only — no lineId)
    @GetMapping("/api/downtimes")
    ServiceApiResponse<List<DowntimeEventResponse>> getAllDowntimesByDateRange(
            @RequestParam String from,
            @RequestParam String to);
}

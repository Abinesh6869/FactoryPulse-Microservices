package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.response.DowntimeEventResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "events-client", url = "${fp.events-base-url:http://localhost:9092}")
public interface EventsClient {

    // GET /api/downtimes?lineId=X&from=Y&to=Z
    @GetMapping("/api/downtimes")
    ServiceApiResponse<List<DowntimeEventResponse>> getDowntimesByLine(
            @RequestParam Long lineId,
            @RequestParam String from,
            @RequestParam String to);
}

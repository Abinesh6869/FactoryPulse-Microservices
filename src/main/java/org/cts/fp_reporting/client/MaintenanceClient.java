package org.cts.fp_reporting.client;

import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.WorkOrderResponse;
import org.cts.fp_reporting.client.fallback.MaintenanceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "fp-maintenance", fallback = MaintenanceClientFallback.class)
public interface MaintenanceClient {

    // GET /api/workorders?from=Y&to=Z
    @GetMapping("/api/workorders")
    ServiceApiResponse<List<WorkOrderResponse>> getWorkOrdersByDateRange(
            @RequestParam String from,
            @RequestParam String to);
}

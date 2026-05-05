package org.cts.fp_reporting.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.MaintenanceClient;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.WorkOrderResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MaintenanceClientFallback implements MaintenanceClient {

    private static final String MSG = "fp-maintenance is unavailable — using fallback";

    @Override
    public ServiceApiResponse<List<WorkOrderResponse>> getWorkOrdersByDateRange(String from, String to) {
        log.warn("FALLBACK: getWorkOrdersByDateRange — {}", MSG);
        ServiceApiResponse<List<WorkOrderResponse>> resp = new ServiceApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(Collections.emptyList());
        return resp;
    }
}

package org.cts.fp_reporting.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.EventsClient;
import org.cts.fp_reporting.dto.response.DowntimeEventResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventsClientFallback implements EventsClient {

    private static final String MSG = "fp-events is unavailable — using fallback";

    @Override
    public ServiceApiResponse<List<DowntimeEventResponse>> getDowntimesByLine(
            Long lineId, String from, String to) {
        log.warn("FALLBACK: getDowntimesByLine(lineId={}) — {}", lineId, MSG);
        return fallback();
    }

    @Override
    public ServiceApiResponse<List<DowntimeEventResponse>> getAllDowntimesByDateRange(
            String from, String to) {
        log.warn("FALLBACK: getAllDowntimesByDateRange — {}", MSG);
        return fallback();
    }

    private ServiceApiResponse<List<DowntimeEventResponse>> fallback() {
        ServiceApiResponse<List<DowntimeEventResponse>> resp = new ServiceApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(MSG);
        resp.setData(Collections.emptyList());
        return resp;
    }
}

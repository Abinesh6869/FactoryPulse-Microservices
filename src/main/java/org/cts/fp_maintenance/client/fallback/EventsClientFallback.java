package org.cts.fp_maintenance.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_maintenance.client.EventsClient;
import org.cts.fp_maintenance.dto.request.InternalNotificationRequest;
import org.cts.fp_maintenance.dto.response.DowntimeInfo;
import org.cts.fp_maintenance.dto.response.IdentityApiResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class EventsClientFallback implements EventsClient {

    private static final String MSG = "fp-events is unavailable — using fallback";

    @Override
    public IdentityApiResponse<DowntimeInfo> getDowntimeById(Long id) {
        log.warn("FALLBACK: getDowntimeById({}) — {}", id, MSG);
        IdentityApiResponse<DowntimeInfo> resp = new IdentityApiResponse<>();
        resp.setMessage(MSG);
        return resp;
    }

    @Override
    public void closeDowntime(Long id, LocalDateTime endAt) {
        log.warn("FALLBACK: closeDowntime({}) skipped — {}", id, MSG);
    }

    @Override
    public void sendNotification(InternalNotificationRequest request) {
        log.warn("FALLBACK: sendNotification to userId={} skipped — {}", request.getUserId(), MSG);
    }
}

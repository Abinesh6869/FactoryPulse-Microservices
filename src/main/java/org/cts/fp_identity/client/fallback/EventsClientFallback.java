package org.cts.fp_identity.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.cts.fp_identity.client.EventsClient;
import org.cts.fp_identity.dto.request.InternalNotificationRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventsClientFallback implements EventsClient {

    private static final String MSG = "fp-events is unavailable — using fallback";

    @Override
    public void sendNotification(InternalNotificationRequest request) {
        log.warn("FALLBACK: sendNotification to userId={} skipped — {}", request.getUserId(), MSG);
    }
}

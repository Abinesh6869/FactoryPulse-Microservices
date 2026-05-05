package org.cts.fp_identity.client;

import org.cts.fp_identity.dto.request.InternalNotificationRequest;
import org.cts.fp_identity.client.fallback.EventsClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fp-events", fallback = EventsClientFallback.class)
public interface EventsClient {

    @PostMapping("/api/alerts/notifications/internal")
    void sendNotification(@RequestBody InternalNotificationRequest request);
}

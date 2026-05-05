package org.cts.fp_maintenance.client;

import org.cts.fp_maintenance.dto.request.InternalNotificationRequest;
import org.cts.fp_maintenance.dto.response.DowntimeInfo;
import org.cts.fp_maintenance.dto.response.IdentityApiResponse;
import org.cts.fp_maintenance.client.fallback.EventsClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(name = "fp-events", fallback = EventsClientFallback.class)
public interface EventsClient {

    // Fetch downtime by ID (used to validate it is not already closed)
    @GetMapping("/api/downtimes/{id}")
    IdentityApiResponse<DowntimeInfo> getDowntimeById(@PathVariable Long id);

    // Close linked downtime when maintenance log is created (matches monolith behaviour)
    @PatchMapping("/api/downtimes/{id}/close")
    void closeDowntime(@PathVariable Long id,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt);

    // Push IN_APP notification to assigned technician (matches monolith behaviour)
    @PostMapping("/api/alerts/notifications/internal")
    void sendNotification(@RequestBody InternalNotificationRequest request);
}

package org.cts.fp_events.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.cts.fp_events.dto.request.InternalNotificationRequest;
import org.cts.fp_events.dto.response.AlertResponse;
import org.cts.fp_events.dto.response.NotificationResponse;
import org.cts.fp_events.dto.response.PageResponse;
import org.cts.fp_events.exception.ApiResponse;
import org.cts.fp_events.security.UserPrincipal;
import org.cts.fp_events.service.AlertService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {
    private final AlertService alertService;

    // GET /api/alerts
    // GET /api/alerts?open=true
    // GET /api/alerts?ruleId=1
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAlerts(
            @RequestParam(required = false) Boolean open,
            @RequestParam(required = false) Long ruleId,
            @PageableDefault(size = 10, sort = "alertId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (open != null && open)
            return ResponseEntity.ok(ApiResponse.success("Open alerts fetched successfully",
                    alertService.getOpenAlerts()));
        if (ruleId != null)
            return ResponseEntity.ok(ApiResponse.success("Alerts fetched successfully",
                    alertService.getAllByRule(ruleId)));
        return ResponseEntity.ok(ApiResponse.success("Alerts fetched successfully",
                new PageResponse<>(alertService.getAllAlerts(pageable))));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully",
                alertService.resolveAlert(id)));
    }

    @GetMapping("/notifications/me")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotificationsByUser(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "notificationId", direction = Sort.Direction.ASC) Pageable pageable) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully",
                new PageResponse<>(alertService.getNotificationsByUser(userId, pageable))));
    }

    @GetMapping("/notifications/me/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotificationsByUser(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully",
                alertService.getUnreadNotificationsByUser(userId)));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read",
                alertService.markNotificationRead(id)));
    }

    // Internal endpoint — called by fp_maintenance to push work-order notifications
    @PostMapping("/notifications/internal")
    public ResponseEntity<ApiResponse<Void>> createInternalNotification(
            @RequestBody InternalNotificationRequest request) {
        alertService.createInternalNotification(
                request.getUserId(), request.getEmployeeId(),
                request.getUserName(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("Notification created", null));
    }
}

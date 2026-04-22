package org.cts.fp_events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.request.AuditLogRequest;
import org.cts.fp_events.dto.request.CorrectiveActionRequest;
import org.cts.fp_events.dto.request.DowntimeEventRequest;
import org.cts.fp_events.dto.response.CorrectiveActionResponse;
import org.cts.fp_events.dto.response.DowntimeEventResponse;
import org.cts.fp_events.exception.BadRequestException;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.model.CorrectiveAction;
import org.cts.fp_events.model.DowntimeEvent;
import org.cts.fp_events.model.Notification;
import org.cts.fp_events.repository.CorrectiveActionRepository;
import org.cts.fp_events.repository.DowntimeEventRepository;
import org.cts.fp_events.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DowntimeEventService {
    private final DowntimeEventRepository downtimeEventRepository;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final NotificationRepository notificationRepository;
    private final IdentityClient identityClient;

    public DowntimeEventResponse createDowntime(DowntimeEventRequest request,
                                                Long loggedById,
                                                String loggedByName,
                                                String loggedByEmployeeId) {
        // Block if machine already has an active downtime
        boolean alreadyDown = downtimeEventRepository.findByEndAtIsNull().stream()
                .anyMatch(d -> d.getMachineId().equals(request.getMachineId()));
        if (alreadyDown) {
            throw new BadRequestException("Machine '" + request.getMachineName()
                    + "' already has an active downtime. Close it before creating a new one.");
        }
        // Block if startAt is in the future
        if (request.getStartAt() != null && request.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Start time cannot be in the future.");
        }
        // Block if endAt is before startAt
        if (request.getStartAt() != null && request.getEndAt() != null
                && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BadRequestException("End time cannot be before start time.");
        }

        DowntimeEvent event = new DowntimeEvent();
        event.setLineId(request.getLineId());
        event.setLineName(request.getLineName());
        event.setMachineId(request.getMachineId());
        event.setMachineName(request.getMachineName());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCategory(request.getCategory());
        event.setNotes(request.getNotes());
        event.setRootCauseId(request.getRootCauseId());
        event.setRootCauseCode(request.getRootCauseCode());
        event.setRootCauseDescription(request.getRootCauseDescription());
        event.setLoggedById(loggedById);
        event.setLoggedByName(loggedByName);
        event.setLoggedByEmployeeId(loggedByEmployeeId);
        if (request.getEndAt() != null && request.getStartAt() != null) {
            event.setDurationSec(Duration.between(request.getStartAt(), request.getEndAt()).toSeconds());
        }
        DowntimeEvent created = downtimeEventRepository.save(event);
        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_DOWNTIME", "DowntimeEvent", "Created downtime ID: " + created.getDowntimeId() + " on machine: " + event.getMachineName())); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toDowntimeResponse(created);
    }

    public Page<DowntimeEventResponse> getAllDowntimes(Pageable pageable) {
        return getAllDowntimes(null, pageable);
    }

    public Page<DowntimeEventResponse> getAllDowntimes(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return downtimeEventRepository.findAll(pageable).map(this::toDowntimeResponse);
        return downtimeEventRepository.search(search, pageable).map(this::toDowntimeResponse);
    }

    public List<DowntimeEventResponse> getDowntimesByLine(Long lineId, LocalDateTime from, LocalDateTime to) {
        return downtimeEventRepository.findByLineIdAndStartAtBetweenOrderByStartAtDesc(lineId, from, to)
                .stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public List<DowntimeEventResponse> getDowntimesByMachine(Long machineId, LocalDateTime from, LocalDateTime to) {
        return downtimeEventRepository.findByMachineIdAndStartAtBetweenOrderByStartAtDesc(machineId, from, to)
                .stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public List<DowntimeEventResponse> getActiveDowntimes() {
        return downtimeEventRepository.findByEndAtIsNull()
                .stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public DowntimeEventResponse getDowntimeById(Long id) {
        return toDowntimeResponse(downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found with id: " + id)));
    }

    public DowntimeEventResponse closeDowntime(Long id, LocalDateTime endAt) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found with id: " + id));
        if (event.getEndAt() != null) {
            throw new BadRequestException("Downtime ID " + id + " is already closed.");
        }
        if (endAt.isBefore(event.getStartAt())) {
            throw new BadRequestException("End time cannot be before start time.");
        }
        event.setEndAt(endAt);
        event.setDurationSec(Duration.between(event.getStartAt(), endAt).toSeconds());
        DowntimeEvent closed = downtimeEventRepository.save(event);
        try { identityClient.recordAuditLog(new AuditLogRequest("CLOSE_DOWNTIME", "DowntimeEvent", "Closed downtime ID: " + id)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toDowntimeResponse(closed);
    }

    public DowntimeEventResponse tagRootCause(Long id, Long rootCauseId,
                                               String rootCauseCode, String rootCauseDescription) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found with id: " + id));
        String warning = null;
        if (event.getEndAt() != null) {
            warning = "Downtime ID " + id + " is already closed. Root cause tagged on a closed downtime.";
        }
        if (event.getRootCauseId() != null) {
            warning = (warning != null ? warning + " | " : "") +
                    "Root cause already set ('" + event.getRootCauseCode() + "'). It will be overwritten.";
        }
        event.setRootCauseId(rootCauseId);
        if (rootCauseCode != null) event.setRootCauseCode(rootCauseCode);
        if (rootCauseDescription != null) event.setRootCauseDescription(rootCauseDescription);
        DowntimeEvent tagged = downtimeEventRepository.save(event);
        try { identityClient.recordAuditLog(new AuditLogRequest("TAG_ROOT_CAUSE", "DowntimeEvent", "Tagged root cause ID: " + rootCauseId + " on downtime ID: " + id)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toDowntimeResponse(tagged, warning);
    }

    public CorrectiveActionResponse createCorrectiveAction(CorrectiveActionRequest request) {
        DowntimeEvent downtime = downtimeEventRepository.findById(request.getDowntimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found with id: " + request.getDowntimeId()));
        if (downtime.getEndAt() == null) {
            throw new BadRequestException("Cannot create corrective action for an open downtime. Close the downtime first.");
        }
        if (downtime.getRootCauseId() == null) {
            throw new BadRequestException("Cannot create corrective action without a root cause. Tag a root cause on the downtime first.");
        }
        if (request.getDueDate() != null && request.getDueDate().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("Due date cannot be in the past.");
        }

        CorrectiveAction action = new CorrectiveAction();
        action.setDowntimeId(downtime.getDowntimeId());
        action.setMachineName(downtime.getMachineName());
        action.setLineName(downtime.getLineName());
        action.setRootCauseCode(downtime.getRootCauseCode());
        action.setAssignedToId(request.getAssignedTo());
        action.setAssignedToEmployeeId(request.getAssignedToEmployeeId());
        action.setAssignedToName(request.getAssignedToName());
        action.setDescription(request.getDescription());
        action.setDueDate(request.getDueDate());
        action.setStatus("OPEN");

        CorrectiveAction saved = correctiveActionRepository.save(action);

        // Notify the assigned user in-app (same as monolithic behaviour)
        try {
            Notification notification = new Notification();
            notification.setUserId(request.getAssignedTo());
            notification.setEmployeeId(request.getAssignedToEmployeeId());
            notification.setUserName(request.getAssignedToName());
            notification.setAlertId(null);
            notification.setChannel("IN_APP");
            notification.setMessage("You have been assigned a Corrective Action: \"" + request.getDescription()
                    + "\" for Machine: " + action.getMachineName()
                    + " | Root Cause: " + action.getRootCauseCode()
                    + " | Due: " + request.getDueDate());
            notification.setSentAt(LocalDateTime.now());
            notification.setStatus("SENT");
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create corrective action notification: {}", e.getMessage());
        }

        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_CORRECTIVE_ACTION", "CorrectiveAction", "Created corrective action ID: " + saved.getActionId() + " for downtime ID: " + request.getDowntimeId())); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toActionResponse(saved);
    }

    public List<CorrectiveActionResponse> getActionsByDowntime(Long downtimeId) {
        return correctiveActionRepository.findByDowntimeId(downtimeId)
                .stream().map(this::toActionResponse).collect(Collectors.toList());
    }

    public List<CorrectiveActionResponse> getMyActions(Long userId) {
        return correctiveActionRepository.findByAssignedToId(userId)
                .stream().map(this::toActionResponse).collect(Collectors.toList());
    }

    public CorrectiveActionResponse completeAction(Long actionId) {
        CorrectiveAction action = correctiveActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Corrective action not found with id: " + actionId));
        if ("COMPLETED".equalsIgnoreCase(action.getStatus())) {
            throw new BadRequestException("Corrective action ID " + actionId + " is already completed.");
        }
        action.setStatus("COMPLETED");
        action.setCompletedAt(LocalDateTime.now());
        CorrectiveAction completed = correctiveActionRepository.save(action);
        try { identityClient.recordAuditLog(new AuditLogRequest("COMPLETE_CORRECTIVE_ACTION", "CorrectiveAction", "Completed action ID: " + actionId)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toActionResponse(completed);
    }

    private DowntimeEventResponse toDowntimeResponse(DowntimeEvent e) {
        return toDowntimeResponse(e, null);
    }

    private DowntimeEventResponse toDowntimeResponse(DowntimeEvent e, String warning) {
        return DowntimeEventResponse.builder()
                .downtimeId(e.getDowntimeId())
                .lineId(e.getLineId())
                .lineName(e.getLineName())
                .machineId(e.getMachineId())
                .machineName(e.getMachineName())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .durationSec(e.getDurationSec())
                .category(e.getCategory())
                .rootCauseId(e.getRootCauseId())
                .rootCauseCode(e.getRootCauseCode())
                .rootCauseDescription(e.getRootCauseDescription())
                .loggedBy(e.getLoggedById())
                .loggedByEmployeeId(e.getLoggedByEmployeeId())
                .loggedByName(e.getLoggedByName())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .warning(warning)
                .build();
    }

    private CorrectiveActionResponse toActionResponse(CorrectiveAction a) {
        return CorrectiveActionResponse.builder()
                .actionId(a.getActionId())
                .downtimeId(a.getDowntimeId())
                .machineName(a.getMachineName())
                .lineName(a.getLineName())
                .rootCauseCode(a.getRootCauseCode())
                .assignedTo(a.getAssignedToId())
                .assignedToEmployeeId(a.getAssignedToEmployeeId())
                .assignedToName(a.getAssignedToName())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .completedAt(a.getCompletedAt())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

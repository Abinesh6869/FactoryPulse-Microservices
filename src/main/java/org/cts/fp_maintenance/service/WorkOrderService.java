package org.cts.fp_maintenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_maintenance.client.EventsClient;
import org.cts.fp_maintenance.client.IdentityClient;
import org.cts.fp_maintenance.dto.request.AuditLogRequest;
import org.cts.fp_maintenance.dto.request.InternalNotificationRequest;
import org.cts.fp_maintenance.dto.request.WorkOrderRequest;
import org.cts.fp_maintenance.dto.response.DowntimeInfo;
import org.cts.fp_maintenance.dto.response.IdentityApiResponse;
import org.cts.fp_maintenance.dto.response.MachineInfo;
import org.cts.fp_maintenance.dto.response.UserInfo;
import org.cts.fp_maintenance.dto.response.WorkOrderResponse;
import org.cts.fp_maintenance.exception.BadRequestException;
import org.cts.fp_maintenance.exception.ResourceNotFoundException;
import org.cts.fp_maintenance.model.WorkOrder;
import org.cts.fp_maintenance.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final IdentityClient identityClient;
    private final EventsClient eventsClient;

    public WorkOrderResponse createWorkOrder(WorkOrderRequest request,
                                             Long createdById,
                                             String createdByName,
                                             String createdByEmployeeId) {
        // Block if another work order already exists for this downtime
        if (workOrderRepository.existsByDowntimeId(request.getDowntimeId())) {
            throw new BadRequestException("A work order already exists for downtime ID: "
                    + request.getDowntimeId() + ". Only one work order per downtime is allowed.");
        }

        // Block if the linked downtime is already closed
        try {
            IdentityApiResponse<DowntimeInfo> downtimeResp = eventsClient.getDowntimeById(request.getDowntimeId());
            if (downtimeResp == null || downtimeResp.getData() == null) {
                throw new BadRequestException("Downtime not found: " + request.getDowntimeId());
            }
            if (downtimeResp.getData().getEndAt() != null) {
                throw new BadRequestException("Cannot link a closed downtime to a work order");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not validate downtime status for downtimeId={}: {}", request.getDowntimeId(), e.getMessage());
        }

        // Auto-resolve machineName from fp_identity if not provided (matches monolith behaviour)
        String machineName = request.getMachineName();
        if ((machineName == null || machineName.isBlank()) && request.getMachineId() != null) {
            try {
                IdentityApiResponse<MachineInfo> resp = identityClient.getMachineById(request.getMachineId());
                if (resp != null && resp.getData() != null) machineName = resp.getData().getName();
                if (machineName == null) machineName = "Unknown Machine";
            } catch (Exception e) {
                log.warn("Could not resolve machineName for machineId={}: {}", request.getMachineId(), e.getMessage());
                machineName = "Unknown Machine";
            }
        }

        // Auto-resolve assignedToName/employeeId from fp_identity if not provided
        String assignedToName = request.getAssignedToName();
        String assignedToEmployeeId = request.getAssignedToEmployeeId();
        if ((assignedToName == null || assignedToEmployeeId == null) && request.getAssignedToUserId() != null) {
            try {
                IdentityApiResponse<UserInfo> resp = identityClient.getUserById(request.getAssignedToUserId());
                if (resp != null && resp.getData() != null) {
                    if (assignedToName == null) assignedToName = resp.getData().getName();
                    if (assignedToEmployeeId == null) assignedToEmployeeId = resp.getData().getEmployeeId();
                }
            } catch (Exception e) { log.warn("Could not resolve user details for userId={}: {}", request.getAssignedToUserId(), e.getMessage()); }
        }

        // Warn if technician already has active jobs
        List<WorkOrder> activeJobs = workOrderRepository.findByAssignedToIdAndStatusIn(
                request.getAssignedToUserId(), List.of("OPEN", "IN_PROGRESS"));
        String warning = null;
        if (!activeJobs.isEmpty()) {
            WorkOrder activeJob = activeJobs.get(0);
            warning = "Technician already has " + activeJobs.size()
                    + " active job(s). Active Work Order #" + activeJob.getWorkOrderId()
                    + " on Machine: " + activeJob.getMachineName()
                    + " (Status: " + activeJob.getStatus() + "). Consider reassigning for better efficiency.";
        }

        WorkOrder wo = new WorkOrder();
        wo.setMachineId(request.getMachineId());
        wo.setMachineName(machineName);
        wo.setDowntimeId(request.getDowntimeId());
        wo.setCreatedById(createdById);
        wo.setCreatedByName(createdByName);
        wo.setCreatedByEmployeeId(createdByEmployeeId);
        wo.setPriority(request.getPriority());
        wo.setDescription(request.getDescription());
        wo.setStatus("OPEN");
        wo.setAssignedToId(request.getAssignedToUserId());
        wo.setAssignedToName(assignedToName);
        wo.setAssignedToEmployeeId(assignedToEmployeeId);

        WorkOrder created = workOrderRepository.save(wo);

        // Notify assigned technician (matches monolith behaviour)
        final String finalAssignedToName = assignedToName;
        final String finalAssignedToEmployeeId = assignedToEmployeeId;
        final String finalMachineName = machineName;
        try {
            eventsClient.sendNotification(new InternalNotificationRequest(
                    request.getAssignedToUserId(), finalAssignedToEmployeeId, finalAssignedToName,
                    "You have been assigned Work Order #" + created.getWorkOrderId()
                            + " — Machine: " + finalMachineName
                            + " | Priority: " + request.getPriority()
                            + " | " + request.getDescription()));
        } catch (Exception e) { log.warn("Failed to send work order notification: {}", e.getMessage()); }

        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_WORK_ORDER", "WorkOrder", "Created work order ID: " + created.getWorkOrderId() + " for machine: " + finalMachineName)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toResponse(created, warning);
    }

    public Page<WorkOrderResponse> getAllWorkOrders(Pageable pageable) {
        return getAllWorkOrders(null, pageable);
    }

    public Page<WorkOrderResponse> getAllWorkOrders(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return workOrderRepository.findAll(pageable).map(this::toResponse);
        return workOrderRepository.search(search, pageable).map(this::toResponse);
    }

    public List<WorkOrderResponse> getWorkOrdersByStatus(String status) {
        return workOrderRepository.findByStatus(status).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<WorkOrderResponse> getWorkOrdersByMachine(Long machineId) {
        return workOrderRepository.findByMachineId(machineId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WorkOrderResponse getWorkOrderById(Long id) {
        return toResponse(workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id)));
    }

    public WorkOrderResponse updateStatus(Long id, String status) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));

        List<String> validStatuses = List.of("OPEN", "IN_PROGRESS", "COMPLETED");
        if (!validStatuses.contains(status.toUpperCase())) {
            throw new BadRequestException("Invalid status: '" + status + "'. Allowed values: OPEN, IN_PROGRESS, COMPLETED.");
        }
        if ("COMPLETED".equalsIgnoreCase(wo.getStatus()) && !status.equalsIgnoreCase("COMPLETED")) {
            throw new BadRequestException("Cannot reopen a completed work order. Work order ID: " + id + " is already COMPLETED.");
        }

        wo.setStatus(status.toUpperCase());
        WorkOrder saved = workOrderRepository.save(wo);
        try { identityClient.recordAuditLog(new AuditLogRequest("UPDATE_WORK_ORDER_STATUS", "WorkOrder", "Updated work order ID: " + id + " to status: " + status)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toResponse(saved);
    }

    public List<WorkOrderResponse> getWorkOrdersByDateRange(LocalDateTime from, LocalDateTime to) {
        return workOrderRepository.findByCreatedAtBetween(from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<WorkOrderResponse> getMyWorkOrders(Long technicianId) {
        return workOrderRepository.findByAssignedToId(technicianId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WorkOrderResponse reassign(Long workOrderId, Long newTechnicianId,
                                       String technicianName, String technicianEmployeeId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + workOrderId));

        if ("COMPLETED".equalsIgnoreCase(wo.getStatus())) {
            throw new BadRequestException("Cannot reassign a completed work order. Work order ID: "
                    + workOrderId + " is already COMPLETED.");
        }

        // Auto-resolve technicianName/employeeId from fp_identity if not provided (matches monolith)
        String resolvedName = technicianName;
        String resolvedEmployeeId = technicianEmployeeId;
        if ((resolvedName == null || resolvedEmployeeId == null) && newTechnicianId != null) {
            try {
                IdentityApiResponse<UserInfo> resp = identityClient.getUserById(newTechnicianId);
                if (resp != null && resp.getData() != null) {
                    if (resolvedName == null) resolvedName = resp.getData().getName();
                    if (resolvedEmployeeId == null) resolvedEmployeeId = resp.getData().getEmployeeId();
                }
                if (resolvedName == null) resolvedName = "Unknown Technician";
            } catch (Exception e) {
                log.warn("Could not resolve user details for technicianId={}: {}", newTechnicianId, e.getMessage());
                if (resolvedName == null) resolvedName = "Unknown Technician";
            }
        }

        wo.setAssignedToId(newTechnicianId);
        wo.setAssignedToName(resolvedName);
        wo.setAssignedToEmployeeId(resolvedEmployeeId);

        WorkOrder saved = workOrderRepository.save(wo);

        // Notify newly assigned technician (matches monolith behaviour)
        final String finalName = resolvedName;
        final String finalEmployeeId = resolvedEmployeeId;
        try {
            eventsClient.sendNotification(new InternalNotificationRequest(
                    newTechnicianId, finalEmployeeId, finalName,
                    "Work Order #" + workOrderId + " has been reassigned to you"
                            + " — Machine: " + wo.getMachineName()
                            + " | Priority: " + wo.getPriority()
                            + " | " + wo.getDescription()));
        } catch (Exception e) { log.warn("Failed to send reassign notification: {}", e.getMessage()); }

        try { identityClient.recordAuditLog(new AuditLogRequest("REASSIGN_WORK_ORDER", "WorkOrder", "Work order ID: " + workOrderId + " reassigned to technician ID: " + newTechnicianId)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toResponse(saved);
    }

    private WorkOrderResponse toResponse(WorkOrder wo) {
        return toResponse(wo, null);
    }

    private WorkOrderResponse toResponse(WorkOrder wo, String warning) {
        return WorkOrderResponse.builder()
                .workOrderId(wo.getWorkOrderId())
                .machineId(wo.getMachineId())
                .machineName(wo.getMachineName())
                .downtimeId(wo.getDowntimeId())
                .createdBy(wo.getCreatedById())
                .createdByName(wo.getCreatedByName())
                .createdByEmployeeId(wo.getCreatedByEmployeeId())
                .priority(wo.getPriority())
                .description(wo.getDescription())
                .status(wo.getStatus())
                .assignedToId(wo.getAssignedToId())
                .assignedToName(wo.getAssignedToName())
                .assignedToEmployeeId(wo.getAssignedToEmployeeId())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .warning(warning)
                .build();
    }
}

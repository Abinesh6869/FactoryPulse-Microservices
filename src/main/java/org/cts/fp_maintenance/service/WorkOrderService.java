package org.cts.fp_maintenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_maintenance.client.IdentityClient;

import org.cts.fp_maintenance.dto.request.AuditLogRequest;
import org.cts.fp_maintenance.dto.request.WorkOrderRequest;
import org.cts.fp_maintenance.dto.response.WorkOrderResponse;
import org.cts.fp_maintenance.exception.BadRequestException;
import org.cts.fp_maintenance.exception.ResourceNotFoundException;
import org.cts.fp_maintenance.model.WorkOrder;
import org.cts.fp_maintenance.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final IdentityClient identityClient;

    public WorkOrderResponse createWorkOrder(WorkOrderRequest request,
                                             Long createdById,
                                             String createdByName,
                                             String createdByEmployeeId) {
        // Block if another work order already exists for this downtime
        if (request.getDowntimeId() != null &&
                workOrderRepository.existsByDowntimeId(request.getDowntimeId())) {
            throw new BadRequestException("A work order already exists for downtime ID: "
                    + request.getDowntimeId() + ". Only one work order per downtime is allowed.");
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
        wo.setMachineName(request.getMachineName());
        wo.setDowntimeId(request.getDowntimeId());
        wo.setCreatedById(createdById);
        wo.setCreatedByName(createdByName);
        wo.setCreatedByEmployeeId(createdByEmployeeId);
        wo.setPriority(request.getPriority());
        wo.setDescription(request.getDescription());
        wo.setStatus("OPEN");
        wo.setAssignedToId(request.getAssignedToUserId());
        wo.setAssignedToName(request.getAssignedToName());
        wo.setAssignedToEmployeeId(request.getAssignedToEmployeeId());

        WorkOrder created = workOrderRepository.save(wo);
        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_WORK_ORDER", "WorkOrder", "Created work order ID: " + created.getWorkOrderId() + " for machine: " + wo.getMachineName())); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
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

        wo.setAssignedToId(newTechnicianId);
        if (technicianName != null) wo.setAssignedToName(technicianName);
        if (technicianEmployeeId != null) wo.setAssignedToEmployeeId(technicianEmployeeId);

        WorkOrder saved = workOrderRepository.save(wo);
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

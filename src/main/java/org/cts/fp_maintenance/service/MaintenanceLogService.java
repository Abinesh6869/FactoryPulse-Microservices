package org.cts.fp_maintenance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_maintenance.client.EventsClient;
import org.cts.fp_maintenance.client.IdentityClient;
import org.cts.fp_maintenance.dto.request.AuditLogRequest;
import org.cts.fp_maintenance.dto.request.MaintenanceLogRequest;
import org.cts.fp_maintenance.dto.response.MaintenanceLogResponse;
import org.cts.fp_maintenance.exception.BadRequestException;
import org.cts.fp_maintenance.exception.ResourceNotFoundException;
import org.cts.fp_maintenance.model.MaintenanceLog;
import org.cts.fp_maintenance.model.WorkOrder;
import org.cts.fp_maintenance.repository.MaintenanceLogRepository;
import org.cts.fp_maintenance.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaintenanceLogService {
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ObjectMapper objectMapper;
    private final IdentityClient identityClient;
    private final EventsClient eventsClient;

    public MaintenanceLogResponse createLog(MaintenanceLogRequest request,
                                            Long performedById,
                                            String performedByName,
                                            String performedByEmployeeId) throws JsonProcessingException {
        WorkOrder wo = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + request.getWorkOrderId()));

        if ("COMPLETED".equalsIgnoreCase(wo.getStatus())) {
            throw new BadRequestException("Cannot create a maintenance log for an already completed work order. Work order ID: "
                    + request.getWorkOrderId());
        }

        MaintenanceLog mlog = new MaintenanceLog();
        mlog.setWorkOrderId(wo.getWorkOrderId());
        mlog.setMachineId(wo.getMachineId());
        mlog.setMachineName(wo.getMachineName());
        mlog.setPerformedById(performedById);
        mlog.setPerformedByName(performedByName);
        mlog.setPerformedByEmployeeId(performedByEmployeeId);
        mlog.setNotes(request.getNotes());
        mlog.setPartsUsedJson(objectMapper.writeValueAsString(request.getPartsUsedJson()));
        mlog.setTimeSpentMinutes(request.getTimeSpentMinutes());

        // Auto-close the work order (matches monolith behaviour)
        wo.setStatus("COMPLETED");
        workOrderRepository.save(wo);

        MaintenanceLog created = maintenanceLogRepository.save(mlog);

        // Auto-close linked downtime if still open (matches monolith behaviour)
        if (wo.getDowntimeId() != null) {
            try {
                eventsClient.closeDowntime(wo.getDowntimeId(), java.time.LocalDateTime.now());
                log.info("Auto-closed downtime ID: {} after maintenance log created", wo.getDowntimeId());
            } catch (Exception e) {
                log.warn("Could not auto-close downtime ID={}: {}", wo.getDowntimeId(), e.getMessage());
            }
        }

        // Set machine back to ACTIVE (matches monolith behaviour)
        if (wo.getMachineId() != null) {
            try {
                identityClient.updateMachineStatus(wo.getMachineId(), "ACTIVE");
                log.info("Machine ID: {} set back to ACTIVE after maintenance", wo.getMachineId());
            } catch (Exception e) {
                log.warn("Could not update machine status for machineId={}: {}", wo.getMachineId(), e.getMessage());
            }
        }

        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_MAINTENANCE_LOG", "MaintenanceLog", "Created log ID: " + created.getLogId() + " for work order ID: " + mlog.getWorkOrderId())); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toResponse(created);
    }

    public Page<MaintenanceLogResponse> getAllLogs(Pageable pageable) {
        return getAllLogs(null, pageable);
    }

    public Page<MaintenanceLogResponse> getAllLogs(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return maintenanceLogRepository.findAll(pageable).map(this::toResponse);
        return maintenanceLogRepository.search(search, pageable).map(this::toResponse);
    }

    public List<MaintenanceLogResponse> getLogsByWorkOrder(Long workOrderId) {
        workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + workOrderId));
        return maintenanceLogRepository.findByWorkOrderId(workOrderId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MaintenanceLogResponse> getLogsByMachine(Long machineId) {
        return maintenanceLogRepository.findByMachineId(machineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MaintenanceLogResponse getLogById(Long id) {
        return toResponse(maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceLog not found with id: " + id)));
    }

    private MaintenanceLogResponse toResponse(MaintenanceLog mlog) {
        List<MaintenanceLogResponse.PartUsed> parts = new ArrayList<>();
        try {
            if (mlog.getPartsUsedJson() != null && !mlog.getPartsUsedJson().isBlank()) {
                parts = objectMapper.readValue(mlog.getPartsUsedJson(),
                        new TypeReference<List<MaintenanceLogResponse.PartUsed>>() {});
            }
        } catch (Exception e) {
            log.warn("Error parsing partsUsedJson for log ID: {}", mlog.getLogId());
        }
        return MaintenanceLogResponse.builder()
                .logId(mlog.getLogId())
                .workOrderId(mlog.getWorkOrderId())
                .machineId(mlog.getMachineId())
                .machineName(mlog.getMachineName())
                .performedBy(mlog.getPerformedById())
                .performedByName(mlog.getPerformedByName())
                .performedByEmployeeId(mlog.getPerformedByEmployeeId())
                .performedAt(mlog.getPerformedAt())
                .notes(mlog.getNotes())
                .partsUsedJson(parts)
                .timeSpentMinutes(mlog.getTimeSpentMinutes())
                .createdAt(mlog.getCreatedAt())
                .build();
    }
}

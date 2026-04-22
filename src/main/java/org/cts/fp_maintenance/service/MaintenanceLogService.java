package org.cts.fp_maintenance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        MaintenanceLog log = new MaintenanceLog();
        log.setWorkOrderId(wo.getWorkOrderId());
        log.setMachineId(wo.getMachineId());
        log.setMachineName(wo.getMachineName());
        log.setPerformedById(performedById);
        log.setPerformedByName(performedByName);
        log.setPerformedByEmployeeId(performedByEmployeeId);
        log.setNotes(request.getNotes());
        log.setPartsUsedJson(objectMapper.writeValueAsString(request.getPartsUsedJson()));
        log.setTimeSpentMinutes(request.getTimeSpentMinutes());

        // Auto-close the work order
        wo.setStatus("COMPLETED");
        workOrderRepository.save(wo);

        MaintenanceLog created = maintenanceLogRepository.save(log);
        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_MAINTENANCE_LOG", "MaintenanceLog", "Created log ID: " + created.getLogId() + " for work order ID: " + log.getWorkOrderId())); } catch (Exception e) { System.out.println("Audit log failed: {}"+ e.getMessage()); }
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

    private MaintenanceLogResponse toResponse(MaintenanceLog log) {
        List<MaintenanceLogResponse.PartUsed> parts = new ArrayList<>();
        try {
            if (log.getPartsUsedJson() != null && !log.getPartsUsedJson().isBlank()) {
                parts = objectMapper.readValue(log.getPartsUsedJson(),
                        new TypeReference<List<MaintenanceLogResponse.PartUsed>>() {});
            }
        } catch (Exception e) {
            System.out.println("Error parsing partsUsedJson for log ID: " + log.getLogId());
        }
        return MaintenanceLogResponse.builder()
                .logId(log.getLogId())
                .workOrderId(log.getWorkOrderId())
                .machineId(log.getMachineId())
                .machineName(log.getMachineName())
                .performedBy(log.getPerformedById())
                .performedByName(log.getPerformedByName())
                .performedByEmployeeId(log.getPerformedByEmployeeId())
                .performedAt(log.getPerformedAt())
                .notes(log.getNotes())
                .partsUsedJson(parts)
                .timeSpentMinutes(log.getTimeSpentMinutes())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

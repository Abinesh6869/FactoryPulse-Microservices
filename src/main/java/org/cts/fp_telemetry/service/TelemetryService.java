package org.cts.fp_telemetry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.request.AuditLogRequest;
import org.cts.fp_telemetry.dto.request.ProductionCountRequest;
import org.cts.fp_telemetry.dto.request.TelemetryEventRequest;
import org.cts.fp_telemetry.dto.response.*;
import org.cts.fp_telemetry.exception.BadRequestException;
import org.cts.fp_telemetry.exception.ResourceNotFoundException;
import org.cts.fp_telemetry.model.ProductionCount;
import org.cts.fp_telemetry.model.TelemetryEvent;
import org.cts.fp_telemetry.repository.ProductionCountRepository;
import org.cts.fp_telemetry.repository.TelemetryEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryEventRepository telemetryEventRepository;
    private final ProductionCountRepository productionCountRepository;
    private final IdentityClient identityClient;

    public Page<TelemetryEventResponse> getEventsByMachine(Long machineId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        try {
            IdentityApiResponse<MachineInfo> machineResp = identityClient.getMachineById(machineId);
            if (machineResp != null && machineResp.getData() != null
                    && !"ACTIVE".equalsIgnoreCase(machineResp.getData().getStatus())) {
                return Page.empty(pageable);
            }
        } catch (Exception e) {
            log.warn("Could not check machine status for machineId={}: {}", machineId, e.getMessage());
        }
        if (from == null || to == null) {
            return telemetryEventRepository
                    .findByMachineIdOrderByTimeStampDesc(machineId, pageable)
                    .map(this::toEventResponse);
        }
        return telemetryEventRepository
                .findByMachineIdAndTimeStampBetweenOrderByTimeStampDesc(machineId, from, to, pageable)
                .map(this::toEventResponse);
    }

    public Page<TelemetryEventResponse> getEventsByPoint(Long pointId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        try {
            IdentityApiResponse<TelemetryPointInfo> resp = identityClient.getPointById(pointId);
            if (resp == null || resp.getData() == null) throw new ResourceNotFoundException("Telemetry point not found: " + pointId);
        } catch (ResourceNotFoundException ex) { throw ex; }
        catch (Exception e) { log.warn("Could not validate pointId={} with identity service: {}", pointId, e.getMessage()); }
        if (from == null || to == null) {
            return telemetryEventRepository
                    .findByPointIdOrderByTimeStampDesc(pointId, pageable)
                    .map(this::toEventResponse);
        }
        return telemetryEventRepository
                .findByPointIdAndTimeStampBetweenOrderByTimeStampDesc(pointId, from, to, pageable)
                .map(this::toEventResponse);
    }

    public List<TelemetryEventResponse> getLatestEventsByMachine(Long machineId) {
        // Return empty if machine is not currently ACTIVE — no live telemetry when machine is off
        try {
            IdentityApiResponse<MachineInfo> machineResp = identityClient.getMachineById(machineId);
            if (machineResp != null && machineResp.getData() != null
                    && !"ACTIVE".equalsIgnoreCase(machineResp.getData().getStatus())) {
                return List.of();
            }
        } catch (Exception e) {
            log.warn("Could not check machine status for machineId={}: {}", machineId, e.getMessage());
        }
        return telemetryEventRepository
                .findTop10ByMachineIdOrderByTimeStampDesc(machineId)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    public Page<ProductionCountResponse> getProductionCountsByLine(Long lineId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        try {
            IdentityApiResponse<LineInfo> resp = identityClient.getLineById(lineId);
            if (resp == null || resp.getData() == null) throw new ResourceNotFoundException("Line not found: " + lineId);
        } catch (ResourceNotFoundException ex) { throw ex; }
        catch (Exception e) { log.warn("Could not validate lineId={} with identity service: {}", lineId, e.getMessage()); }
        if (from != null && to != null) {
            return productionCountRepository
                    .findByLineIdAndTimeStampBetweenOrderByTimeStampDesc(lineId, from, to, pageable)
                    .map(this::toCountResponse);
        }
        return productionCountRepository
                .findByLineIdOrderByTimeStampDesc(lineId, pageable)
                .map(this::toCountResponse);
    }

    public Page<ProductionCountResponse> getProductionCountsByShift(Long shiftId, Pageable pageable) {
        try {
            IdentityApiResponse<ShiftInfo> resp = identityClient.getShiftById(shiftId);
            if (resp == null || resp.getData() == null) throw new ResourceNotFoundException("Shift not found: " + shiftId);
        } catch (ResourceNotFoundException ex) { throw ex; }
        catch (Exception e) { log.warn("Could not validate shiftId={} with identity service: {}", shiftId, e.getMessage()); }
        return productionCountRepository
                .findByShiftId(shiftId, pageable)
                .map(this::toCountResponse);
    }

    public ProductionCountResponse getProductionCountById(Long countId) {
        return toCountResponse(productionCountRepository.findById(countId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCount not found with id: " + countId)));
    }

    public ProductionCountResponse updateProductionCount(Long countId, Integer goodCount, Integer rejectCount) {
        ProductionCount productionCount = productionCountRepository.findById(countId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCount not found with id: " + countId));
        if (goodCount != null) {
            productionCount.setGoodCount(goodCount);
        }
        if (rejectCount != null) {
            productionCount.setRejectCount(rejectCount);
        }
        ProductionCountResponse response = toCountResponse(productionCountRepository.save(productionCount));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("UPDATE_PRODUCTION_COUNT", "ProductionCount",
                    "Updated production count ID: " + countId));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return response;
    }

    // ------------------------------------------------------------------
    // Manual create
    // ------------------------------------------------------------------

    public TelemetryEventResponse createEvent(TelemetryEventRequest req) {
        // Block telemetry creation for machines that are not ACTIVE
        if (req.getMachineId() != null) {
            try {
                IdentityApiResponse<MachineInfo> machineResp = identityClient.getMachineById(req.getMachineId());
                if (machineResp != null && machineResp.getData() != null) {
                    String machineStatus = machineResp.getData().getStatus();
                    if (!"ACTIVE".equalsIgnoreCase(machineStatus)) {
                        throw new BadRequestException("Cannot create telemetry event for machine '"
                                + machineResp.getData().getName() + "' — machine is currently " + machineStatus + ".");
                    }
                }
            } catch (BadRequestException ex) { throw ex; }
            catch (Exception e) {
                log.warn("Could not validate machine status for machineId={}: {}", req.getMachineId(), e.getMessage());
            }
        }
        TelemetryEvent event = new TelemetryEvent();
        event.setPointId(req.getPointId());
        event.setPointName(req.getPointName());
        event.setMachineId(req.getMachineId());
        event.setMachineName(req.getMachineName());
        event.setLineId(req.getLineId());
        event.setLineName(req.getLineName());
        event.setValue(req.getValue());
        event.setSource(req.getSource() != null ? req.getSource() : "MANUAL");
        event.setStatus(req.getStatus() != null ? req.getStatus() : "OK");
        TelemetryEventResponse response = toEventResponse(telemetryEventRepository.save(event));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CREATE_TELEMETRY_EVENT", "TelemetryEvent",
                    "Manual telemetry event created for machine ID: " + req.getMachineId() + ", point: " + req.getPointName()));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return response;
    }

    public ProductionCountResponse createProductionCount(ProductionCountRequest req) {
        ProductionCount count = new ProductionCount();
        count.setLineId(req.getLineId());
        count.setLineName(req.getLineName());
        count.setShiftId(req.getShiftId());
        count.setShiftName(req.getShiftName());
        count.setGoodCount(req.getGoodCount());
        count.setRejectCount(req.getRejectCount());
        ProductionCountResponse response = toCountResponse(productionCountRepository.save(count));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CREATE_PRODUCTION_COUNT", "ProductionCount",
                    "Manual production count created for line ID: " + req.getLineId() + " — good: " + req.getGoodCount() + ", reject: " + req.getRejectCount()));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return response;
    }

    // ------------------------------------------------------------------
    // Mappers
    // ------------------------------------------------------------------

    private TelemetryEventResponse toEventResponse(TelemetryEvent e) {
        return TelemetryEventResponse.builder()
                .eventId(e.getEventId())
                .pointId(e.getPointId())
                .pointName(e.getPointName())
                .machineId(e.getMachineId())
                .machineName(e.getMachineName())
                .lineId(e.getLineId())
                .lineName(e.getLineName())
                .timestamp(e.getTimeStamp())
                .value(e.getValue())
                .source(e.getSource())
                .status(e.getStatus())
                .build();
    }

    private ProductionCountResponse toCountResponse(ProductionCount c) {
        int good   = c.getGoodCount()   != null ? c.getGoodCount()   : 0;
        int reject = c.getRejectCount() != null ? c.getRejectCount() : 0;
        return ProductionCountResponse.builder()
                .countId(c.getCountId())
                .lineId(c.getLineId())
                .lineName(c.getLineName())
                .shiftId(c.getShiftId())
                .shiftName(c.getShiftName())
                .timestamp(c.getTimeStamp())
                .goodCount(good)
                .rejectCount(reject)
                .totalCount(good + reject)
                .build();
    }
}

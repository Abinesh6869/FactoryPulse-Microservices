package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.TelemetryPointRequest;
import org.cts.fp_identity.dto.response.TelemetryPointResponse;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Machine;
import org.cts.fp_identity.model.TelemetryPoint;
import org.cts.fp_identity.repository.MachineRepository;
import org.cts.fp_identity.repository.TelemetryPointRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetryPointService {

    private final TelemetryPointRepository telemetryPointRepository;
    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;

    public TelemetryPointResponse createPoint(TelemetryPointRequest request) {
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + request.getMachineId()));

        if (telemetryPointRepository.existsByMachineMachineIdAndNameIgnoreCase(
                request.getMachineId(), request.getName())) {
            throw new BadRequestException("A telemetry point named '" + request.getName()
                    + "' already exists on machine '" + machine.getName() + "'.");
        }

        TelemetryPoint point = new TelemetryPoint();
        point.setMachine(machine);
        point.setName(request.getName());
        point.setDataType(request.getDataType());
        point.setUnit(request.getUnit());
        TelemetryPoint saved = telemetryPointRepository.save(point);
        auditLogService.log("CREATE_TELEMETRY_POINT", "TelemetryPoint", "Created point ID: " + saved.getPointId());
        return toResponse(saved);
    }

    public List<TelemetryPointResponse> getAllPoints() {
        return getAllPoints(null);
    }

    public List<TelemetryPointResponse> getAllPoints(String search) {
        if (search == null || search.isBlank())
            return telemetryPointRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return telemetryPointRepository.findByNameContainingIgnoreCaseOrUnitContainingIgnoreCase(search, search)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TelemetryPointResponse getTelemetryPointById(Long id) {
        return toResponse(telemetryPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TelemetryPoint not found: " + id)));
    }

    public List<TelemetryPointResponse> getPointsByMachine(Long machineId) {
        machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + machineId));
        return telemetryPointRepository.findByMachineMachineId(machineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TelemetryPointResponse updatePoint(Long id, TelemetryPointRequest request) {
        TelemetryPoint point = telemetryPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TelemetryPoint not found: " + id));
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + request.getMachineId()));

        // Only check duplicates when name OR machine actually changes
        boolean nameChanged    = !point.getName().equalsIgnoreCase(request.getName());
        boolean machineChanged = !point.getMachine().getMachineId().equals(request.getMachineId());
        if ((nameChanged || machineChanged) &&
                telemetryPointRepository.existsByMachineMachineIdAndNameIgnoreCase(
                        request.getMachineId(), request.getName())) {
            throw new BadRequestException("A telemetry point named '" + request.getName()
                    + "' already exists on machine '" + machine.getName() + "'.");
        }

        point.setMachine(machine);
        point.setName(request.getName());
        point.setDataType(request.getDataType());
        point.setUnit(request.getUnit());
        TelemetryPoint saved = telemetryPointRepository.save(point);
        auditLogService.log("UPDATE_TELEMETRY_POINT", "TelemetryPoint", "Updated point ID: " + id);
        return toResponse(saved);
    }

    public void deletePoint(Long id) {
        telemetryPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TelemetryPoint not found: " + id));
        telemetryPointRepository.deleteById(id);
        auditLogService.log("DELETE_TELEMETRY_POINT", "TelemetryPoint", "Deleted point ID: " + id);
    }

    private TelemetryPointResponse toResponse(TelemetryPoint p) {
        return TelemetryPointResponse.builder()
                .pointId(p.getPointId()).machineId(p.getMachine().getMachineId())
                .machineName(p.getMachine().getName()).name(p.getName())
                .dataType(p.getDataType()).unit(p.getUnit())
                .build();
    }
}

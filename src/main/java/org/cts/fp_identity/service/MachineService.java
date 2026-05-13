package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.MachineRequest;
import org.cts.fp_identity.dto.response.MachineResponse;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Line;
import org.cts.fp_identity.model.Machine;
import org.cts.fp_identity.repository.LineRepository;
import org.cts.fp_identity.repository.MachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final LineRepository lineRepository;
    private final AuditLogService auditLogService;

    public MachineResponse createMachine(MachineRequest request) {
        Line line = lineRepository.findById(request.getLineId())
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: " + request.getLineId()));
        if (!"ACTIVE".equalsIgnoreCase(line.getStatus())) {
            throw new BadRequestException("Cannot create machine under line '" + line.getName()
                    + "' — line is currently " + line.getStatus() + ".");
        }
        if (!"ACTIVE".equalsIgnoreCase(line.getPlant().getStatus())) {
            throw new BadRequestException("Cannot create machine — parent plant '"
                    + line.getPlant().getName() + "' is " + line.getPlant().getStatus() + ".");
        }
        Machine machine = new Machine();
        machine.setLine(line);
        machine.setName(request.getName());
        machine.setType(request.getType());
        machine.setModel(request.getModel());
        machine.setSerialNumber(request.getSerialNumber());
        machine.setInstallDate(request.getInstallDate());
        machine.setStatus(request.getStatus());
        Machine saved = machineRepository.save(machine);
        auditLogService.log("CREATE_MACHINE", "Machine", "Created machine ID: " + saved.getMachineId());
        return toResponse(saved);
    }

    public List<MachineResponse> getAllMachines() {
        return getAllMachines(null);
    }

    public List<MachineResponse> getAllMachines(String search) {
        if (search == null || search.isBlank())
            return machineRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return machineRepository.search(search).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MachineResponse getMachineById(Long id) {
        return toResponse(machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id)));
    }

    public List<MachineResponse> getMachinesByLine(Long lineId) {
        lineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: " + lineId));
        return machineRepository.findByLineLineId(lineId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MachineResponse updateMachineStatus(Long id, String status) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
        machine.setStatus(status);
        Machine saved = machineRepository.save(machine);
        auditLogService.log("UPDATE_MACHINE_STATUS", "Machine", "Machine ID: " + id + " status set to: " + status);
        return toResponse(saved);
    }

    public MachineResponse updateMachine(Long id, MachineRequest request) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + id));
        Line line = lineRepository.findById(request.getLineId())
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: " + request.getLineId()));
        if (!"ACTIVE".equalsIgnoreCase(line.getStatus())) {
            throw new BadRequestException("Cannot reassign machine to line '" + line.getName()
                    + "' — line is currently " + line.getStatus() + ".");
        }
        if (!"ACTIVE".equalsIgnoreCase(line.getPlant().getStatus())) {
            throw new BadRequestException("Cannot reassign machine — parent plant '"
                    + line.getPlant().getName() + "' is " + line.getPlant().getStatus() + ".");
        }
        machine.setLine(line);
        machine.setName(request.getName());
        machine.setType(request.getType());
        machine.setModel(request.getModel());
        machine.setSerialNumber(request.getSerialNumber());
        machine.setInstallDate(request.getInstallDate());
        machine.setStatus(request.getStatus());
        Machine saved = machineRepository.save(machine);
        auditLogService.log("UPDATE_MACHINE", "Machine", "Updated machine ID: " + id);
        return toResponse(saved);
    }

    private MachineResponse toResponse(Machine m) {
        return MachineResponse.builder()
                .machineId(m.getMachineId()).lineId(m.getLine().getLineId())
                .lineName(m.getLine().getName()).plantId(m.getLine().getPlant().getPlantId())
                .plantName(m.getLine().getPlant().getName()).name(m.getName())
                .type(m.getType()).model(m.getModel()).serialNumber(m.getSerialNumber())
                .installDate(m.getInstallDate()).status(m.getStatus())
                .lineStatus(m.getLine().getStatus())
                .plantStatus(m.getLine().getPlant().getStatus())
                .build();
    }
}

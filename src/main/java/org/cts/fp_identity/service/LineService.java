package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.LineRequest;
import org.cts.fp_identity.dto.response.LineResponse;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Line;
import org.cts.fp_identity.model.Plant;
import org.cts.fp_identity.repository.LineRepository;
import org.cts.fp_identity.repository.PlantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineService {

    private final LineRepository lineRepository;
    private final PlantRepository plantRepository;
    private final AuditLogService auditLogService;

    public LineResponse createLine(LineRequest request) {
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + request.getPlantId()));
        if (!"ACTIVE".equalsIgnoreCase(plant.getStatus())) {
            throw new BadRequestException("Cannot create line under plant '" + plant.getName()
                    + "' — plant is currently " + plant.getStatus() + ".");
        }
        Line line = new Line();
        line.setPlant(plant);
        line.setName(request.getName());
        line.setProductFamily(request.getProductFamily());
        line.setShiftPattern(request.getShiftPattern());
        line.setStatus(request.getStatus());
        Line saved = lineRepository.save(line);
        auditLogService.log("CREATE_LINE", "Line", "Created line ID: " + saved.getLineId());
        return toResponse(saved);
    }

    public List<LineResponse> getAllLines() {
        return lineRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public LineResponse getLineById(Long id) {
        return toResponse(lineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: " + id)));
    }

    public LineResponse updateLine(Long id, LineRequest request) {
        Line line = lineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Line not found: " + id));
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + request.getPlantId()));
        if (!"ACTIVE".equalsIgnoreCase(plant.getStatus())) {
            throw new BadRequestException("Cannot reassign line to plant '" + plant.getName()
                    + "' — plant is currently " + plant.getStatus() + ".");
        }
        line.setPlant(plant);
        line.setName(request.getName());
        line.setProductFamily(request.getProductFamily());
        line.setShiftPattern(request.getShiftPattern());
        line.setStatus(request.getStatus());
        Line saved = lineRepository.save(line);
        auditLogService.log("UPDATE_LINE", "Line", "Updated line ID: " + id);
        return toResponse(saved);
    }

    private LineResponse toResponse(Line l) {
        return LineResponse.builder()
                .lineId(l.getLineId()).plantId(l.getPlant().getPlantId())
                .plantName(l.getPlant().getName()).name(l.getName())
                .productFamily(l.getProductFamily()).shiftPattern(l.getShiftPattern()).status(l.getStatus())
                .build();
    }
}

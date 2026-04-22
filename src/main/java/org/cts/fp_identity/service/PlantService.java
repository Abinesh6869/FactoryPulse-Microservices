package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.PlantRequest;
import org.cts.fp_identity.dto.response.PlantResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Plant;
import org.cts.fp_identity.repository.PlantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlantService {

    private final PlantRepository plantRepository;
    private final AuditLogService auditLogService;

    public PlantResponse createPlant(PlantRequest request) {
        Plant plant = new Plant();
        plant.setName(request.getName());
        plant.setLocation(request.getLocation());
        plant.setTimezone(request.getTimezone());
        plant.setStatus(request.getStatus());
        Plant saved = plantRepository.save(plant);
        auditLogService.log("CREATE_PLANT", "Plant", "Created plant ID: " + saved.getPlantId());
        return toResponse(saved);
    }

    public List<PlantResponse> getAllPlants() {
        return plantRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PlantResponse updatePlant(Long id, PlantRequest request) {
        Plant plant = plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + id));
        plant.setName(request.getName());
        plant.setLocation(request.getLocation());
        plant.setTimezone(request.getTimezone());
        plant.setStatus(request.getStatus());
        Plant saved = plantRepository.save(plant);
        auditLogService.log("UPDATE_PLANT", "Plant", "Updated plant ID: " + id);
        return toResponse(saved);
    }

    private PlantResponse toResponse(Plant p) {
        return PlantResponse.builder()
                .plantId(p.getPlantId()).name(p.getName())
                .location(p.getLocation()).timezone(p.getTimezone()).status(p.getStatus())
                .build();
    }
}

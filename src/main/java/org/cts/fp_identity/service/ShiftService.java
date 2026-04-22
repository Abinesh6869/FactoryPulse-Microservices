package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.ShiftRequest;
import org.cts.fp_identity.dto.response.ShiftResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Plant;
import org.cts.fp_identity.model.Shift;
import org.cts.fp_identity.repository.PlantRepository;
import org.cts.fp_identity.repository.ShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final PlantRepository plantRepository;
    private final AuditLogService auditLogService;

    public ShiftResponse createShift(ShiftRequest request) {
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + request.getPlantId()));
        Shift shift = new Shift();
        shift.setPlant(plant);
        shift.setName(request.getName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setDate(request.getDate());
        Shift saved = shiftRepository.save(shift);
        auditLogService.log("CREATE_SHIFT", "Shift", "Created shift ID: " + saved.getShiftId());
        return toResponse(saved);
    }

    public Page<ShiftResponse> getAllShifts(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return shiftRepository.findAll(pageable).map(this::toResponse);
        return shiftRepository.search(search, pageable).map(this::toResponse);
    }

    public ShiftResponse getShiftById(Long id) {
        return toResponse(shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id)));
    }

    public List<ShiftResponse> getShiftsByDate(LocalDate date) {
        return shiftRepository.findByDate(date).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftResponse> getShiftsByDateRange(LocalDate from, LocalDate to) {
        return shiftRepository.findByDateBetween(from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ShiftResponse updateShift(Long id, ShiftRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found: " + request.getPlantId()));
        shift.setPlant(plant);
        shift.setName(request.getName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setDate(request.getDate());
        Shift saved = shiftRepository.save(shift);
        auditLogService.log("UPDATE_SHIFT", "Shift", "Updated shift ID: " + id);
        return toResponse(saved);
    }

    private ShiftResponse toResponse(Shift s) {
        return ShiftResponse.builder()
                .shiftId(s.getShiftId()).plantId(s.getPlant().getPlantId())
                .plantName(s.getPlant().getName()).name(s.getName())
                .startTime(s.getStartTime()).endTime(s.getEndTime()).date(s.getDate())
                .build();
    }
}

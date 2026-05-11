package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cts.fp_identity.dto.request.ShiftRequest;
import org.cts.fp_identity.dto.response.BulkShiftResult;
import org.cts.fp_identity.dto.response.ShiftResponse;
import org.cts.fp_identity.exception.BadRequestException;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Plant;
import org.cts.fp_identity.model.Shift;
import org.cts.fp_identity.repository.PlantRepository;
import org.cts.fp_identity.repository.ShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
                .orElseThrow(() -> new ResourceNotFoundException("Plant not found with id: " + request.getPlantId()));

        if (shiftRepository.existsByPlantPlantIdAndDateAndName(request.getPlantId(), request.getDate(), request.getName())) {
            throw new BadRequestException("A '" + request.getName() + "' shift already exists for this plant on " + request.getDate());
        }

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

    public BulkShiftResult bulkCreateShifts(MultipartFile file) {
        List<BulkShiftResult.SkippedRow> skipped = new ArrayList<>();
        List<BulkShiftResult.FailedRow>  failed  = new ArrayList<>();
        List<ShiftResponse>              created = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csv = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true)
                     .build().parse(reader)) {

            int rowNum = 1;
            for (CSVRecord record : csv) {
                rowNum++;
                String name = get(record, "name");
                try {
                    String dateStr      = get(record, "date");
                    String startTimeStr = get(record, "startTime");
                    String endTimeStr   = get(record, "endTime");
                    String plantIdStr   = get(record, "plantId");

                    if (name.isBlank() || dateStr.isBlank() || startTimeStr.isBlank() || endTimeStr.isBlank() || plantIdStr.isBlank()) {
                        failed.add(new BulkShiftResult.FailedRow(rowNum, name.isBlank() ? "—" : name, "Missing required field(s)"));
                        continue;
                    }

                    // Strip Excel formula wrapper: ="2026-05-12" or ="2026-05-12" → 2026-05-12
                    if (dateStr.startsWith("=\"") && dateStr.endsWith("\"")) {
                        dateStr = dateStr.substring(2, dateStr.length() - 1);
                    } else if (dateStr.startsWith("=")) {
                        dateStr = dateStr.substring(1);
                    }

                    LocalDate date;
                    LocalTime startTime, endTime;
                    long plantId;
                    try { date = LocalDate.parse(dateStr); }
                    catch (DateTimeParseException e) { failed.add(new BulkShiftResult.FailedRow(rowNum, name, "Invalid date format (use yyyy-MM-dd)")); continue; }
                    try { startTime = LocalTime.parse(startTimeStr); }
                    catch (DateTimeParseException e) { failed.add(new BulkShiftResult.FailedRow(rowNum, name, "Invalid startTime format (use HH:mm:ss)")); continue; }
                    try { endTime = LocalTime.parse(endTimeStr); }
                    catch (DateTimeParseException e) { failed.add(new BulkShiftResult.FailedRow(rowNum, name, "Invalid endTime format (use HH:mm:ss)")); continue; }
                    try { plantId = Long.parseLong(plantIdStr); }
                    catch (NumberFormatException e) { failed.add(new BulkShiftResult.FailedRow(rowNum, name, "Invalid plantId — must be a number")); continue; }

                    if (!plantRepository.existsById(plantId)) {
                        failed.add(new BulkShiftResult.FailedRow(rowNum, name, "Plant not found with id: " + plantId)); continue;
                    }
                    if (shiftRepository.existsByPlantPlantIdAndDateAndName(plantId, date, name)) {
                        skipped.add(new BulkShiftResult.SkippedRow(rowNum, name, "Shift '" + name + "' already exists on " + date)); continue;
                    }

                    ShiftRequest req = new ShiftRequest();
                    req.setPlantId(plantId);
                    req.setName(name);
                    req.setDate(date);
                    req.setStartTime(startTime);
                    req.setEndTime(endTime);
                    created.add(createShift(req));

                } catch (Exception ex) {
                    failed.add(new BulkShiftResult.FailedRow(rowNum, name.isBlank() ? "—" : name, ex.getMessage()));
                }
            }
        } catch (Exception ex) {
            throw new BadRequestException("Failed to parse CSV: " + ex.getMessage());
        }

        return BulkShiftResult.builder()
                .totalRows(created.size() + skipped.size() + failed.size())
                .created(created.size()).skipped(skipped.size()).failed(failed.size())
                .createdShifts(created).skippedRows(skipped).failedRows(failed)
                .build();
    }

    private String get(CSVRecord r, String col) {
        try { return r.get(col) == null ? "" : r.get(col).trim(); }
        catch (Exception e) { return ""; }
    }

    private ShiftResponse toResponse(Shift s) {
        return ShiftResponse.builder()
                .shiftId(s.getShiftId()).plantId(s.getPlant().getPlantId())
                .plantName(s.getPlant().getName()).name(s.getName())
                .startTime(s.getStartTime()).endTime(s.getEndTime()).date(s.getDate())
                .build();
    }
}

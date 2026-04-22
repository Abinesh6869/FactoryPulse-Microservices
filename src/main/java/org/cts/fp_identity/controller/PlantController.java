package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.PlantRequest;
import org.cts.fp_identity.dto.response.PlantResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.PlantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlantResponse>> createPlant(@Valid @RequestBody PlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plant created successfully", plantService.createPlant(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlantResponse>>> getAllPlants() {
        return ResponseEntity.ok(ApiResponse.success("Plants fetched", plantService.getAllPlants()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlantResponse>> updatePlant(@PathVariable Long id,
            @Valid @RequestBody PlantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Plant updated successfully", plantService.updatePlant(id, request)));
    }
}

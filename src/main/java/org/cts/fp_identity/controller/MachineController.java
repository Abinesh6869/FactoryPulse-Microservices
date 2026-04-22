package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.MachineRequest;
import org.cts.fp_identity.dto.response.MachineResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.MachineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @PostMapping
    public ResponseEntity<ApiResponse<MachineResponse>> createMachine(@Valid @RequestBody MachineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Machine created successfully", machineService.createMachine(request)));
    }

    // GET /api/machines
    // GET /api/machines?id=1
    // GET /api/machines?lineId=1
    // GET /api/machines?search=cnc
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMachines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Machine fetched successfully", machineService.getMachineById(id)));
        if (lineId != null)
            return ResponseEntity.ok(ApiResponse.success("Machines fetched successfully", machineService.getMachinesByLine(lineId)));
        return ResponseEntity.ok(ApiResponse.success("Machines fetched successfully", machineService.getAllMachines(search)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineResponse>> updateMachine(@PathVariable Long id,
            @Valid @RequestBody MachineRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Machine updated successfully", machineService.updateMachine(id, request)));
    }
}

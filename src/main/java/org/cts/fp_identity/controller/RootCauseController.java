package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.RootCauseRequest;
import org.cts.fp_identity.dto.response.RootCauseResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.RootCauseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rootcauses")
@RequiredArgsConstructor
public class RootCauseController {

    private final RootCauseService rootCauseService;

    @PostMapping
    public ResponseEntity<ApiResponse<RootCauseResponse>> createRootCause(@Valid @RequestBody RootCauseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Root cause created", rootCauseService.createRootCause(request)));
    }

    // GET /api/rootcauses
    // GET /api/rootcauses?id=1
    // GET /api/rootcauses?category=ELECTRICAL
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getRootCauses(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String category) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Root cause fetched successfully",
                    rootCauseService.getRootCauseById(id)));
        if (category != null)
            return ResponseEntity.ok(ApiResponse.success("Root causes fetched successfully",
                    rootCauseService.getRootCausesByCategory(category)));
        return ResponseEntity.ok(ApiResponse.success("Root causes fetched successfully",
                rootCauseService.getAllRootCauses()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RootCauseResponse>> updateRootCause(@PathVariable Long id,
            @Valid @RequestBody RootCauseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Root cause updated", rootCauseService.updateRootCause(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRootCause(@PathVariable Long id) {
        rootCauseService.deleteRootCause(id);
        return ResponseEntity.ok(ApiResponse.success("Root cause deleted", null));
    }
}

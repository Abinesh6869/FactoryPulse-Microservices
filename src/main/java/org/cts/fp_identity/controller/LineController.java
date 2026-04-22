package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.LineRequest;
import org.cts.fp_identity.dto.response.LineResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.LineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lines")
@RequiredArgsConstructor
public class LineController {

    private final LineService lineService;

    @PostMapping
    public ResponseEntity<ApiResponse<LineResponse>> createLine(@Valid @RequestBody LineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Line created successfully", lineService.createLine(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getLines(@RequestParam(required = false) Long id) {
        if (id != null)
            return ResponseEntity.ok(ApiResponse.success("Line fetched", lineService.getLineById(id)));
        return ResponseEntity.ok(ApiResponse.success("Lines fetched", lineService.getAllLines()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LineResponse>> updateLine(@PathVariable Long id,
            @Valid @RequestBody LineRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Line updated successfully", lineService.updateLine(id, request)));
    }
}

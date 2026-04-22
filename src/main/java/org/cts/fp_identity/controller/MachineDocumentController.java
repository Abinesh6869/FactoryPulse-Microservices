package org.cts.fp_identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.MachineDocumentRequest;
import org.cts.fp_identity.dto.response.MachineDocumentResponse;
import org.cts.fp_identity.exception.ApiResponse;
import org.cts.fp_identity.service.MachineDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machine-docs")
@RequiredArgsConstructor
public class MachineDocumentController {

    private final MachineDocumentService machineDocumentService;

    @PostMapping
    public ResponseEntity<ApiResponse<MachineDocumentResponse>> uploadDocument(@Valid @RequestBody MachineDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded", machineDocumentService.uploadDocument(request)));
    }

    // GET /api/machine-docs
    // GET /api/machine-docs?machineId=1
    // GET /api/machine-docs?search=manual
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDocuments(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false, defaultValue = "") String search) {
        if (machineId != null)
            return ResponseEntity.ok(ApiResponse.success("MachineDocuments fetched successfully",
                    machineDocumentService.getDocumentsByMachine(machineId)));
        return ResponseEntity.ok(ApiResponse.success("All MachineDocuments fetched successfully",
                machineDocumentService.getAllDocuments(search)));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<MachineDocumentResponse>> verifyDocument(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Document verified", machineDocumentService.verifyDocument(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        machineDocumentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }
}

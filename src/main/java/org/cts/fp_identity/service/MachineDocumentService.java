package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.MachineDocumentRequest;
import org.cts.fp_identity.dto.response.MachineDocumentResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Machine;
import org.cts.fp_identity.model.MachineDocument;
import org.cts.fp_identity.repository.MachineDocumentRepository;
import org.cts.fp_identity.repository.MachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineDocumentService {

    private final MachineDocumentRepository machineDocumentRepository;
    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;

    public MachineDocumentResponse uploadDocument(MachineDocumentRequest request) {
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + request.getMachineId()));
        MachineDocument doc = new MachineDocument();
        doc.setMachine(machine);
        doc.setDocType(request.getDocType());
        doc.setFileUri(request.getFileUri());
        doc.setVerified(false);
        MachineDocument saved = machineDocumentRepository.save(doc);
        auditLogService.log("UPLOAD_DOCUMENT", "MachineDocument", "Uploaded doc ID: " + saved.getDocId());
        return toResponse(saved);
    }

    public List<MachineDocumentResponse> getAllDocuments() {
        return getAllDocuments(null);
    }

    public List<MachineDocumentResponse> getAllDocuments(String search) {
        if (search == null || search.isBlank())
            return machineDocumentRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return machineDocumentRepository.search(search).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MachineDocumentResponse> getDocumentsByMachine(Long machineId) {
        machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + machineId));
        return machineDocumentRepository.findMachineDocumentByMachineMachineId(machineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MachineDocumentResponse verifyDocument(Long id) {
        MachineDocument doc = machineDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        doc.setVerified(true);
        MachineDocument saved = machineDocumentRepository.save(doc);
        auditLogService.log("VERIFY_DOCUMENT", "MachineDocument", "Verified doc ID: " + id);
        return toResponse(saved);
    }

    public void deleteDocument(Long id) {
        machineDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        machineDocumentRepository.deleteById(id);
        auditLogService.log("DELETE_DOCUMENT", "MachineDocument", "Deleted doc ID: " + id);
    }

    private MachineDocumentResponse toResponse(MachineDocument d) {
        return MachineDocumentResponse.builder()
                .docId(d.getDocId()).machineId(d.getMachine().getMachineId())
                .machineName(d.getMachine().getName()).docType(d.getDocType())
                .fileUri(d.getFileUri()).uploadedAt(d.getUploadedAt()).verified(d.getVerified())
                .build();
    }
}

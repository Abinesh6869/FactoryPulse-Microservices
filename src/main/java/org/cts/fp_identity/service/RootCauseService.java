package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.request.RootCauseRequest;
import org.cts.fp_identity.dto.response.RootCauseResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.RootCause;
import org.cts.fp_identity.model.User;
import org.cts.fp_identity.repository.RootCauseRepository;
import org.cts.fp_identity.repository.UserRepository;
import org.cts.fp_identity.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RootCauseService {

    private final RootCauseRepository rootCauseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RootCauseResponse createRootCause(RootCauseRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User creator = userRepository.findById(principal.getUserId()).orElse(null);

        RootCause rc = new RootCause();
        rc.setCode(request.getCode());
        rc.setDescription(request.getDescription());
        rc.setCategory(request.getCategory());
        rc.setCreatedBy(creator);
        RootCause saved = rootCauseRepository.save(rc);
        auditLogService.log("CREATE_ROOT_CAUSE", "RootCause", "Created root cause: " + saved.getCode());
        return toResponse(saved);
    }

    public RootCauseResponse getRootCauseById(Long id) {
        return toResponse(rootCauseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + id)));
    }

    public List<RootCauseResponse> getAllRootCauses() {
        return rootCauseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RootCauseResponse> getRootCausesByCategory(String category) {
        return rootCauseRepository.findByCategory(category)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RootCauseResponse updateRootCause(Long id, RootCauseRequest request) {
        RootCause rc = rootCauseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + id));
        rc.setCode(request.getCode());
        rc.setDescription(request.getDescription());
        rc.setCategory(request.getCategory());
        RootCause saved = rootCauseRepository.save(rc);
        auditLogService.log("UPDATE_ROOT_CAUSE", "RootCause", "Updated root cause ID: " + id);
        return toResponse(saved);
    }

    public void deleteRootCause(Long id) {
        rootCauseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + id));
        rootCauseRepository.deleteById(id);
        auditLogService.log("DELETE_ROOT_CAUSE", "RootCause", "Deleted root cause ID: " + id);
    }

    private RootCauseResponse toResponse(RootCause rc) {
        return RootCauseResponse.builder()
                .rootCauseId(rc.getRootCauseId()).code(rc.getCode())
                .description(rc.getDescription()).category(rc.getCategory())
                .createdById(rc.getCreatedBy() != null ? rc.getCreatedBy().getUserId() : null)
                .createdByEmployeeId(rc.getCreatedBy() != null ? rc.getCreatedBy().getEmployeeId() : null)
                .createdByName(rc.getCreatedBy() != null ? rc.getCreatedBy().getUserName() : null)
                .createdAt(rc.getCreatedAt())
                .build();
    }
}

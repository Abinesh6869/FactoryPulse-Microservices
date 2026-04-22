package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.cts.fp_identity.dto.response.AuditLogResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.AuditLog;
import org.cts.fp_identity.model.User;
import org.cts.fp_identity.repository.AuditLogRepository;
import org.cts.fp_identity.repository.UserRepository;
import org.cts.fp_identity.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String action, String resource, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal userPrincipal)) return;
        User user = userRepository.findById(userPrincipal.getUserId()).orElse(null);
        save(user, action, resource, details);
    }

    public void log(User user, String action, String resource, String details) {
        save(user, action, resource, details);
    }

    public Page<AuditLogResponse> getAllAuditLogs(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return auditLogRepository.findAll(pageable).map(this::toResponse);
        return auditLogRepository.search(search, pageable).map(this::toResponse);
    }

    private void save(User user, String action, String resource, String details) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setResource(resource);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .auditId(log.getAuditId())
                .userId(log.getUser() != null ? log.getUser().getUserId() : null)
                .userName(log.getUser() != null ? log.getUser().getUserName() : null)
                .employeeId(log.getUser() != null ? log.getUser().getEmployeeId() : null)
                .action(log.getAction())
                .resource(log.getResource())
                .timestamp(log.getTimestamp())
                .details(log.getDetails())
                .build();
    }
}

package org.cts.fp_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.request.QualityCorrelationRequest;
import org.cts.fp_reporting.dto.response.QualityCorrelationResponse;
import org.cts.fp_reporting.dto.response.QualitySummaryResponse;
import org.cts.fp_reporting.exception.ResourceNotFoundException;
import org.cts.fp_reporting.model.QualityCorrelation;
import org.cts.fp_reporting.repository.QualityCorrelationRepository;
import org.cts.fp_reporting.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QualityCorrelationService {

    private final QualityCorrelationRepository qualityCorrelationRepository;
    private final IdentityClient identityClient;

    public QualitySummaryResponse getSummaryByLine(Long lineId, LocalDateTime from, LocalDateTime to) {
        List<QualityCorrelationResponse> records = qualityCorrelationRepository
                .findByLineIdAndCreatedAtBetween(lineId, from, to)
                .stream().map(this::toResponse).toList();
        return QualitySummaryResponse.builder()
                .lineId(lineId)
                .lineName(records.isEmpty() ? "" : records.get(0).getLineName())
                .qualityRecords(records)
                .build();
    }

    public QualityCorrelationResponse createQualityRecord(QualityCorrelationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        QualityCorrelation record = new QualityCorrelation();
        record.setLineId(request.getLineId());
        record.setLineName(request.getLineName());
        record.setProductionCountId(request.getProductionCountId());
        record.setGoodCount(request.getGoodCount());
        record.setRejectCount(request.getRejectCount());
        record.setProductionTimestamp(request.getProductionTimestamp());
        record.setDowntimeEventId(request.getDowntimeEventId());
        record.setTelemetryEventId(request.getTelemetryEventId());
        record.setReviewedById(principal.getUserId());
        record.setReviewedByEmployeeId(principal.getEmployeeId());
        record.setReviewedByName(principal.getUsername());
        record.setNotes(request.getNotes());

        QualityCorrelationResponse response = toResponse(qualityCorrelationRepository.save(record));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CREATE_QUALITY_RECORD", "QualityCorrelation",
                    "Created quality record ID: " + response.getQualityRecordId() + " for line: " + request.getLineId()));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return response;
    }

    public Page<QualityCorrelationResponse> getAllQualityRecords(Pageable pageable) {
        return qualityCorrelationRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<QualityCorrelationResponse> getQualityRecordsByLine(Long lineId, Pageable pageable) {
        return qualityCorrelationRepository.findByLineIdOrderByCreatedAtDesc(lineId, pageable).map(this::toResponse);
    }

    public Page<QualityCorrelationResponse> getMyQualityRecords(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return qualityCorrelationRepository
                .findByReviewedByIdOrderByCreatedAtDesc(principal.getUserId(), pageable)
                .map(this::toResponse);
    }

    public QualityCorrelationResponse updateQualityRecord(Long id, QualityCorrelationRequest request) {
        QualityCorrelation record = qualityCorrelationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quality record not found with id: " + id));

        if (request.getLineId() != null)             record.setLineId(request.getLineId());
        if (request.getLineName() != null)           record.setLineName(request.getLineName());
        if (request.getProductionCountId() != null)  record.setProductionCountId(request.getProductionCountId());
        if (request.getGoodCount() != null)          record.setGoodCount(request.getGoodCount());
        if (request.getRejectCount() != null)        record.setRejectCount(request.getRejectCount());
        if (request.getProductionTimestamp() != null) record.setProductionTimestamp(request.getProductionTimestamp());
        if (request.getDowntimeEventId() != null)    record.setDowntimeEventId(request.getDowntimeEventId());
        if (request.getTelemetryEventId() != null)   record.setTelemetryEventId(request.getTelemetryEventId());
        if (request.getNotes() != null)              record.setNotes(request.getNotes());

        QualityCorrelationResponse response = toResponse(qualityCorrelationRepository.save(record));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("UPDATE_QUALITY_RECORD", "QualityCorrelation",
                    "Updated quality record ID: " + id));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return response;
    }

    private QualityCorrelationResponse toResponse(QualityCorrelation r) {
        return QualityCorrelationResponse.builder()
                .qualityRecordId(r.getQualityRecordId())
                .lineId(r.getLineId())
                .lineName(r.getLineName())
                .productionCountId(r.getProductionCountId())
                .goodCount(r.getGoodCount())
                .rejectCount(r.getRejectCount())
                .productionTimestamp(r.getProductionTimestamp())
                .downtimeEventId(r.getDowntimeEventId())
                .telemetryEventId(r.getTelemetryEventId())
                .reviewedById(r.getReviewedById())
                .employeeId(r.getReviewedByEmployeeId())
                .reviewedByName(r.getReviewedByName())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

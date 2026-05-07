package org.cts.fp_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.response.ProductionCountResponse;
import org.cts.fp_reporting.dto.response.ServiceApiResponse;
import org.cts.fp_reporting.dto.response.ServicePageResponse;
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
    private final TelemetryClient telemetryClient;

    public QualitySummaryResponse getSummaryByLine(Long lineId, LocalDateTime from, LocalDateTime to) {
        List<QualityCorrelationResponse> records = qualityCorrelationRepository
                .findByLineIdAndCreatedAtBetween(lineId, from, to)
                .stream().map(this::toResponse).toList();

        // Fetch actual production counts from fp_telemetry for the line+date range
        // (matches monolith which reads from local ProductionCountRepository)
        List<ProductionCountResponse> productionCounts = java.util.Collections.emptyList();
        String lineName = records.isEmpty() ? "" : records.get(0).getLineName();
        try {
            ServiceApiResponse<ServicePageResponse<ProductionCountResponse>> pcResp =
                    telemetryClient.getProductionByLine(lineId,
                            from.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME),
                            to.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME),
                            10000, 0);
            if (pcResp != null && pcResp.getData() != null && pcResp.getData().getContent() != null) {
                productionCounts = pcResp.getData().getContent();
                if (lineName.isBlank() && !productionCounts.isEmpty())
                    lineName = productionCounts.get(0).getLineName() != null
                            ? productionCounts.get(0).getLineName() : lineName;
            }
        } catch (Exception e) {
            log.warn("Could not fetch production counts for lineId={}: {}", lineId, e.getMessage());
        }

        return QualitySummaryResponse.builder()
                .lineId(lineId)
                .lineName(lineName)
                .qualityRecords(records)
                .productionCounts(productionCounts)
                .build();
    }

    public QualityCorrelationResponse createQualityRecord(QualityCorrelationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        // Resolve production count details from fp_telemetry (matches monolith behaviour)
        Long lineId = null;
        String lineName = null;
        Integer goodCount = null;
        Integer rejectCount = null;
        java.time.LocalDateTime productionTimestamp = null;
        try {
            ServiceApiResponse<ProductionCountResponse> pcResp =
                    telemetryClient.getProductionCountById(request.getProductionCountId());
            if (pcResp != null && pcResp.getData() != null) {
                ProductionCountResponse pc = pcResp.getData();
                lineId              = pc.getLineId();
                lineName            = pc.getLineName();
                goodCount           = pc.getGoodCount();
                rejectCount         = pc.getRejectCount();
                productionTimestamp = pc.getTimestamp();
            }
        } catch (Exception e) {
            log.warn("Could not resolve production count details for id={}: {}", request.getProductionCountId(), e.getMessage());
        }

        QualityCorrelation record = new QualityCorrelation();
        record.setLineId(lineId);
        record.setLineName(lineName);
        record.setProductionCountId(request.getProductionCountId());
        record.setGoodCount(goodCount);
        record.setRejectCount(rejectCount);
        record.setProductionTimestamp(productionTimestamp);
        record.setDowntimeEventId(request.getDowntimeEventId());
        record.setTelemetryEventId(request.getTelemetryEventId());
        record.setReviewedById(principal.getUserId());
        record.setReviewedByEmployeeId(principal.getEmployeeId());
        record.setReviewedByName(principal.getUsername());
        record.setNotes(request.getNotes());

        QualityCorrelationResponse response = toResponse(qualityCorrelationRepository.save(record));
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CREATE_QUALITY_RECORD", "QualityCorrelation",
                    "Created quality record ID: " + response.getQualityRecordId() + " for productionCountId: " + request.getProductionCountId()));
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

        if (request.getProductionCountId() != null)  record.setProductionCountId(request.getProductionCountId());
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

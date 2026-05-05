package org.cts.fp_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.request.KPIRequest;
import org.cts.fp_reporting.dto.response.KPIResponse;
import org.cts.fp_reporting.exception.ResourceNotFoundException;
import org.cts.fp_reporting.model.Kpi;
import org.cts.fp_reporting.repository.KpiRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KPIService {

    private final KpiRepository kpiRepository;
    private final IdentityClient identityClient;

    public KPIResponse createKPI(KPIRequest request) {
        Kpi kpi = new Kpi();
        kpi.setName(request.getName());
        kpi.setDefinition(request.getDefinition());
        kpi.setTarget(request.getTarget());
        kpi.setCurrentValue(request.getCurrentValue());
        kpi.setReportingPeriod(request.getReportingPeriod());
        KPIResponse created = toKPIResponse(kpiRepository.save(kpi));
        try { identityClient.recordAuditLog(new AuditLogRequest("CREATE_KPI", "KPI", "Created KPI: " + kpi.getName())); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return created;
    }

    public KPIResponse updateKPI(Long id, KPIRequest request) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KPI not found with ID " + id));
        kpi.setName(request.getName());
        kpi.setDefinition(request.getDefinition());
        kpi.setTarget(request.getTarget());
        kpi.setCurrentValue(request.getCurrentValue());
        kpi.setReportingPeriod(request.getReportingPeriod());
        KPIResponse updated = toKPIResponse(kpiRepository.save(kpi));
        try { identityClient.recordAuditLog(new AuditLogRequest("UPDATE_KPI", "KPI", "Updated KPI ID: " + id)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return updated;
    }

    public KPIResponse updateCurrentValue(Long id, Double value) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KPI not found with ID " + id));
        kpi.setCurrentValue(value);
        KPIResponse updated = toKPIResponse(kpiRepository.save(kpi));
        try { identityClient.recordAuditLog(new AuditLogRequest("UPDATE_KPI_VALUE", "KPI", "Updated KPI ID: " + id + " value to: " + value)); } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return updated;
    }

    public List<KPIResponse> getAllKPIs() {
        return kpiRepository.findAll().stream().map(this::toKPIResponse).collect(Collectors.toList());
    }

    public KPIResponse getKPIById(Long id) {
        return toKPIResponse(kpiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KPI not found with ID " + id)));
    }

    private KPIResponse toKPIResponse(Kpi k) {
        String status = "ON_TRACK";
        if (k.getCurrentValue() != null && k.getTarget() != null) {
            double pct = k.getCurrentValue() / k.getTarget();
            if (pct < 0.7) status = "BELOW_TARGET";
            else if (pct < 0.9) status = "AT_RISK";
        }
        return KPIResponse.builder()
                .kpiId(k.getKpiId())
                .name(k.getName())
                .definition(k.getDefinition())
                .target(k.getTarget())
                .currentValue(k.getCurrentValue())
                .reportingPeriod(k.getReportingPeriod())
                .status(status)
                .build();
    }
}

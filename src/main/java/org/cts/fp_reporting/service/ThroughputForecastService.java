package org.cts.fp_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_reporting.client.IdentityClient;
import org.cts.fp_reporting.client.TelemetryClient;
import org.cts.fp_reporting.dto.request.AuditLogRequest;
import org.cts.fp_reporting.dto.request.ThroughputForecastRequest;
import org.cts.fp_reporting.dto.response.ProductionCountResponse;
import org.cts.fp_reporting.dto.response.ThroughputCompareResponse;
import org.cts.fp_reporting.dto.response.ThroughputForecastResponse;
import org.cts.fp_reporting.exception.BadRequestException;
import org.cts.fp_reporting.exception.ResourceNotFoundException;
import org.cts.fp_reporting.model.ThroughputForecast;
import org.cts.fp_reporting.repository.ThroughputForecastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThroughputForecastService {

    private final ThroughputForecastRepository throughputForecastRepository;
    private final IdentityClient identityClient;
    private final TelemetryClient telemetryClient;

    public ThroughputForecastResponse createThroughputForecast(ThroughputForecastRequest request) {
        if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
            throw new BadRequestException("Period end should be greater than period start");
        }

        // Auto-populate lineName / plantName from identity service if not provided
        String lineName  = request.getLineName();
        String plantName = request.getPlantName();
        if (lineName == null || lineName.isBlank()) {
            try {
                var resp = identityClient.getLineById(request.getLineId());
                if (resp != null && resp.getData() != null) {
                    lineName  = resp.getData().getName();
                    if (plantName == null || plantName.isBlank())
                        plantName = resp.getData().getPlantName() != null ? resp.getData().getPlantName() : "";
                }
            } catch (Exception e) {
                log.warn("Could not fetch line info for forecast: {}", e.getMessage());
            }
        }

        ThroughputForecast forecast = new ThroughputForecast();
        forecast.setLineId(request.getLineId());
        forecast.setLineName(lineName);
        forecast.setPlantName(plantName != null ? plantName : "");
        forecast.setPeriodStart(request.getPeriodStart());
        forecast.setPeriodEnd(request.getPeriodEnd());
        forecast.setExpectedUnits(request.getExpectedUnits());
        forecast.setConfidence(request.getConfidence());
        ThroughputForecast saved = throughputForecastRepository.save(forecast);
        log.info("ThroughputForecast created for line {}", request.getLineId());
        try {
            identityClient.recordAuditLog(new AuditLogRequest("CREATE_THROUGHPUT_FORECAST", "ThroughputForecast",
                    "Created forecast ID: " + saved.getForecastId() + " for line: " + request.getLineId()));
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
        return toResponse(saved);
    }

    public ThroughputForecastResponse getById(Long forecastId) {
        return toResponse(throughputForecastRepository.findById(forecastId)
                .orElseThrow(() -> new ResourceNotFoundException("No Forecast found for the given ID")));
    }

    public Page<ThroughputForecastResponse> getAll(Pageable pageable) {
        return throughputForecastRepository.findAll(pageable).map(this::toResponse);
    }

    public List<ThroughputForecastResponse> getByLine(Long lineId) {
        return throughputForecastRepository.findByLineId(lineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ThroughputForecastResponse getActiveByLine(Long lineId) {
        return throughputForecastRepository.findByActiveLine(lineId, LocalDateTime.now())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active forecast for line: " + lineId));
    }

    public List<ThroughputForecastResponse> getByLineAndPeriod(Long lineId, LocalDateTime start, LocalDateTime end) {
        return throughputForecastRepository.findByLineAndPeriodOverlap(lineId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ThroughputCompareResponse compare(Long forecastId) {
        ThroughputForecast forecast = throughputForecastRepository.findById(forecastId)
                .orElseThrow(() -> new ResourceNotFoundException("No Forecast found for the given ID: " + forecastId));

        long actualUnits = 0L, goodUnits = 0L, rejectUnits = 0L;
        try {
            String fromDT = forecast.getPeriodStart().format(DateTimeFormatter.ISO_DATE_TIME);
            String toDT   = forecast.getPeriodEnd().format(DateTimeFormatter.ISO_DATE_TIME);
            var resp = telemetryClient.getProductionByLine(forecast.getLineId(), fromDT, toDT, 10000, 0);
            if (resp != null && resp.getData() != null && resp.getData().getContent() != null) {
                List<ProductionCountResponse> list = resp.getData().getContent();
                goodUnits   = list.stream().mapToLong(p -> p.getGoodCount()   != null ? p.getGoodCount()   : 0L).sum();
                rejectUnits = list.stream().mapToLong(p -> p.getRejectCount() != null ? p.getRejectCount() : 0L).sum();
                actualUnits = goodUnits + rejectUnits;
            }
        } catch (Exception e) {
            log.warn("Could not fetch production data for forecast compare: {}", e.getMessage());
        }

        double achievementPct = forecast.getExpectedUnits() > 0
                ? Math.round((actualUnits / (double) forecast.getExpectedUnits()) * 10000.0) / 100.0
                : 0.0;

        String status;
        if (achievementPct >= 100.0)     status = "EXCEEDED";
        else if (achievementPct >= 80.0) status = "ON TRACK";
        else                             status = "BEHIND TARGET";

        return ThroughputCompareResponse.builder()
                .forecastId(forecast.getForecastId())
                .lineId(forecast.getLineId())
                .lineName(forecast.getLineName())
                .periodStart(forecast.getPeriodStart())
                .periodEnd(forecast.getPeriodEnd())
                .expectedUnits(forecast.getExpectedUnits())
                .confidence(forecast.getConfidence())
                .actualUnits(actualUnits)
                .goodUnits(goodUnits)
                .rejectUnits(rejectUnits)
                .achievementPct(achievementPct)
                .status(status)
                .build();
    }

    private ThroughputForecastResponse toResponse(ThroughputForecast tf) {
        return ThroughputForecastResponse.builder()
                .forecastId(tf.getForecastId())
                .lineId(tf.getLineId())
                .lineName(tf.getLineName())
                .plantName(tf.getPlantName())
                .expectedUnits(tf.getExpectedUnits())
                .confidence(tf.getConfidence())
                .periodStart(tf.getPeriodStart())
                .periodEnd(tf.getPeriodEnd())
                .build();
    }
}

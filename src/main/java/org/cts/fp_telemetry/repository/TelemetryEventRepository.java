package org.cts.fp_telemetry.repository;

import org.cts.fp_telemetry.model.TelemetryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {
    Page<TelemetryEvent> findByMachineIdAndTimeStampBetweenOrderByTimeStampDesc(Long machineId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<TelemetryEvent> findByPointIdAndTimeStampBetweenOrderByTimeStampDesc(Long pointId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<TelemetryEvent> findTop10ByMachineIdOrderByTimeStampDesc(Long machineId);
}

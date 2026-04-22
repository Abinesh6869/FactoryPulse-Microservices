package org.cts.fp_telemetry.repository;

import org.cts.fp_telemetry.model.ProductionCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionCountRepository extends JpaRepository<ProductionCount, Long> {
    Page<ProductionCount> findByLineIdAndTimeStampBetweenOrderByTimeStampDesc(Long lineId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<ProductionCount> findByLineIdOrderByTimeStampDesc(Long lineId, Pageable pageable);
    List<ProductionCount> findByLineIdAndTimeStampBetweenOrderByTimeStampDesc(Long lineId, LocalDateTime from, LocalDateTime to);
    Page<ProductionCount> findByShiftId(Long shiftId, Pageable pageable);
    List<ProductionCount> findByShiftId(Long shiftId);
    List<ProductionCount> findByTimeStampBetween(LocalDateTime from, LocalDateTime to);
}

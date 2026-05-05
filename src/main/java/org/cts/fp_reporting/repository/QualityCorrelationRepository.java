package org.cts.fp_reporting.repository;

import org.cts.fp_reporting.model.QualityCorrelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QualityCorrelationRepository extends JpaRepository<QualityCorrelation, Long> {
    Page<QualityCorrelation> findByProductionCountIdOrderByCreatedAtDesc(Long productionCountId, Pageable pageable);
    Page<QualityCorrelation> findByReviewedByIdOrderByCreatedAtDesc(Long reviewedById, Pageable pageable);
    Page<QualityCorrelation> findByLineIdOrderByCreatedAtDesc(Long lineId, Pageable pageable);
    List<QualityCorrelation> findByLineIdAndCreatedAtBetween(Long lineId, LocalDateTime from, LocalDateTime to);
}

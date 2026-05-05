package org.cts.fp_reporting.repository;

import org.cts.fp_reporting.model.ThroughputForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThroughputForecastRepository extends JpaRepository<ThroughputForecast, Long> {

    List<ThroughputForecast> findByLineId(Long lineId);

    @Query("SELECT tf FROM ThroughputForecast tf WHERE tf.lineId = :lineId " +
           "AND tf.periodStart <= :end AND tf.periodEnd >= :start")
    List<ThroughputForecast> findByLineAndPeriodOverlap(
            @Param("lineId") Long lineId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT tf FROM ThroughputForecast tf WHERE tf.lineId = :lineId " +
           "AND tf.periodStart <= :now AND tf.periodEnd >= :now")
    Optional<ThroughputForecast> findByActiveLine(
            @Param("lineId") Long lineId,
            @Param("now") LocalDateTime now);
}

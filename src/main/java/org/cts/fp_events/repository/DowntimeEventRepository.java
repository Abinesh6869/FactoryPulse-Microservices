package org.cts.fp_events.repository;

import org.cts.fp_events.model.DowntimeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {
    List<DowntimeEvent> findByLineIdAndStartAtBetweenOrderByStartAtDesc(Long lineId, LocalDateTime from, LocalDateTime to);
    List<DowntimeEvent> findByMachineIdAndStartAtBetweenOrderByStartAtDesc(Long machineId, LocalDateTime from, LocalDateTime to);
    List<DowntimeEvent> findByEndAtIsNull();
    List<DowntimeEvent> findByStartAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT d FROM DowntimeEvent d WHERE " +
           "LOWER(d.lineName) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "(d.machineName IS NOT NULL AND LOWER(d.machineName) LIKE LOWER(CONCAT('%', :s, '%'))) OR " +
           "LOWER(d.category) LIKE LOWER(CONCAT('%', :s, '%'))")
    Page<DowntimeEvent> search(@Param("s") String s, Pageable pageable);
}

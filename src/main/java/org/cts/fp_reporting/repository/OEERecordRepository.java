package org.cts.fp_reporting.repository;

import org.cts.fp_reporting.model.OEERecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OEERecordRepository extends JpaRepository<OEERecord, Long> {

    Optional<OEERecord> findByLineIdAndShiftId(Long lineId, Long shiftId);

    Page<OEERecord> findByLineIdOrderByDateDesc(Long lineId, Pageable pageable);

    Page<OEERecord> findByLineIdAndDateBetweenOrderByDateDesc(Long lineId, LocalDate from, LocalDate to, Pageable pageable);

    Page<OEERecord> findByShiftId(Long shiftId, Pageable pageable);

    Optional<OEERecord> findTop1ByLineIdOrderByDateDesc(Long lineId);

    List<OEERecord> findByDateBetween(LocalDate start, LocalDate end);
}

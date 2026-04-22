package org.cts.fp_reporting.repository;

import org.cts.fp_reporting.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r WHERE " +
           "LOWER(r.scope) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "(r.generatedByName IS NOT NULL AND LOWER(r.generatedByName) LIKE LOWER(CONCAT('%', :s, '%')))")
    Page<Report> search(@Param("s") String s, Pageable pageable);
}

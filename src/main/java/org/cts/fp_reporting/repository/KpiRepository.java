package org.cts.fp_reporting.repository;

import org.cts.fp_reporting.model.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KpiRepository extends JpaRepository<Kpi, Long> {
    Optional<Kpi> findByName(String name);
}

package org.cts.fp_events.repository;

import org.cts.fp_events.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByRuleId(Long ruleId);
    List<Alert> findByStatus(String status);
    Page<Alert> findAll(Pageable pageable);
    boolean existsByRuleIdAndRelatedEntityIdAndStatus(Long ruleId, Long relatedEntityId, String status);
}

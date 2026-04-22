package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByActiveTrue();
}

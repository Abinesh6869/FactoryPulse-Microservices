package org.cts.fp_events.repository;

import org.cts.fp_events.model.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    List<CorrectiveAction> findByDowntimeId(Long downtimeId);
    List<CorrectiveAction> findByAssignedToId(Long userId);
}

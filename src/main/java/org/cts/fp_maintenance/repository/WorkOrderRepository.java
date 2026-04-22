package org.cts.fp_maintenance.repository;

import org.cts.fp_maintenance.model.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(String status);
    List<WorkOrder> findByMachineId(Long machineId);
    Page<WorkOrder> findAll(Pageable pageable);
    List<WorkOrder> findByAssignedToId(Long userId);
    List<WorkOrder> findByAssignedToIdAndStatusIn(Long userId, List<String> statuses);
    boolean existsByDowntimeId(Long downtimeId);
    List<WorkOrder> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT w FROM WorkOrder w WHERE " +
           "LOWER(w.description) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(w.status) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(w.priority) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "(w.machineName IS NOT NULL AND LOWER(w.machineName) LIKE LOWER(CONCAT('%', :s, '%'))) OR " +
           "(w.assignedToName IS NOT NULL AND LOWER(w.assignedToName) LIKE LOWER(CONCAT('%', :s, '%')))")
    Page<WorkOrder> search(@Param("s") String s, Pageable pageable);
}

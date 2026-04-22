package org.cts.fp_maintenance.repository;

import org.cts.fp_maintenance.model.MaintenanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {
    List<MaintenanceLog> findByWorkOrderId(Long workOrderId);
    List<MaintenanceLog> findByMachineId(Long machineId);

    @Query("SELECT l FROM MaintenanceLog l WHERE " +
           "(l.machineName IS NOT NULL AND LOWER(l.machineName) LIKE LOWER(CONCAT('%', :s, '%'))) OR " +
           "(l.notes IS NOT NULL AND LOWER(l.notes) LIKE LOWER(CONCAT('%', :s, '%'))) OR " +
           "(l.performedByName IS NOT NULL AND LOWER(l.performedByName) LIKE LOWER(CONCAT('%', :s, '%')))")
    Page<MaintenanceLog> search(@Param("s") String s, Pageable pageable);
}

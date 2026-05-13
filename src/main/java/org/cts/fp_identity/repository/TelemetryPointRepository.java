package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.TelemetryPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryPointRepository extends JpaRepository<TelemetryPoint, Long> {
    List<TelemetryPoint> findByMachineMachineId(Long machineId);

    List<TelemetryPoint> findByNameContainingIgnoreCaseOrUnitContainingIgnoreCase(String name, String unit);

    // Duplicate guard: same point name on the same machine (case-insensitive)
    boolean existsByMachineMachineIdAndNameIgnoreCase(Long machineId, String name);
}

package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, Long> {
}

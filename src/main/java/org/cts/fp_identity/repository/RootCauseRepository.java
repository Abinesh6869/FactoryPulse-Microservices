package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.RootCause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RootCauseRepository extends JpaRepository<RootCause, Long> {
    List<RootCause> findByCategory(String category);
}

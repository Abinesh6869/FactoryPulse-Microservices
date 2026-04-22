package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByLineLineId(Long lineId);

    @Query("SELECT m FROM Machine m LEFT JOIN m.line l WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(m.type) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "(m.model IS NOT NULL AND LOWER(m.model) LIKE LOWER(CONCAT('%', :s, '%'))) OR " +
           "(l IS NOT NULL AND LOWER(l.name) LIKE LOWER(CONCAT('%', :s, '%')))")
    List<Machine> search(@Param("s") String s);
}

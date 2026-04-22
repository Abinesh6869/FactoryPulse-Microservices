package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByDate(LocalDate date);

    List<Shift> findByDateBetween(LocalDate from, LocalDate to);

    List<Shift> findByPlantPlantId(Long plantId);

    boolean existsByPlantPlantIdAndDateAndName(Long plantId, LocalDate date, String name);

    @Query("SELECT s FROM Shift s JOIN s.plant p WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "CAST(s.date AS string) LIKE CONCAT('%', :s, '%')")
    Page<Shift> search(@Param("s") String s, Pageable pageable);
}

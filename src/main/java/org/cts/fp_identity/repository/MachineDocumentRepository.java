package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.MachineDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineDocumentRepository extends JpaRepository<MachineDocument, Long> {
    List<MachineDocument> findByMachineMachineId(Long machineId);

    List<MachineDocument> findMachineDocumentByMachineMachineId(Long machineId);

    @Query("SELECT d FROM MachineDocument d LEFT JOIN d.machine m WHERE " +
           "LOWER(d.docType) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "(m IS NOT NULL AND LOWER(m.name) LIKE LOWER(CONCAT('%', :s, '%')))")
    List<MachineDocument> search(@Param("s") String s);
}

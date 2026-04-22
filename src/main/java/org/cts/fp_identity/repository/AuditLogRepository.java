package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a LEFT JOIN a.user u WHERE " +
           "LOWER(u.userName) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(a.resource) LIKE LOWER(CONCAT('%', :s, '%'))")
    Page<AuditLog> search(@Param("s") String s, Pageable pageable);
}

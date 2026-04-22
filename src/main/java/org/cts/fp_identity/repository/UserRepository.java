package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.Role;
import org.cts.fp_identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT u.employeeId FROM User u WHERE u.role = :role AND u.employeeId IS NOT NULL ORDER BY u.employeeId DESC LIMIT 1")
    Optional<String> findEmployeeIdByRole(Role role);

    List<User> findByUserNameContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(String userName, String employeeId);

    Optional<User> findByEmployeeId(String employeeId);
}

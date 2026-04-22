package org.cts.fp_identity.repository;

import org.cts.fp_identity.model.Role;
import org.cts.fp_identity.model.ShiftAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShiftAllocationRepository extends JpaRepository<ShiftAllocation, Long> {

    List<ShiftAllocation> findByShiftShiftId(Long shiftId);
    List<ShiftAllocation> findByUserUserId(Long userId);
    boolean existsByShiftShiftIdAndUserUserId(Long shiftId, Long userId);
    boolean existsByUserUserIdAndShiftDateAndShiftShiftIdNot(Long userId, LocalDate date, Long shiftId);
    List<ShiftAllocation> findByShiftShiftIdAndUserRole(Long shiftId, Role role);

    @Query("SELECT COUNT(sa) > 0 FROM ShiftAllocation sa WHERE sa.user.userId = :userId " +
           "AND sa.shift.date = :prevDate " +
           "AND sa.shift.startTime > sa.shift.endTime " +
           "AND sa.shift.endTime >= :newShiftStart")
    boolean existsCrossMidnightConflict(
            @Param("userId") Long userId,
            @Param("prevDate") LocalDate prevDate,
            @Param("newShiftStart") LocalTime newShiftStart);

    @Query("SELECT sa FROM ShiftAllocation sa WHERE " +
           "(sa.shift.date = :today OR (sa.shift.startTime > sa.shift.endTime AND sa.shift.date = :yesterday)) " +
           "AND (" +
           "    (sa.shift.startTime <= sa.shift.endTime AND sa.shift.startTime <= :now AND sa.shift.endTime >= :now) " +
           "    OR " +
           "    (sa.shift.startTime > sa.shift.endTime AND (sa.shift.startTime <= :now OR sa.shift.endTime >= :now))" +
           ")")
    List<ShiftAllocation> findActiveAllocationsByTime(
            @Param("now") LocalTime now,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday);

    @Query("SELECT sa FROM ShiftAllocation sa WHERE sa.user.role = :role " +
           "AND (sa.shift.date = :today OR (sa.shift.startTime > sa.shift.endTime AND sa.shift.date = :yesterday)) " +
           "AND (" +
           "    (sa.shift.startTime <= sa.shift.endTime AND sa.shift.startTime <= :now AND sa.shift.endTime >= :now) " +
           "    OR " +
           "    (sa.shift.startTime > sa.shift.endTime AND (sa.shift.startTime <= :now OR sa.shift.endTime >= :now))" +
           ")")
    List<ShiftAllocation> findActiveAllocationsByTimeAndRole(
            @Param("now") LocalTime now,
            @Param("role") Role role,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday);

    @Query("SELECT a FROM ShiftAllocation a LEFT JOIN a.user u LEFT JOIN a.shift s WHERE " +
           "LOWER(u.userName) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :s, '%')) OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :s, '%'))")
    List<ShiftAllocation> search(@Param("s") String s);
}

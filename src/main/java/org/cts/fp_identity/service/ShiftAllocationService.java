package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.fp_identity.client.EventsClient;
import org.cts.fp_identity.dto.request.InternalNotificationRequest;
import org.cts.fp_identity.dto.request.ShiftAllocationRequest;
import org.cts.fp_identity.dto.response.ShiftAllocationResponse;
import org.cts.fp_identity.dto.response.ShiftAllocationSummaryResponse;
import org.cts.fp_identity.exception.ResourceNotFoundException;
import org.cts.fp_identity.model.Role;
import org.cts.fp_identity.model.Shift;
import org.cts.fp_identity.model.ShiftAllocation;
import org.cts.fp_identity.model.User;
import org.cts.fp_identity.repository.ShiftAllocationRepository;
import org.cts.fp_identity.repository.ShiftRepository;
import org.cts.fp_identity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShiftAllocationService {

    private final ShiftAllocationRepository shiftAllocationRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EventsClient eventsClient;

    public ShiftAllocationSummaryResponse allocate(Long shiftId, ShiftAllocationRequest request, Long allocatedById) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + shiftId));
        User allocatedBy = userRepository.findById(allocatedById)
                .orElseThrow(() -> new ResourceNotFoundException("Allocator not found"));

        List<ShiftAllocationResponse> allocated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        List<String> invalidRole = new ArrayList<>();

        for (String employeeId : request.getEmployeeIds()) {
            User user = userRepository.findByEmployeeId(employeeId).orElse(null);
            if (user == null) {
                notFound.add(employeeId);
                continue;
            }
            if (user.getRole() != Role.OPERATOR && user.getRole() != Role.TECHNICIAN) {
                invalidRole.add(employeeId + " (role: " + user.getRole() + ")");
                continue;
            }
            if (shiftAllocationRepository.existsByShiftShiftIdAndUserUserId(shiftId, user.getUserId())) {
                skipped.add(employeeId + " (already in this shift)");
                continue;
            }
            if (shiftAllocationRepository.existsByUserUserIdAndShiftDateAndShiftShiftIdNot(
                    user.getUserId(), shift.getDate(), shiftId)) {
                skipped.add(employeeId + " (already allocated to another shift on " + shift.getDate() + ")");
                continue;
            }
            if (shiftAllocationRepository.existsCrossMidnightConflict(
                    user.getUserId(), shift.getDate().minusDays(1), shift.getStartTime())) {
                skipped.add(employeeId + " (night shift from previous day overlaps with this shift)");
                continue;
            }
            ShiftAllocation allocation = new ShiftAllocation();
            allocation.setShift(shift);
            allocation.setUser(user);
            allocation.setAllocatedBy(allocatedBy);
            ShiftAllocationResponse saved = toResponse(shiftAllocationRepository.save(allocation));
            allocated.add(saved);

            // Notify the allocated operator/technician
            try {
                eventsClient.sendNotification(new InternalNotificationRequest(
                        user.getUserId(), user.getEmployeeId(), user.getUserName(),
                        "You have been allocated to shift: " + shift.getName()
                                + " on " + shift.getDate()
                                + " (" + shift.getStartTime() + " - " + shift.getEndTime() + ")"
                                + " by " + allocatedBy.getUserName()));
            } catch (Exception e) {
                log.warn("Failed to send shift allocation notification to userId={}: {}", user.getUserId(), e.getMessage());
            }
        }

        auditLogService.log("ALLOCATE_SHIFT", "ShiftAllocation",
                "Allocated " + allocated.size() + " user(s) to shift ID: " + shiftId);

        return ShiftAllocationSummaryResponse.builder()
                .allocated(allocated)
                .skipped(skipped)
                .notFound(notFound)
                .invalidRole(invalidRole)
                .build();
    }

    public List<ShiftAllocationResponse> getAllocationsByShift(Long shiftId) {
        return shiftAllocationRepository.findByShiftShiftId(shiftId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getAllocationsByUser(Long userId) {
        return shiftAllocationRepository.findByUserUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getAllocationsByShiftAndRole(Long shiftId, String role) {
        return shiftAllocationRepository.findByShiftShiftIdAndUserRole(shiftId, Role.valueOf(role.toUpperCase()))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getAllAllocations(String search) {
        if (search == null || search.isBlank())
            return shiftAllocationRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
        return shiftAllocationRepository.search(search).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void removeAllocation(Long allocationId) {
        ShiftAllocation allocation = shiftAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + allocationId));

        User user = allocation.getUser();
        Shift shift = allocation.getShift();

        shiftAllocationRepository.deleteById(allocationId);
        auditLogService.log("REMOVE_SHIFT_ALLOCATION", "ShiftAllocation", "Removed allocation ID: " + allocationId);

        // Notify the removed user
        try {
            eventsClient.sendNotification(new InternalNotificationRequest(
                    user.getUserId(), user.getEmployeeId(), user.getUserName(),
                    "You have been removed from shift: " + shift.getName()
                            + " on " + shift.getDate()
                            + " (" + shift.getStartTime() + " - " + shift.getEndTime() + ")"));
        } catch (Exception e) {
            log.warn("Failed to send shift removal notification to userId={}: {}", user.getUserId(), e.getMessage());
        }
    }

    public List<User> getOnDutyUsersByRole(Role role) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        return shiftAllocationRepository
                .findActiveAllocationsByTimeAndRole(now, role, today, today.minusDays(1))
                .stream().map(ShiftAllocation::getUser)
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .collect(Collectors.toList());
    }

    public List<User> getAllOnDutyUsers() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        return shiftAllocationRepository
                .findActiveAllocationsByTime(now, today, today.minusDays(1))
                .stream().map(ShiftAllocation::getUser)
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getOnDutyByRole(String role) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        return shiftAllocationRepository
                .findActiveAllocationsByTimeAndRole(now, Role.valueOf(role.toUpperCase()), today, today.minusDays(1))
                .stream().filter(sa -> "ACTIVE".equalsIgnoreCase(sa.getUser().getStatus()))
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getAllOnDutyAllocations() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        return shiftAllocationRepository
                .findActiveAllocationsByTime(now, today, today.minusDays(1))
                .stream().filter(sa -> "ACTIVE".equalsIgnoreCase(sa.getUser().getStatus()))
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ShiftAllocationResponse> getAllocationsAtTime(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        return shiftAllocationRepository
                .findActiveAllocationsByTime(dateTime.toLocalTime(), date, date.minusDays(1))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public boolean isTechnicianOnDuty(Long userId) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        return shiftAllocationRepository.findActiveAllocationsByTime(now, today, today.minusDays(1))
                .stream().anyMatch(sa -> sa.getUser().getUserId().equals(userId)
                        && "ACTIVE".equalsIgnoreCase(sa.getUser().getStatus()));
    }

    private ShiftAllocationResponse toResponse(ShiftAllocation a) {
        return ShiftAllocationResponse.builder()
                .allocationId(a.getAllocationId())
                .shiftId(a.getShift().getShiftId())
                .shiftName(a.getShift().getName())
                .shiftDate(a.getShift().getDate())
                .startTime(a.getShift().getStartTime())
                .endTime(a.getShift().getEndTime())
                .userId(a.getUser().getUserId())
                .userName(a.getUser().getUserName())
                .employeeId(a.getUser().getEmployeeId())
                .role(a.getUser().getRole())
                .allocatedById(a.getAllocatedBy().getUserId())
                .allocatedByName(a.getAllocatedBy().getUserName())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

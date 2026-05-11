package org.cts.fp_events.service;

import org.cts.fp_events.client.IdentityClient;
import org.cts.fp_events.dto.request.CorrectiveActionRequest;
import org.cts.fp_events.dto.request.DowntimeEventRequest;
import org.cts.fp_events.dto.response.DowntimeEventResponse;
import org.cts.fp_events.dto.response.CorrectiveActionResponse;
import org.cts.fp_events.dto.response.IdentityApiResponse;
import org.cts.fp_events.dto.response.MachineInfo;
import org.cts.fp_events.exception.BadRequestException;
import org.cts.fp_events.exception.ResourceNotFoundException;
import org.cts.fp_events.model.CorrectiveAction;
import org.cts.fp_events.model.DowntimeEvent;
import org.cts.fp_events.repository.CorrectiveActionRepository;
import org.cts.fp_events.repository.DowntimeEventRepository;
import org.cts.fp_events.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DowntimeEventServiceTest {

    @Mock DowntimeEventRepository downtimeEventRepository;
    @Mock CorrectiveActionRepository correctiveActionRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock IdentityClient identityClient;

    @InjectMocks DowntimeEventService service;

    // ── createDowntime ────────────────────────────────────────────────────────

    @Test
    void createDowntime_validRequest_savesAndReturns() {
        DowntimeEventRequest req = buildRequest(1L, 1L,
                LocalDateTime.now().minusHours(1), null);
        req.setLineName("Line A");
        req.setMachineName("Machine 1");

        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(Collections.emptyList());
        mockMachineStatus("ACTIVE");
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> {
            DowntimeEvent e = inv.getArgument(0);
            e.setDowntimeId(1L);
            return e;
        });

        DowntimeEventResponse result = service.createDowntime(req, 1L, "admin", "EMP001");

        assertThat(result.getDowntimeId()).isEqualTo(1L);
        assertThat(result.getMachineName()).isEqualTo("Machine 1");
        verify(downtimeEventRepository).save(any());
    }

    @Test
    void createDowntime_machineAlreadyDown_throwsBadRequestException() {
        DowntimeEventRequest req = buildRequest(1L, 5L,
                LocalDateTime.now().minusHours(1), null);
        req.setLineName("Line A");
        req.setMachineName("Machine 5");

        DowntimeEvent active = new DowntimeEvent();
        active.setMachineId(5L);
        active.setEndAt(null);
        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(List.of(active));

        assertThatThrownBy(() -> service.createDowntime(req, 1L, "admin", "EMP001"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has an active downtime");
    }

    @Test
    void createDowntime_startAtInFuture_throwsBadRequestException() {
        DowntimeEventRequest req = buildRequest(1L, 1L,
                LocalDateTime.now().plusHours(1), null);
        req.setLineName("Line A");
        req.setMachineName("Machine 1");

        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(Collections.emptyList());
        mockMachineStatus("ACTIVE");

        assertThatThrownBy(() -> service.createDowntime(req, 1L, "admin", "EMP001"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time cannot be in the future");
    }

    @Test
    void createDowntime_endAtBeforeStartAt_throwsBadRequestException() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(3);
        DowntimeEventRequest req = buildRequest(1L, 1L, start, end);
        req.setLineName("Line A");
        req.setMachineName("Machine 1");

        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(Collections.emptyList());
        mockMachineStatus("ACTIVE");

        assertThatThrownBy(() -> service.createDowntime(req, 1L, "admin", "EMP001"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time cannot be before start time");
    }

    @Test
    void createDowntime_calculatesDurationWhenBothTimesProvided() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        DowntimeEventRequest req = buildRequest(1L, 1L, start, end);
        req.setLineName("Line A");
        req.setMachineName("Machine 1");

        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(Collections.emptyList());
        mockMachineStatus("ACTIVE");
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DowntimeEventResponse result = service.createDowntime(req, 1L, "admin", "EMP001");

        assertThat(result.getDurationSec()).isEqualTo(3600L);
    }

    // ── getAllDowntimes ────────────────────────────────────────────────────────

    @Test
    void getAllDowntimes_noSearch_returnsAllPage() {
        DowntimeEvent e = new DowntimeEvent();
        e.setDowntimeId(1L);
        when(downtimeEventRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        Page<DowntimeEventResponse> result = service.getAllDowntimes("", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAllDowntimes_withSearch_callsSearchMethod() {
        when(downtimeEventRepository.search(eq("electrical"), any()))
                .thenReturn(Page.empty());

        service.getAllDowntimes("electrical", PageRequest.of(0, 10));

        verify(downtimeEventRepository).search(eq("electrical"), any());
    }

    // ── getDowntimeById ───────────────────────────────────────────────────────

    @Test
    void getDowntimeById_found_returnsResponse() {
        DowntimeEvent e = downtimeWith(5L, 1L, null);
        when(downtimeEventRepository.findById(5L)).thenReturn(Optional.of(e));

        DowntimeEventResponse result = service.getDowntimeById(5L);

        assertThat(result.getDowntimeId()).isEqualTo(5L);
    }

    @Test
    void getDowntimeById_notFound_throwsResourceNotFoundException() {
        when(downtimeEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDowntimeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getActiveDowntimes ────────────────────────────────────────────────────

    @Test
    void getActiveDowntimes_returnsOpenDowntimes() {
        DowntimeEvent active = downtimeWith(1L, 1L, null);
        when(downtimeEventRepository.findByEndAtIsNull()).thenReturn(List.of(active));

        List<DowntimeEventResponse> result = service.getActiveDowntimes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEndAt()).isNull();
    }

    // ── closeDowntime ─────────────────────────────────────────────────────────

    @Test
    void closeDowntime_setsEndAtAndCalculatesDuration() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        DowntimeEvent e = downtimeWith(1L, 1L, null);
        e.setStartAt(start);
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime endAt = start.plusHours(2);
        DowntimeEventResponse result = service.closeDowntime(1L, endAt);

        assertThat(result.getEndAt()).isEqualTo(endAt);
        assertThat(result.getDurationSec()).isEqualTo(7200L);
    }

    @Test
    void closeDowntime_alreadyClosed_throwsBadRequestException() {
        DowntimeEvent closed = downtimeWith(1L, 1L, LocalDateTime.now());
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.closeDowntime(1L, LocalDateTime.now()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void closeDowntime_endBeforeStart_throwsBadRequestException() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        DowntimeEvent e = downtimeWith(1L, 1L, null);
        e.setStartAt(start);
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.closeDowntime(1L, start.minusHours(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time cannot be before start time");
    }

    @Test
    void closeDowntime_notFound_throwsResourceNotFoundException() {
        when(downtimeEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeDowntime(99L, LocalDateTime.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── tagRootCause ──────────────────────────────────────────────────────────

    @Test
    void tagRootCause_setsRootCauseFields() {
        DowntimeEvent e = downtimeWith(1L, 1L, null);
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(e));
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DowntimeEventResponse result = service.tagRootCause(1L, 7L, "RC-007", "Electrical fault");

        assertThat(result.getRootCauseId()).isEqualTo(7L);
        assertThat(result.getRootCauseCode()).isEqualTo("RC-007");
        assertThat(result.getRootCauseDescription()).isEqualTo("Electrical fault");
    }

    @Test
    void tagRootCause_notFound_throwsResourceNotFoundException() {
        when(downtimeEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tagRootCause(99L, 1L, "RC-001", "desc"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createCorrectiveAction ────────────────────────────────────────────────

    @Test
    void createCorrectiveAction_validRequest_createsAction() {
        DowntimeEvent closed = downtimeWith(1L, 1L, LocalDateTime.now().minusHours(1));
        closed.setRootCauseId(5L);
        closed.setRootCauseCode("RC-005");
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(closed));
        when(identityClient.getOnDuty(null)).thenReturn(new IdentityApiResponse<>());
        when(correctiveActionRepository.save(any())).thenAnswer(inv -> {
            CorrectiveAction a = inv.getArgument(0);
            a.setActionId(1L);
            return a;
        });

        CorrectiveActionRequest req = new CorrectiveActionRequest();
        req.setDowntimeId(1L);
        req.setAssignedTo(10L);
        req.setAssignedToName("Tech User");
        req.setAssignedToEmployeeId("EMP010");
        req.setDescription("Fix wiring");
        req.setDueDate(LocalDate.now().plusDays(3));

        CorrectiveActionResponse result = service.createCorrectiveAction(req);

        assertThat(result.getActionId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getDescription()).isEqualTo("Fix wiring");
    }

    @Test
    void createCorrectiveAction_openDowntime_throwsBadRequestException() {
        DowntimeEvent open = downtimeWith(1L, 1L, null); // endAt is null = still open
        open.setRootCauseId(5L);
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(open));

        CorrectiveActionRequest req = new CorrectiveActionRequest();
        req.setDowntimeId(1L);
        req.setAssignedTo(10L);
        req.setDescription("Fix");
        req.setDueDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.createCorrectiveAction(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("open downtime");
    }

    @Test
    void createCorrectiveAction_noRootCause_throwsBadRequestException() {
        DowntimeEvent closed = downtimeWith(1L, 1L, LocalDateTime.now().minusHours(1));
        closed.setRootCauseId(null); // no root cause
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(closed));

        CorrectiveActionRequest req = new CorrectiveActionRequest();
        req.setDowntimeId(1L);
        req.setAssignedTo(10L);
        req.setDescription("Fix");
        req.setDueDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.createCorrectiveAction(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("root cause");
    }

    @Test
    void createCorrectiveAction_pastDueDate_throwsBadRequestException() {
        DowntimeEvent closed = downtimeWith(1L, 1L, LocalDateTime.now().minusHours(1));
        closed.setRootCauseId(5L);
        when(downtimeEventRepository.findById(1L)).thenReturn(Optional.of(closed));

        CorrectiveActionRequest req = new CorrectiveActionRequest();
        req.setDowntimeId(1L);
        req.setAssignedTo(10L);
        req.setDescription("Fix");
        req.setDueDate(LocalDate.now().minusDays(1)); // yesterday

        assertThatThrownBy(() -> service.createCorrectiveAction(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Due date cannot be in the past");
    }

    // ── completeAction ────────────────────────────────────────────────────────

    @Test
    void completeAction_setsCompletedStatus() {
        CorrectiveAction action = new CorrectiveAction();
        action.setActionId(1L);
        action.setStatus("OPEN");
        when(correctiveActionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(correctiveActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CorrectiveActionResponse result = service.completeAction(1L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    void completeAction_alreadyCompleted_throwsBadRequestException() {
        CorrectiveAction action = new CorrectiveAction();
        action.setActionId(1L);
        action.setStatus("COMPLETED");
        when(correctiveActionRepository.findById(1L)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.completeAction(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void completeAction_notFound_throwsResourceNotFoundException() {
        when(correctiveActionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeAction(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getActionsByDowntime ──────────────────────────────────────────────────

    @Test
    void getActionsByDowntime_returnsActionsForDowntime() {
        CorrectiveAction a = new CorrectiveAction();
        a.setActionId(1L);
        a.setDowntimeId(5L);
        a.setStatus("OPEN");
        when(correctiveActionRepository.findByDowntimeId(5L)).thenReturn(List.of(a));

        List<CorrectiveActionResponse> result = service.getActionsByDowntime(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDowntimeId()).isEqualTo(5L);
    }

    // ── getMyActions ──────────────────────────────────────────────────────────

    @Test
    void getMyActions_returnsActionsAssignedToUser() {
        CorrectiveAction a = new CorrectiveAction();
        a.setActionId(1L);
        a.setAssignedToId(10L);
        a.setStatus("OPEN");
        when(correctiveActionRepository.findByAssignedToId(10L)).thenReturn(List.of(a));

        List<CorrectiveActionResponse> result = service.getMyActions(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedTo()).isEqualTo(10L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DowntimeEventRequest buildRequest(Long lineId, Long machineId,
                                              LocalDateTime startAt, LocalDateTime endAt) {
        DowntimeEventRequest req = new DowntimeEventRequest();
        req.setLineId(lineId);
        req.setMachineId(machineId);
        req.setStartAt(startAt);
        req.setEndAt(endAt);
        return req;
    }

    private DowntimeEvent downtimeWith(Long id, Long machineId, LocalDateTime endAt) {
        DowntimeEvent e = new DowntimeEvent();
        e.setDowntimeId(id);
        e.setMachineId(machineId);
        e.setLineName("Line A");
        e.setMachineName("Machine 1");
        e.setStartAt(LocalDateTime.now().minusHours(3));
        e.setEndAt(endAt);
        return e;
    }

    private void mockMachineStatus(String status) {
        MachineInfo machine = new MachineInfo();
        machine.setStatus(status);
        IdentityApiResponse<MachineInfo> resp = new IdentityApiResponse<>();
        resp.setData(machine);
        when(identityClient.getMachineById(any())).thenReturn(resp);
    }
}

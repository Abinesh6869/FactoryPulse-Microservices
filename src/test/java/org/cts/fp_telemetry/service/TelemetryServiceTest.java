package org.cts.fp_telemetry.service;

import org.cts.fp_telemetry.client.IdentityClient;
import org.cts.fp_telemetry.dto.request.ProductionCountRequest;
import org.cts.fp_telemetry.dto.request.TelemetryEventRequest;
import org.cts.fp_telemetry.dto.response.*;
import org.cts.fp_telemetry.exception.BadRequestException;
import org.cts.fp_telemetry.exception.ResourceNotFoundException;
import org.cts.fp_telemetry.model.ProductionCount;
import org.cts.fp_telemetry.model.TelemetryEvent;
import org.cts.fp_telemetry.repository.ProductionCountRepository;
import org.cts.fp_telemetry.repository.TelemetryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock private TelemetryEventRepository telemetryEventRepository;
    @Mock private ProductionCountRepository productionCountRepository;
    @Mock private IdentityClient identityClient;

    @InjectMocks private TelemetryService telemetryService;

    private Pageable pageable;
    private TelemetryEvent sampleEvent;
    private ProductionCount sampleCount;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10, Sort.by("eventId").ascending());

        sampleEvent = TelemetryEvent.builder()
                .eventId(1L).pointId(10L).pointName("Temp Sensor")
                .machineId(5L).machineName("Machine A")
                .lineId(2L).lineName("Line 1")
                .value("75.5").source("SENSOR").status("OK")
                .build();

        sampleCount = new ProductionCount();
        sampleCount.setCountId(1L);
        sampleCount.setLineId(2L);
        sampleCount.setLineName("Line 1");
        sampleCount.setShiftId(3L);
        sampleCount.setShiftName("Morning Shift");
        sampleCount.setGoodCount(100);
        sampleCount.setRejectCount(5);
    }

    // getEventsByMachine

    @Test
    @Order(1)
    void getEventsByMachine_activeMachine_returnsEvents() {
        mockActiveMachine(5L);
        when(telemetryEventRepository.findByMachineIdOrderByTimeStampDesc(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(sampleEvent)));

        Page<TelemetryEventResponse> result = telemetryService.getEventsByMachine(5L, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMachineId()).isEqualTo(5L);
    }

    @Test
    @Order(2)
    void getEventsByMachine_inactiveMachine_returnsEmptyPage() {
        mockMachineWithStatus(5L, "INACTIVE");

        Page<TelemetryEventResponse> result = telemetryService.getEventsByMachine(5L, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(telemetryEventRepository, never()).findByMachineIdOrderByTimeStampDesc(any(), any());
    }

    @Test
    @Order(3)
    void getEventsByMachine_identityServiceThrows_fallsBackToRepo() {
        when(identityClient.getMachineById(5L)).thenThrow(new RuntimeException("Service down"));
        when(telemetryEventRepository.findByMachineIdOrderByTimeStampDesc(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(sampleEvent)));

        Page<TelemetryEventResponse> result = telemetryService.getEventsByMachine(5L, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @Order(4)
    void getEventsByMachine_withDateRange_usesRangeQuery() {
        mockActiveMachine(5L);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to   = LocalDateTime.now();
        when(telemetryEventRepository
                .findByMachineIdAndTimeStampBetweenOrderByTimeStampDesc(eq(5L), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(sampleEvent)));

        telemetryService.getEventsByMachine(5L, from, to, pageable);

        verify(telemetryEventRepository)
                .findByMachineIdAndTimeStampBetweenOrderByTimeStampDesc(eq(5L), eq(from), eq(to), any());
    }

    // getEventsByPoint

    @Test
    @Order(5)
    void getEventsByPoint_validPoint_returnsEvents() {
        mockValidPoint(10L);
        when(telemetryEventRepository.findByPointIdOrderByTimeStampDesc(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(sampleEvent)));

        Page<TelemetryEventResponse> result = telemetryService.getEventsByPoint(10L, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPointId()).isEqualTo(10L);
    }

    @Test
    @Order(6)
    void getEventsByPoint_pointNotFound_throwsResourceNotFoundException() {
        IdentityApiResponse<TelemetryPointInfo> resp = new IdentityApiResponse<>();
        resp.setData(null);
        when(identityClient.getPointById(10L)).thenReturn(resp);

        assertThatThrownBy(() -> telemetryService.getEventsByPoint(10L, null, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("10");
    }

    // getLatestEventsByMachine

    @Test
    @Order(7)
    void getLatestEventsByMachine_activeMachine_returnsTop10() {
        mockActiveMachine(5L);
        when(telemetryEventRepository.findTop10ByMachineIdOrderByTimeStampDesc(5L))
                .thenReturn(List.of(sampleEvent));

        List<TelemetryEventResponse> result = telemetryService.getLatestEventsByMachine(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMachineId()).isEqualTo(5L);
    }

    @Test
    @Order(8)
    void getLatestEventsByMachine_maintenanceMachine_returnsEmptyList() {
        mockMachineWithStatus(5L, "MAINTENANCE");

        List<TelemetryEventResponse> result = telemetryService.getLatestEventsByMachine(5L);

        assertThat(result).isEmpty();
        verify(telemetryEventRepository, never()).findTop10ByMachineIdOrderByTimeStampDesc(any());
    }

    // getProductionCountsByLine

    @Test
    @Order(9)
    void getProductionCountsByLine_validLine_returnsPage() {
        mockValidLine(2L);
        when(productionCountRepository.findByLineIdOrderByTimeStampDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(sampleCount)));

        Page<ProductionCountResponse> result = telemetryService.getProductionCountsByLine(2L, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLineId()).isEqualTo(2L);
    }

    @Test
    @Order(10)
    void getProductionCountsByLine_lineNotFound_throwsResourceNotFoundException() {
        IdentityApiResponse<LineInfo> resp = new IdentityApiResponse<>();
        resp.setData(null);
        when(identityClient.getLineById(2L)).thenReturn(resp);

        assertThatThrownBy(() -> telemetryService.getProductionCountsByLine(2L, null, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("2");
    }

    // getProductionCountsByShift

    @Test
    @Order(11)
    void getProductionCountsByShift_validShift_returnsPage() {
        mockValidShift(3L);
        when(productionCountRepository.findByShiftId(eq(3L), any()))
                .thenReturn(new PageImpl<>(List.of(sampleCount)));

        Page<ProductionCountResponse> result = telemetryService.getProductionCountsByShift(3L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @Order(12)
    void getProductionCountsByShift_shiftNotFound_throwsResourceNotFoundException() {
        IdentityApiResponse<ShiftInfo> resp = new IdentityApiResponse<>();
        resp.setData(null);
        when(identityClient.getShiftById(3L)).thenReturn(resp);

        assertThatThrownBy(() -> telemetryService.getProductionCountsByShift(3L, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("3");
    }

    // getProductionCountById

    @Test
    @Order(13)
    void getProductionCountById_found_calculatesTotalCount() {
        when(productionCountRepository.findById(1L)).thenReturn(Optional.of(sampleCount));

        ProductionCountResponse result = telemetryService.getProductionCountById(1L);

        assertThat(result.getGoodCount()).isEqualTo(100);
        assertThat(result.getRejectCount()).isEqualTo(5);
        assertThat(result.getTotalCount()).isEqualTo(105);
    }

    @Test
    @Order(14)
    void getProductionCountById_notFound_throwsResourceNotFoundException() {
        when(productionCountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.getProductionCountById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @Order(15)
    void getProductionCountById_nullCounts_treatedAsZero() {
        ProductionCount count = new ProductionCount();
        count.setCountId(5L);
        count.setLineId(1L);
        count.setGoodCount(null);
        count.setRejectCount(null);
        when(productionCountRepository.findById(5L)).thenReturn(Optional.of(count));

        ProductionCountResponse result = telemetryService.getProductionCountById(5L);

        assertThat(result.getTotalCount()).isZero();
    }

    // updateProductionCount

    @Test
    @Order(16)
    void updateProductionCount_updatesGoodAndRejectCounts() {
        when(productionCountRepository.findById(1L)).thenReturn(Optional.of(sampleCount));
        when(productionCountRepository.save(any())).thenReturn(sampleCount);

        telemetryService.updateProductionCount(1L, 120, 10);

        verify(productionCountRepository).save(argThat(pc ->
                pc.getGoodCount() == 120 && pc.getRejectCount() == 10));
    }

    @Test
    @Order(17)
    void updateProductionCount_onlyGoodCount_doesNotChangeReject() {
        when(productionCountRepository.findById(1L)).thenReturn(Optional.of(sampleCount));
        when(productionCountRepository.save(any())).thenReturn(sampleCount);

        telemetryService.updateProductionCount(1L, 200, null);

        verify(productionCountRepository).save(argThat(pc ->
                pc.getGoodCount() == 200 && pc.getRejectCount() == 5));
    }

    @Test
    @Order(18)
    void updateProductionCount_notFound_throwsResourceNotFoundException() {
        when(productionCountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.updateProductionCount(99L, 10, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // createEvent

    @Test
    @Order(19)
    void createEvent_activeMachine_savesAndReturns() {
        mockActiveMachine(5L);
        when(telemetryEventRepository.save(any())).thenReturn(sampleEvent);

        TelemetryEventResponse result = telemetryService.createEvent(buildEventRequest());

        assertThat(result.getMachineId()).isEqualTo(5L);
        verify(telemetryEventRepository).save(any());
    }

    @Test
    @Order(20)
    void createEvent_inactiveMachine_throwsBadRequestException() {
        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setStatus("MAINTENANCE");
        machineInfo.setName("Machine A");
        IdentityApiResponse<MachineInfo> resp = new IdentityApiResponse<>();
        resp.setData(machineInfo);
        when(identityClient.getMachineById(5L)).thenReturn(resp);

        assertThatThrownBy(() -> telemetryService.createEvent(buildEventRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    @Test
    @Order(21)
    void createEvent_defaultsSourceAndStatus_whenNotProvided() {
        mockActiveMachine(5L);
        when(telemetryEventRepository.save(any())).thenReturn(sampleEvent);

        TelemetryEventRequest req = buildEventRequest();
        req.setSource(null);
        req.setStatus(null);

        telemetryService.createEvent(req);

        verify(telemetryEventRepository).save(argThat(e ->
                "MANUAL".equals(e.getSource()) && "OK".equals(e.getStatus())));
    }

    // createProductionCount

    @Test
    @Order(22)
    void createProductionCount_savesAndReturnsTotalCount() {
        when(productionCountRepository.save(any())).thenReturn(sampleCount);

        ProductionCountRequest req = new ProductionCountRequest();
        req.setLineId(2L); req.setLineName("Line 1");
        req.setShiftId(3L); req.setShiftName("Morning Shift");
        req.setGoodCount(100); req.setRejectCount(5);

        ProductionCountResponse result = telemetryService.createProductionCount(req);

        assertThat(result.getTotalCount()).isEqualTo(105);
    }

    // Helpers

    private void mockActiveMachine(Long id) {
        mockMachineWithStatus(id, "ACTIVE");
    }

    private void mockMachineWithStatus(Long id, String status) {
        MachineInfo info = new MachineInfo();
        info.setStatus(status);
        info.setName("Machine " + id);
        IdentityApiResponse<MachineInfo> resp = new IdentityApiResponse<>();
        resp.setData(info);
        when(identityClient.getMachineById(id)).thenReturn(resp);
    }

    private void mockValidPoint(Long id) {
        IdentityApiResponse<TelemetryPointInfo> resp = new IdentityApiResponse<>();
        resp.setData(new TelemetryPointInfo());
        when(identityClient.getPointById(id)).thenReturn(resp);
    }

    private void mockValidLine(Long id) {
        IdentityApiResponse<LineInfo> resp = new IdentityApiResponse<>();
        resp.setData(new LineInfo());
        when(identityClient.getLineById(id)).thenReturn(resp);
    }

    private void mockValidShift(Long id) {
        IdentityApiResponse<ShiftInfo> resp = new IdentityApiResponse<>();
        resp.setData(new ShiftInfo());
        when(identityClient.getShiftById(id)).thenReturn(resp);
    }

    private TelemetryEventRequest buildEventRequest() {
        TelemetryEventRequest req = new TelemetryEventRequest();
        req.setPointId(10L); req.setPointName("Temp Sensor");
        req.setMachineId(5L); req.setMachineName("Machine A");
        req.setValue("75.5");
        return req;
    }
}
package com.hims.linen.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.linen.application.exceptions.InvalidLinenRequestException;
import com.hims.linen.application.exceptions.LinenNotFoundException;
import com.hims.linen.domain.model.LinenRequest;
import com.hims.linen.domain.model.LinenRequestDetails;
import com.hims.linen.domain.model.LinenStatus;
import com.hims.linen.domain.repository.LinenRequestDetailsRepository;
import com.hims.linen.domain.repository.LinenRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** L1 — linen request lifecycle business rules (7-state machine, detail stamping, guards, numbering). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinenRequestServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private LinenRequestRepository requestRepository;
    @Mock private LinenRequestDetailsRepository detailsRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private LinenRequestService service;

    @BeforeEach
    void setUp() {
        service = new LinenRequestService(requestRepository, detailsRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence stubs
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(requestRepository.save(any(LinenRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detailsRepository.save(any(LinenRequestDetails.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void raiseRequest_stampsRequisitionNoStatusAndSavesDetails() throws Exception {
        LinenRequest r = service.raiseRequest(mapper.readTree("""
                {"request":{"storeId":10,"toStoreId":20},
                 "details":[{"itemId":1,"batchId":5,"quantity":3,"remark":"bedsheets"},
                            {"itemId":2,"quantity":1}]}
                """), 7L, "RISL");
        assertThat(r.getRequisitionNo()).matches("RISL/LR/\\d{2}/\\d{5}");
        assertThat(r.getRequestStatus()).isEqualTo(LinenStatus.PENDING);
        assertThat(r.getRequestDate()).isNotNull();
        assertThat(r.getUnitId()).isEqualTo(7L);
        assertThat(r.getAddedBy()).isEqualTo(LinenRequestService.AUDIT_ACTOR_ID);
        org.mockito.Mockito.verify(detailsRepository, org.mockito.Mockito.times(2))
                .save(any(LinenRequestDetails.class));
    }

    @Test
    void raiseRequest_defaultUnitPrefix() throws Exception {
        LinenRequest r = service.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), null, null);
        assertThat(r.getRequisitionNo()).startsWith("IHMS/LR/");
    }

    @Test
    void raiseRequest_missingRequestNode_throws() {
        assertThatThrownBy(() -> service.raiseRequest(mapper.readTree("{\"details\":[]}"), 1L, null))
                .isInstanceOf(InvalidLinenRequestException.class);
    }

    @Test
    void approve_pendingToAccepted() {
        LinenRequest r = request(LinenStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThat(service.approve(1L).getRequestStatus()).isEqualTo(LinenStatus.ACCEPTED);
    }

    @Test
    void approve_wrongStatus_throws() {
        LinenRequest r = request(LinenStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(InvalidLinenRequestException.class)
                .hasMessageContaining("approve");
    }

    @Test
    void handover_requiresHandedOverTo() {
        LinenRequest r = request(LinenStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.handover(1L, " "))
                .isInstanceOf(InvalidLinenRequestException.class);
    }

    @Test
    void handover_acceptedToHandovered_stampsHandedOverTo() {
        LinenRequest r = request(LinenStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequest out = service.handover(1L, "LaundryA");
        assertThat(out.getRequestStatus()).isEqualTo(LinenStatus.HANDOVERED);
        assertThat(out.getHandedOverTo()).isEqualTo("LaundryA");
    }

    @Test
    void receiveMainStore_handoveredToReceived_stampsQuantities() throws Exception {
        LinenRequest r = request(LinenStatus.HANDOVERED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequestDetails line = detail(11L, 1L);
        when(detailsRepository.findById(11L)).thenReturn(Optional.of(line));

        LinenRequest out = service.receiveMainStore(1L, mapper.readTree(
                "[{\"id\":11,\"receivedQuantity\":10,\"damagedQuantity\":2,\"reason\":\"torn\"}]"));
        assertThat(out.getRequestStatus()).isEqualTo(LinenStatus.RECEIVED);
        assertThat(line.getReceivedQuantity()).isEqualTo(10L);
        assertThat(line.getDamagedQuantity()).isEqualTo(2L);
        assertThat(line.getReason()).isEqualTo("torn");
    }

    @Test
    void process_receivedToWashed() {
        LinenRequest r = request(LinenStatus.RECEIVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThat(service.process(1L).getRequestStatus()).isEqualTo(LinenStatus.WASHED);
    }

    @Test
    void dispatch_washedToDispatched_accumulatesDamaged() throws Exception {
        LinenRequest r = request(LinenStatus.WASHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequestDetails line = detail(11L, 1L);
        line.setDamagedQuantity(2L); // receive-stage damage
        when(detailsRepository.findById(11L)).thenReturn(Optional.of(line));

        LinenRequest out = service.dispatch(1L, mapper.readTree(
                "[{\"id\":11,\"dispatchedQuantity\":8,\"damagedQuantity\":1,\"reason\":\"stained\"}]"));
        assertThat(out.getRequestStatus()).isEqualTo(LinenStatus.DISPATCHED);
        assertThat(line.getDispatchedQuantity()).isEqualTo(8L);
        assertThat(line.getDamagedQuantity()).isEqualTo(3L); // 1 + previous 2 (legacy accumulation)
        assertThat(line.getReason()).isEqualTo("stained");
    }

    @Test
    void complete_dispatchedToCompleted_stampsFinalQuantityAndReceivedBy() throws Exception {
        LinenRequest r = request(LinenStatus.DISPATCHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequestDetails line = detail(11L, 1L);
        when(detailsRepository.findById(11L)).thenReturn(Optional.of(line));

        LinenRequest out = service.complete(1L, "WardNurse", mapper.readTree(
                "[{\"id\":11,\"finalQuantity\":9}]"));
        assertThat(out.getRequestStatus()).isEqualTo(LinenStatus.COMPLETED);
        assertThat(out.getReceivedBy()).isEqualTo("WardNurse");
        assertThat(line.getFinalQuantity()).isEqualTo(9L);
    }

    @Test
    void complete_wrongStatus_throws() {
        LinenRequest r = request(LinenStatus.RECEIVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.complete(1L, "X", null))
                .isInstanceOf(InvalidLinenRequestException.class)
                .hasMessageContaining("complete");
    }

    @Test
    void deleteRequest_softDelete() {
        LinenRequest r = request(LinenStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequest out = service.deleteRequest(1L);
        assertThat(out.getStatus()).isFalse();
        assertThat(out.getRequestStatus()).isEqualTo(0L);
    }

    @Test
    void updateDetails_requiresPending() {
        LinenRequest r = request(LinenStatus.ACCEPTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.updateDetails(1L, mapper.readTree("[]")))
                .isInstanceOf(InvalidLinenRequestException.class);
    }

    @Test
    void updateDetails_stampsQuantityAndRemark() throws Exception {
        LinenRequest r = request(LinenStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequestDetails line = detail(11L, 1L);
        when(detailsRepository.findById(11L)).thenReturn(Optional.of(line));

        List<LinenRequestDetails> updated = service.updateDetails(1L, mapper.readTree(
                "[{\"id\":11,\"quantity\":5,\"remark\":\"extra\"}]"));
        assertThat(updated).hasSize(1);
        assertThat(line.getQuantity()).isEqualTo(5L);
        assertThat(line.getRemark()).isEqualTo("extra");
    }

    @Test
    void updateDetails_wrongRequest_throws() throws Exception {
        LinenRequest r = request(LinenStatus.PENDING);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(r));
        LinenRequestDetails foreign = detail(11L, 99L); // belongs to request 99
        when(detailsRepository.findById(11L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.updateDetails(1L, mapper.readTree("[{\"id\":11,\"quantity\":5}]")))
                .isInstanceOf(InvalidLinenRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getRequest_unknown_throws404() {
        when(requestRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRequest(404L))
                .isInstanceOf(LinenNotFoundException.class);
    }

    // -------------------------------------------------------------- helpers

    private LinenRequest request(long status) {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setUnitId(7L);
        r.setRequisitionNo("IHMS/LR/26/00001");
        r.setRequestStatus(status);
        r.setStatus(true);
        return r;
    }

    private LinenRequestDetails detail(long id, long requestId) {
        LinenRequestDetails d = new LinenRequestDetails();
        d.setId(id);
        d.setLinenRequestId(requestId);
        d.setQuantity(10L);
        d.setStatus(true);
        return d;
    }
}

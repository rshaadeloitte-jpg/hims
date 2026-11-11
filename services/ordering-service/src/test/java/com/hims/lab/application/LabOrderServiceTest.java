package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
import com.hims.lab.domain.model.PathoPatientParameterDetails;
import com.hims.lab.domain.repository.PathOrderBookingRepository;
import com.hims.lab.domain.repository.PathOrderDetailRepository;
import com.hims.lab.domain.repository.PathoPatientParameterDetailsRepository;
import com.hims.lab.infrastructure.outbox.OutboxEvent;
import com.hims.lab.infrastructure.outbox.OutboxPublisher;
import com.hims.lab.infrastructure.outbox.OutboxService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — status machine, result upsert guards and the E-05 contract, no Spring context.
 * Key register assertion: LabResultFinalized is emitted ONLY when the WHOLE booking
 * finalizes (never per-detail), type string 'LabResultFinalized',
 * partition key encounterId, payload keys exactly per catalog.
 */
class LabOrderServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PathOrderBookingRepository bookingRepository;
    private PathOrderDetailRepository detailRepository;
    private PathoPatientParameterDetailsRepository parameterRepository;
    private OutboxService outboxService;
    private EntityManager em;
    private LabOrderService service;
    private long nextSeq = 500;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(PathOrderBookingRepository.class);
        detailRepository = mock(PathOrderDetailRepository.class);
        parameterRepository = mock(PathoPatientParameterDetailsRepository.class);
        outboxService = mock(OutboxService.class);
        em = mock(EntityManager.class);
        service = new LabOrderService(bookingRepository, detailRepository,
                parameterRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> nextSeq++);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(parameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------- fixtures

    private PathOrderDetail detail(long id, long bookingId) {
        PathOrderDetail d = new PathOrderDetail();
        d.setDetailId(id);
        d.setBookingId(bookingId);
        d.setTestCode("T" + id);
        d.setTestName("Test " + id);
        d.setDetailStatus(PathOrderBooking.STATUS_IN_PROGRESS);
        return d;
    }

    private void stubDetails(long bookingId, PathOrderDetail... details) {
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(bookingId)).thenReturn(List.of(details));
        long open = List.of(details).stream().filter(d -> !Boolean.TRUE.equals(d.getIsFinal())).count();
        when(detailRepository.countByBookingIdAndIsFinalFalse(bookingId)).thenReturn(open);
        when(detailRepository.countByBookingId(bookingId)).thenReturn((long) details.length);
        for (PathOrderDetail d : details) {
            when(detailRepository.findById(d.getDetailId()))
                    .thenReturn(Optional.of(d));
        }
    }

    // ------------------------------------------------------------- creation

    @Test
    void create_requiresPatientId() {
        assertThatThrownBy(() -> service.create(null, 9L, null,
                        List.of(new LabOrderService.TestLine("CBC", "Complete Blood Count"))))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_REQ_INVALID");
    }

    @Test
    void create_requiresAtLeastOneTestLine() {
        assertThatThrownBy(() -> service.create(11L, null, null, List.of()))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("test line");
    }

    @Test
    void create_assignsLabPrefixedBookingNo_andOrderedStatus() {
        java.util.concurrent.atomic.AtomicReference<PathOrderBooking> savedRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            savedRef.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(bookingRepository.findById(anyLong()))
                .thenAnswer(inv -> java.util.Optional.ofNullable(savedRef.get()));
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(anyLong())).thenReturn(List.of());

        PathOrderBooking b = service.create(11L, 77L, true,
                List.of(new LabOrderService.TestLine("CBC", "Complete Blood Count")));
        assertThat(b.getBookingNo()).isEqualTo("LAB-" + b.getBookingId());
        assertThat(b.getBookingStatus()).isEqualTo(PathOrderBooking.STATUS_ORDERED);
        assertThat(b.getIsOutSourced()).isTrue();
    }

    // ------------------------------------------------------------- status machine

    @Test
    void collect_then_start_followsTheMachine() {
        PathOrderDetail d = detail(21L, 1L);
        stubDetails(1L, d);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L)));

        service.collect(1L);
        assertThat(d.getDetailStatus()).isEqualTo(PathOrderBooking.STATUS_SAMPLE_COLLECTED);

        service.start(1L);
        assertThat(d.getDetailStatus()).isEqualTo(PathOrderBooking.STATUS_IN_PROGRESS);
    }

    @Test
    void collect_twice_isIllegalTransition_400() {
        PathOrderDetail d = detail(21L, 1L);
        d.setDetailStatus(PathOrderBooking.STATUS_SAMPLE_COLLECTED);   // already collected
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_SAMPLE_COLLECTED)));
        stubDetails(1L, d);

        assertThatThrownBy(() -> service.collect(1L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
    }

    @Test
    void start_fromOrdered_skipsSampleCollection_illegal() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L)));
        stubDetails(1L, detail(21L, 1L));

        assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("start");
    }

    @Test
    void cancel_finalizedBooking_illegal() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_FINALIZED)));
        stubDetails(1L, detail(21L, 1L));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
    }

    @Test
    void cancel_inProgress_allowed_andMirrorsDetails() {
        PathOrderDetail d = detail(21L, 1L);
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        stubDetails(1L, d);

        service.cancel(1L);
        assertThat(d.getDetailStatus()).isEqualTo(PathOrderBooking.STATUS_CANCELLED);
    }

    // ------------------------------------------------------------- results

    @Test
    void upsertResults_onCancelledBooking_rejected() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_CANCELLED)));

        assertThatThrownBy(() -> service.upsertResults(1L, 21L,
                List.of(new LabOrderService.ResultRow("Hb", "13.5", "g/dL", null))))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void upsertResults_afterDetailFinalized_locked() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        PathOrderDetail d = detail(21L, 1L);
        d.setIsFinal(true);
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.upsertResults(1L, 21L,
                List.of(new LabOrderService.ResultRow("Hb", "13.5", "g/dL", null))))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void upsertResults_detailOfAnotherBooking_rejected() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        PathOrderDetail other = detail(99L, 2L);
        when(detailRepository.findById(99L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.upsertResults(1L, 99L,
                List.of(new LabOrderService.ResultRow("Hb", "13.5", "g/dL", null))))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void upsertResults_reEntry_updatesExistingRow_caseInsensitive() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        stubDetails(1L, detail(21L, 1L));
        PathoPatientParameterDetails existing = param(31L, 21L, "hb");
        when(parameterRepository.findByDetailIdAndParameterNameIgnoreCase(21L, "Hb"))
                .thenReturn(Optional.of(existing));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L))
                .thenReturn(List.of(existing));

        service.upsertResults(1L, 21L, List.of(new LabOrderService.ResultRow("Hb", "14.2", "g/dL", "12-16")));

        assertThat(existing.getValue()).isEqualTo("14.2");
        assertThat(existing.getReferenceRange()).isEqualTo("12-16");
        assertThat(existing.getIsFinal()).isFalse();
    }

    // ------------------------------------------------------------- finalize + E-05

    @Test
    void finalize_beforeStart_illegalTransition() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L)));   // ORDERED
        stubDetails(1L, detail(21L, 1L));

        assertThatThrownBy(() -> service.finalizeDetail(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalize_withoutParameters_rejected_noEvent() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        PathOrderDetail d = detail(21L, 1L);
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.finalizeDetail(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("no result parameters");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void finalize_firstOfTwoDetails_noEventYet() throws Exception {
        PathOrderDetail d1 = detail(21L, 1L);
        PathOrderDetail d2 = detail(22L, 1L);
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d1));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L))
                .thenReturn(List.of(param(31L, 21L, "Hb")));

        // after this finalize, one detail remains open
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(1L)).thenReturn(List.of(d1, d2));
        when(detailRepository.countByBookingIdAndIsFinalFalse(1L)).thenReturn(1L);

        service.finalizeDetail(1L, 21L);

        assertThat(d1.getIsFinal()).isTrue();
        assertThat(d1.getFinalizedAt()).isNotNull();
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());   // NOT all final yet
    }

    @Test
    void finalize_lastDetail_flipsHeader_andEmitsExactE05_once() throws Exception {
        PathOrderDetail d1 = detail(21L, 1L);
        d1.setIsFinal(true);
        PathOrderDetail d2 = detail(22L, 1L);
        PathOrderBooking booking = newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS);
        booking.setEncounterId(7777L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(detailRepository.findById(22L)).thenReturn(Optional.of(d2));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(22L))
                .thenReturn(List.of(
                        param(32L, 22L, "Glucose"),
                        param(33L, 22L, "Cholesterol")));
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(1L)).thenReturn(List.of(d1, d2));
        when(detailRepository.countByBookingIdAndIsFinalFalse(1L))
                .thenAnswer(inv -> d2.getIsFinal() ? 0L : 1L);   // flips to 0 once d2 finalized in-place
        when(parameterRepository.findByDetailIdInOrderByParamIdAsc(any()))
                .thenReturn(List.of(param(31L, 21L, "Hb"), param(32L, 22L, "Glucose"),
                        param(33L, 22L, "Cholesterol")));

        PathOrderBooking result = service.finalizeDetail(1L, 22L);

        assertThat(result.getBookingStatus()).isEqualTo(PathOrderBooking.STATUS_FINALIZED);
        assertThat(result.getFinalizedAt()).isNotNull();

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> aggregateId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idempotencyKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(type.capture(), aggregateType.capture(),
                aggregateId.capture(), partitionKey.capture(), payloadJson.capture(),
                idempotencyKey.capture());

        assertThat(type.getValue()).isEqualTo("LabResultFinalized");                 // EXACT type string (E-05)
        assertThat(aggregateType.getValue()).isEqualTo("labOrder");
        assertThat(aggregateId.getValue()).isEqualTo(1L);
        assertThat(partitionKey.getValue()).isEqualTo("7777");                       // partition key encounterId
        assertThat(idempotencyKey.getValue()).isEqualTo("lab-result-finalized:1");

        JsonNode payload = mapper.readTree(payloadJson.getValue());
        assertThat(payload.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("bookingId", "detailIds", "patientId",
                        "encounterId", "results", "status", "finalizedAt");
        assertThat(payload.get("detailIds"))
                .extracting(JsonNode::asText)
                .containsExactly("21", "22");
        assertThat(payload.get("results")).hasSize(3);
        assertThat(payload.get("results").get(0).fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("parameterName", "value", "unit");
        assertThat(payload.get("status").asText()).isEqualTo("FINALIZED");
    }

    @Test
    void emit_partitionKeyFallsBack_whenEncounterNull() {
        PathOrderBooking booking = newBooking(9L, PathOrderBooking.STATUS_IN_PROGRESS);
        booking.setEncounterId(null);
        when(parameterRepository.findByDetailIdInOrderByParamIdAsc(any()))
                .thenReturn(List.of(param(31L, 21L, "Hb")));

        service.emitLabResultFinalized(booking, List.of(detail(21L, 9L)), Timestamp.from(java.time.Instant.now()));

        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(any(), any(), any(), partitionKey.capture(), any(), any());
        assertThat(partitionKey.getValue()).startsWith("no-encounter:");   // documented fallback (column NOT NULL)
    }

    // ------------------------------------------------------------- M15 critical detection

    @Test
    void isCriticalValue_numericOutsideRange_isCritical() {
        assertThat(LabOrderService.isCriticalValue("5", "10-20")).isTrue();
        assertThat(LabOrderService.isCriticalValue("25", "10-20")).isTrue();
        assertThat(LabOrderService.isCriticalValue("15", "10-20")).isFalse();
        assertThat(LabOrderService.isCriticalValue("abc", "10-20")).isFalse();
        assertThat(LabOrderService.isCriticalValue("15", null)).isFalse();
        assertThat(LabOrderService.isCriticalValue("15", "N/A")).isFalse();
    }

    @Test
    void upsertResults_setsCriticalFlag_whenOutOfRange() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        PathOrderDetail d = detail(21L, 1L);
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        when(parameterRepository.findByDetailIdAndParameterNameIgnoreCase(eq(21L), any()))
                .thenReturn(Optional.empty());
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L))
                .thenReturn(List.of(param(31L, 21L, "Hb")));

        service.upsertResults(1L, 21L,
                List.of(new LabOrderService.ResultRow("Hb", "5", "g/dL", "12-16")));

        ArgumentCaptor<PathoPatientParameterDetails> captor = ArgumentCaptor.forClass(PathoPatientParameterDetails.class);
        verify(parameterRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getIsCritical()).isTrue();
    }

    @Test
    void upsertResults_critical_clearsOnInRangeUpdate() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        PathOrderDetail d = detail(21L, 1L);
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        PathoPatientParameterDetails existing = param(31L, 21L, "Hb");
        existing.setIsCritical(true);
        when(parameterRepository.findByDetailIdAndParameterNameIgnoreCase(21L, "Hb"))
                .thenReturn(Optional.of(existing));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L))
                .thenReturn(List.of(existing));

        service.upsertResults(1L, 21L,
                List.of(new LabOrderService.ResultRow("Hb", "14", "g/dL", "12-16")));

        assertThat(existing.getIsCritical()).isFalse();
    }

    // ------------------------------------------------------------- M15 barcode / accept / reject / verify / notify

    @Test
    void generateBarcode_idempotent_secondCallReturnsSameBarcode() {
        PathOrderDetail d = detail(21L, 1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        PathOrderDetail first = service.generateBarcode(1L, 21L);
        String barcode1 = first.getBarcode();
        assertThat(barcode1).isNotBlank();
        assertThat(first.getSampleNo()).isEqualTo(barcode1);

        PathOrderDetail second = service.generateBarcode(1L, 21L);
        assertThat(second.getBarcode()).isEqualTo(barcode1);
    }

    @Test
    void generateBarcode_onCancelledBooking_rejected() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_CANCELLED)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(detail(21L, 1L)));

        assertThatThrownBy(() -> service.generateBarcode(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
    }

    @Test
    void acceptSample_requiresSampleNo_andGuardsRejected() {
        PathOrderDetail d = detail(21L, 1L);
        d.setBarcode("2026080000001");
        d.setSampleNo("2026080000001");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_SAMPLE_COLLECTED)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        PathOrderDetail accepted = service.acceptSample(1L, 21L, "OK");
        assertThat(accepted.getIsAccepted()).isTrue();
        assertThat(accepted.getAcceptanceDate()).isNotNull();

        // second accept is illegal
        assertThatThrownBy(() -> service.acceptSample(1L, 21L, "again"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("already accepted");

        // rejected detail cannot be accepted
        PathOrderDetail rejected = detail(22L, 1L);
        rejected.setSampleNo("S22");
        rejected.setIsRejected(true);
        when(detailRepository.findById(22L)).thenReturn(Optional.of(rejected));
        assertThatThrownBy(() -> service.acceptSample(1L, 22L, "x"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("already rejected");
    }

    @Test
    void acceptSample_withoutBarcode_rejected() {
        PathOrderDetail d = detail(21L, 1L);
        // no sample_no / barcode
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_SAMPLE_COLLECTED)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.acceptSample(1L, 21L, null))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("Sample number required");
    }

    @Test
    void rejectSample_requiresReason_andClearsFinalize() {
        PathOrderDetail d = detail(21L, 1L);
        d.setSampleNo("S1");
        d.setIsFinal(true);
        d.setIsVerified(false);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.rejectSample(1L, 21L, " "))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("rejectionReason");

        PathOrderDetail rejected = service.rejectSample(1L, 21L, "hemolysed");
        assertThat(rejected.getIsRejected()).isTrue();
        assertThat(rejected.getRejectionReason()).isEqualTo("hemolysed");
        assertThat(rejected.getIsFinal()).isFalse();
        assertThat(rejected.getIsVerified()).isFalse();
    }

    @Test
    void rejectSample_afterVerified_isIllegal() {
        PathOrderDetail d = detail(21L, 1L);
        d.setSampleNo("S1");
        d.setIsFinal(true);
        d.setIsVerified(true);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.rejectSample(1L, 21L, "x"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    void verifyDetail_requiresFinal_andGuardsAlreadyVerified() {
        PathOrderDetail d = detail(21L, 1L);
        d.setIsFinal(false);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.verifyDetail(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("must be finalized");

        d.setIsFinal(true);
        d.setIsRejected(true);
        assertThatThrownBy(() -> service.verifyDetail(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("Rejected");

        d.setIsRejected(false);
        d.setIsVerified(false);
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(1L)).thenReturn(List.of(d));
        PathOrderDetail verified = service.verifyDetail(1L, 21L);
        assertThat(verified.getIsVerified()).isTrue();

        assertThatThrownBy(() -> service.verifyDetail(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    void verifyDetail_lastOfTwo_emitsLabResultVerified_once() {
        PathOrderDetail d1 = detail(21L, 1L);
        d1.setIsFinal(true);
        d1.setIsVerified(true);
        PathOrderDetail d2 = detail(22L, 1L);
        d2.setIsFinal(true);
        d2.setIsVerified(false);
        PathOrderBooking booking = newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS);
        booking.setEncounterId(777L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(detailRepository.findById(22L)).thenReturn(Optional.of(d2));
        when(detailRepository.findByBookingIdOrderByDetailIdAsc(1L)).thenReturn(List.of(d1, d2));

        service.verifyDetail(1L, 22L);

        verify(outboxService, times(1)).append(
                eq("LabResultVerified"), any(), eq(1L), any(), any(), eq("lab-result-verified:1"));
    }

    @Test
    void notifyCritical_requiresCriticalParam_andEmitsAlert() {
        PathOrderDetail d = detail(21L, 1L);
        d.setBarcode("2026080000001");
        d.setSampleNo("2026080000001");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L, PathOrderBooking.STATUS_IN_PROGRESS)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        PathoPatientParameterDetails crit = param(31L, 21L, "Hb");
        crit.setValue("5");
        crit.setReferenceRange("12-16");
        crit.setIsCritical(true);
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of(crit));
        when(parameterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.notifyCritical(1L, 21L);

        assertThat(crit.getCriticalNotifiedAt()).isNotNull();
        verify(outboxService).append(eq("LabCriticalAlert"), any(), eq(1L), any(), any(), contains("lab-critical-alert:21"));
    }

    @Test
    void notifyCritical_withoutCriticalParam_rejected() {
        PathOrderDetail d = detail(21L, 1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(newBooking(1L)));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        PathoPatientParameterDetails normal = param(31L, 21L, "Hb");
        normal.setIsCritical(false);
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of(normal));

        assertThatThrownBy(() -> service.notifyCritical(1L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("no critical");
    }

    // ------------------------------------------------------------- helpers

    private static PathOrderBooking newBooking(long id) {
        return newBooking(id, PathOrderBooking.STATUS_ORDERED);
    }

    private static PathOrderBooking newBooking(long id, short status) {
        PathOrderBooking b = new PathOrderBooking();
        b.setBookingId(id);
        b.setPatientId(505L);
        b.setBookingNo("LAB-" + id);
        b.setBookingStatus(status);
        return b;
    }

    private static PathoPatientParameterDetails param(long id, long detailId, String name) {
        PathoPatientParameterDetails p = new PathoPatientParameterDetails();
        p.setParamId(id);
        p.setDetailId(detailId);
        p.setParameterName(name);
        p.setValue("10");
        p.setUnit("mg/dL");
        return p;
    }
}

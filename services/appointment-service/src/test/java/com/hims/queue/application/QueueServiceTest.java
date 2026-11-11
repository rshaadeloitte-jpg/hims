package com.hims.queue.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.queue.application.exceptions.InvalidQueueRequestException;
import com.hims.queue.application.exceptions.QueueNotFoundException;
import com.hims.queue.domain.model.Counter;
import com.hims.queue.domain.model.QueueToken;
import com.hims.queue.domain.repository.CounterRepository;
import com.hims.queue.domain.repository.DailyCounterRepository;
import com.hims.queue.domain.repository.QueueTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** L0 — token lifecycle business rules: daily counter, state machine guards, priority call-next. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueueServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.now();

    @Mock private QueueTokenRepository tokenRepository;
    @Mock private CounterRepository counterRepository;
    @Mock private DailyCounterRepository dailyCounterRepository;
    @Mock private EntityManager em;

    private QueueService service;
    private final java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong();

    @BeforeEach
    void setUp() {
        service = new QueueService(tokenRepository, counterRepository, dailyCounterRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        jakarta.persistence.Query seqQuery = org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong idSeq = new java.util.concurrent.atomic.AtomicLong();
        when(seqQuery.getSingleResult()).thenAnswer(inv -> idSeq.incrementAndGet() * 100);   // id sequence values
        jakarta.persistence.Query bumpQuery = org.mockito.Mockito.mock(jakarta.persistence.Query.class);
        when(bumpQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());           // daily counter bump
        when(bumpQuery.executeUpdate()).thenReturn(1);
        when(bumpQuery.setParameter(org.mockito.ArgumentMatchers.anyString(), any())).thenReturn(bumpQuery);
        when(seqQuery.setParameter(org.mockito.ArgumentMatchers.anyString(), any())).thenReturn(seqQuery);
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("daily_counter"))).thenReturn(bumpQuery);
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("counter_seq"))).thenReturn(seqQuery);
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("queue_token_seq"))).thenReturn(seqQuery);
        when(tokenRepository.save(any(QueueToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(counterRepository.save(any(Counter.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Counter counter(long id, long dept) {
        Counter c = new Counter();
        c.setId(id);
        c.setCode("C" + id);
        c.setName("Counter " + id);
        c.setDepartmentId(dept);
        c.setStatus(true);
        return c;
    }

    private QueueToken waiting(long id, long dept, short priority, long tokenNo) {
        QueueToken t = new QueueToken();
        t.setId(id);
        t.setQueueDate(TODAY);
        t.setTokenNo(tokenNo);
        t.setPatientId(id * 10);
        t.setDepartmentId(dept);
        t.setPriority(priority);
        t.setQueueStatus(QueueToken.WAITING);
        return t;
    }

    // ------------------------------------------------------------------ issue

    @Test
    void issue_stampsDailyNoWaitingAndAudit() throws Exception {
        QueueToken t = service.issue(mapper.readTree("{\"patientId\":501,\"departmentId\":9}"), 7L);
        assertThat(t.getTokenNo()).isEqualTo(1L);
        assertThat(t.getQueueDate()).isEqualTo(TODAY);
        assertThat(t.getQueueStatus()).isEqualTo(QueueToken.WAITING);
        assertThat(t.getPriority()).isEqualTo(QueueToken.PRIORITY_NORMAL);
        assertThat(t.getAddedBy()).isEqualTo(QueueService.AUDIT_ACTOR_ID);
        assertThat(t.getUnitId()).isEqualTo(7L);
    }

    @Test
    void issue_dailyNumbersIncrementWithinSameDay() throws Exception {
        service.issue(mapper.readTree("{\"patientId\":1,\"departmentId\":9}"), null);
        QueueToken second = service.issue(mapper.readTree("{\"patientId\":2,\"departmentId\":9}"), null);
        assertThat(second.getTokenNo()).isEqualTo(2L);
    }

    @Test
    void issue_rejectsMissingPatientOrDepartment() {
        assertThatThrownBy(() -> service.issue(mapper.readTree("{\"departmentId\":9}"), null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("patientId");
        assertThatThrownBy(() -> service.issue(mapper.readTree("{\"patientId\":1}"), null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("departmentId");
    }

    @Test
    void issue_rejectsBadPriorityAndServerOwnedStatus() throws Exception {
        assertThatThrownBy(() -> service.issue(
                mapper.readTree("{\"patientId\":1,\"departmentId\":9,\"priority\":5}"), null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("priority");
        assertThatThrownBy(() -> service.issue(
                mapper.readTree("{\"patientId\":1,\"departmentId\":9,\"queueStatus\":3}"), null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("server-owned");
    }

    @Test
    void issue_nullPayloadRejected() {
        assertThatThrownBy(() -> service.issue(null, null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
    }

    // -------------------------------------------------------------- call next

    @Test
    void callNext_emergencyJumpsAheadOfNormal_fifoWithinPriority() {
        Counter c = counter(1, 9);
        when(counterRepository.findById(1L)).thenReturn(Optional.of(c));
        QueueToken normalFirst = waiting(11, 9, QueueToken.PRIORITY_NORMAL, 1);
        QueueToken emergencyLate = waiting(12, 9, QueueToken.PRIORITY_EMERGENCY, 3);
        when(tokenRepository.findNext(eq(TODAY), eq(9L), eq(QueueToken.WAITING), any(Pageable.class)))
                .thenReturn(List.of(emergencyLate));

        QueueToken called = service.callNext(1L);
        assertThat(called.getId()).isEqualTo(12L);   // emergency jumped the queue
        assertThat(called.getQueueStatus()).isEqualTo(QueueToken.CALLED);
        assertThat(called.getCalledAt()).isNotNull();
        assertThat(called.getCounterId()).isEqualTo(1L);
    }

    @Test
    void callNext_emptyQueueRejected() {
        when(counterRepository.findById(1L)).thenReturn(Optional.of(counter(1, 9)));
        when(tokenRepository.findNext(eq(TODAY), eq(9L), eq(QueueToken.WAITING), any(Pageable.class)))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.callNext(1L))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void callNext_unknownOrDeactivatedCounterRejected() {
        when(counterRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.callNext(99L))
                .isInstanceOf(QueueNotFoundException.class);

        Counter dead = counter(2, 9);
        dead.setStatus(false);
        when(counterRepository.findById(2L)).thenReturn(Optional.of(dead));
        assertThatThrownBy(() -> service.callNext(2L))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("deactivated");
    }

    // --------------------------------------------------------------- transfer

    @Test
    void transfer_movesToTargetAndRequeues_calledTokenMustBeRecalled() throws Exception {
        QueueToken called = waiting(21, 9, QueueToken.PRIORITY_NORMAL, 5);
        called.setCounterId(1L);
        called.setQueueStatus(QueueToken.CALLED);
        when(tokenRepository.findById(21L)).thenReturn(Optional.of(called));
        when(counterRepository.findById(2L)).thenReturn(Optional.of(counter(2, 10)));

        QueueToken moved = service.transfer(21L, mapper.readTree("{\"toCounterId\":2}"));
        assertThat(moved.getCounterId()).isEqualTo(2L);
        assertThat(moved.getQueueStatus()).isEqualTo(QueueToken.WAITING);   // re-queued at target
        assertThat(moved.getCalledAt()).isNull();
    }

    @Test
    void transfer_rejectsTerminalTokensAndUnknownTarget() throws Exception {
        when(counterRepository.findById(2L)).thenReturn(Optional.of(counter(2, 10)));
        QueueToken done = waiting(22, 9, QueueToken.PRIORITY_NORMAL, 6);
        done.setQueueStatus(QueueToken.COMPLETED);
        when(tokenRepository.findById(22L)).thenReturn(Optional.of(done));
        assertThatThrownBy(() -> service.transfer(22L, mapper.readTree("{\"toCounterId\":2}")))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("terminal");

        QueueToken live = waiting(23, 9, QueueToken.PRIORITY_NORMAL, 7);
        when(tokenRepository.findById(23L)).thenReturn(Optional.of(live));
        when(counterRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transfer(23L, mapper.readTree("{\"toCounterId\":404}")))
                .isInstanceOf(QueueNotFoundException.class);
    }

    // ------------------------------------------------------------ no-show etc

    @Test
    void noShow_requiresCalledStatus_forwardOnly() throws Exception {
        QueueToken waitingTok = waiting(31, 9, QueueToken.PRIORITY_NORMAL, 8);
        when(tokenRepository.findById(31L)).thenReturn(Optional.of(waitingTok));
        assertThatThrownBy(() -> service.markNoShow(31L))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("status");

        waitingTok.setQueueStatus(QueueToken.CALLED);
        QueueToken out = service.markNoShow(31L);
        assertThat(out.getQueueStatus()).isEqualTo(QueueToken.NO_SHOW);
        assertThat(out.getCompletedAt()).isNotNull();
    }

    @Test
    void complete_requiresCalledStatus() {
        QueueToken called = waiting(32, 9, QueueToken.PRIORITY_NORMAL, 9);
        called.setQueueStatus(QueueToken.CALLED);
        when(tokenRepository.findById(32L)).thenReturn(Optional.of(called));

        QueueToken out = service.complete(32L);
        assertThat(out.getQueueStatus()).isEqualTo(QueueToken.COMPLETED);
        assertThat(out.getCompletedAt()).isNotNull();
    }

    @Test
    void unknownTokenActionsReturn404() {
        when(tokenRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(999L)).isInstanceOf(QueueNotFoundException.class);
        assertThatThrownBy(() -> service.markNoShow(999L)).isInstanceOf(QueueNotFoundException.class);
    }

    // ---------------------------------------------------------------- masters

    @Test
    void createCounter_mapsDoctorAndDepartment() throws Exception {
        Counter c = service.createCounter(
                mapper.readTree("{\"code\":\"OPD-1\",\"name\":\"OPD Counter 1\",\"departmentId\":9,\"doctorId\":77}"), 7L);
        assertThat(c.getId()).isPositive();
        assertThat(c.getDepartmentId()).isEqualTo(9L);
        assertThat(c.getDoctorId()).isEqualTo(77L);
        assertThat(c.getStatus()).isTrue();
    }

    @Test
    void createCounter_requiresCodeAndName() throws Exception {
        assertThatThrownBy(() -> service.createCounter(mapper.readTree("{\"name\":\"no code\"}"), null))
                .isInstanceOf(InvalidQueueRequestException.class)
                .hasMessageContaining("code");
    }

    // ------------------------------------------------------------ M27 department-specific (rad/patho) + cabin

    @Test
    void listTokensByDepartment_filtersTodayByDept() {
        when(tokenRepository.findByQueueDateAndDepartmentIdAndStatusTrueOrderByIdAsc(TODAY, 9001L))
                .thenReturn(List.of(waiting(1, 9001L, QueueToken.PRIORITY_NORMAL, 1)));
        var list = service.listTokensByDepartment(9001L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDepartmentId()).isEqualTo(9001L);
    }

    @Test
    void listTokensByDepartment_nullDeptRejected() {
        assertThatThrownBy(() -> service.listTokensByDepartment(null))
                .isInstanceOf(InvalidQueueRequestException.class);
    }

    @Test
    void listCountersByDepartment_returnsDeptCounters() {
        when(counterRepository.findByDepartmentIdAndStatusTrueOrderByIdAsc(9001L))
                .thenReturn(List.of(counter(9001L, 9001L)));
        assertThat(service.listCountersByDepartment(9001L)).hasSize(1);
    }

    @Test
    void dashboardByDepartment_countsPerDept() {
        when(tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(TODAY, 9001L, QueueToken.WAITING)).thenReturn(2L);
        when(tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(TODAY, 9001L, QueueToken.CALLED)).thenReturn(1L);
        when(tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(TODAY, 9001L, QueueToken.COMPLETED)).thenReturn(0L);
        when(tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(TODAY, 9001L, QueueToken.NO_SHOW)).thenReturn(0L);
        when(tokenRepository.findByDateDeptAndStatus(eq(TODAY), eq(9001L), eq(QueueToken.CALLED), any(Pageable.class))).thenReturn(List.of());
        when(tokenRepository.findByDateDeptAndStatus(eq(TODAY), eq(9001L), eq(QueueToken.WAITING), any(Pageable.class))).thenReturn(List.of());
        when(counterRepository.findByDepartmentIdAndStatusTrueOrderByIdAsc(9001L)).thenReturn(List.of(counter(9001L, 9001L)));
        var dash = service.dashboardByDepartment(9001L);
        assertThat(dash.get("departmentId")).isEqualTo(9001L);
        assertThat(dash.get("waiting")).isEqualTo(2L);
        assertThat(dash.get("counters")).isNotNull();
    }

    @Test
    void cabinBoard_returnsPerCabinTokens() {
        when(counterRepository.findById(1L)).thenReturn(java.util.Optional.of(counter(1L, 9001L)));
        when(tokenRepository.findByQueueDateAndCounterIdAndStatusTrueOrderByIdAsc(TODAY, 1L))
                .thenReturn(List.of(waiting(1, 9001L, QueueToken.PRIORITY_NORMAL, 1)));
        when(tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(TODAY, 9001L, QueueToken.WAITING)).thenReturn(1L);
        var board = service.cabinBoard(1L);
        assertThat(board.get("counterId")).isEqualTo(1L);
        assertThat(board.get("tokens")).isNotNull();
    }

    @Test
    void cabinBoard_unknownCounter404() {
        when(counterRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cabinBoard(99L)).isInstanceOf(QueueNotFoundException.class);
    }
}

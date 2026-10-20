package com.hims.linen.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.linen.LinenServiceApplication;
import com.hims.linen.application.LinenRequestService;
import com.hims.linen.domain.model.LinenRequest;
import com.hims.linen.domain.model.LinenRequestDetails;
import com.hims.linen.domain.model.LinenStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — MIG-023 linen service on real PG16: V1 schema applies (ihms_linen), and
 * the full 7-state request lifecycle (raise -> approve -> handover ->
 * receive-main-store -> process -> dispatch -> complete) with detail quantity
 * stamping, damaged-quantity accumulation, and the forward-only guards.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = LinenServiceApplication.class)
@DirtiesContext
@Transactional
class LinenServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LinenRequestService linenService;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void resetSchema() {
        // setval is non-transactional in PG: survives the test-tx rollback, so each
        // test deterministically starts ids at fixed bases.
        em.createNativeQuery("SELECT setval('ihms_linen.\"linen_request_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_linen.\"linen_request_details_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_linen.\"linen_request_req_no_seq\"', 3000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_linen.linen_request_details, ihms_linen.linen_request CASCADE""")
                    .executeUpdate();
        });
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_linen' AND table_name IN
                          ('linen_request','linen_request_details')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("linen_request", "linen_request_details");
    }

    @Test
    void fullLifecycle_raiseThroughComplete() throws Exception {
        LinenRequest r = linenService.raiseRequest(mapper.readTree("""
                {"request":{"storeId":10,"toStoreId":20},
                 "details":[{"itemId":1,"batchId":5,"quantity":10,"remark":"bedsheets"},
                            {"itemId":2,"quantity":4,"remark":"towels"}]}
                """), 7L, "RISL");

        assertThat(r.getRequisitionNo()).matches("RISL/LR/\\d{2}/\\d{5}");
        assertThat(r.getRequestStatus()).isEqualTo(LinenStatus.PENDING);

        List<LinenRequestDetails> details = linenService.getDetails(r.getId());
        assertThat(details).hasSize(2);
        assertThat(details.get(0).getQuantity()).isEqualTo(10L);

        // approve (em.clear(): REQUIRES_NEW writes commit outside the test tx, so the
        // test persistence context must be cleared to see the committed state)
        LinenRequest approved = linenService.approve(r.getId());
        assertThat(approved.getRequestStatus()).isEqualTo(LinenStatus.ACCEPTED);
        em.clear();

        // handover
        LinenRequest handed = linenService.handover(r.getId(), "LaundryA");
        assertThat(handed.getRequestStatus()).isEqualTo(LinenStatus.HANDOVERED);
        assertThat(handed.getHandedOverTo()).isEqualTo("LaundryA");
        em.clear();

        // receive at main store: line 1 damaged, line 2 clean
        LinenRequestDetails line1 = linenService.getDetails(r.getId()).get(0);
        LinenRequestDetails line2 = linenService.getDetails(r.getId()).get(1);
        LinenRequest received = linenService.receiveMainStore(r.getId(), mapper.readTree("""
                [{"id":%d,"receivedQuantity":9,"damagedQuantity":1,"reason":"torn"},
                 {"id":%d,"receivedQuantity":4}]
                """.formatted(line1.getId(), line2.getId())));
        assertThat(received.getRequestStatus()).isEqualTo(LinenStatus.RECEIVED);
        em.clear();
        LinenRequestDetails l1 = linenService.getDetails(r.getId()).get(0);
        assertThat(l1.getReceivedQuantity()).isEqualTo(9L);
        assertThat(l1.getDamagedQuantity()).isEqualTo(1L);
        assertThat(l1.getReason()).isEqualTo("torn");

        // process (wash)
        LinenRequest washed = linenService.process(r.getId());
        assertThat(washed.getRequestStatus()).isEqualTo(LinenStatus.WASHED);
        em.clear();

        // dispatch: line 1 accumulates damage (1 + 1 = 2)
        LinenRequestDetails dl1 = linenService.getDetails(r.getId()).get(0);
        LinenRequest dispatched = linenService.dispatch(r.getId(), mapper.readTree("""
                [{"id":%d,"dispatchedQuantity":8,"damagedQuantity":1,"reason":"stained"}]
                """.formatted(dl1.getId())));
        assertThat(dispatched.getRequestStatus()).isEqualTo(LinenStatus.DISPATCHED);
        em.clear();
        LinenRequestDetails dl1After = linenService.getDetails(r.getId()).get(0);
        assertThat(dl1After.getDispatchedQuantity()).isEqualTo(8L);
        assertThat(dl1After.getDamagedQuantity()).isEqualTo(2L); // accumulated

        // complete
        LinenRequestDetails cl1 = linenService.getDetails(r.getId()).get(0);
        LinenRequest completed = linenService.complete(r.getId(), "WardNurse", mapper.readTree("""
                [{"id":%d,"finalQuantity":8}]
                """.formatted(cl1.getId())));
        assertThat(completed.getRequestStatus()).isEqualTo(LinenStatus.COMPLETED);
        assertThat(completed.getReceivedBy()).isEqualTo("WardNurse");
        em.clear();
        LinenRequestDetails fl1 = linenService.getDetails(r.getId()).get(0);
        assertThat(fl1.getFinalQuantity()).isEqualTo(8L);

        // counts
        Map<String, Long> counts = linenService.getCounts(7L, 10L, null, null);
        assertThat(counts.get("TOTAL")).isEqualTo(1L);
        assertThat(counts.get("COMPLETED")).isEqualTo(1L);
    }

    @Test
    void softDelete_setsStatusFalseAndRequestStatusZero() throws Exception {
        LinenRequest r = linenService.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), 7L, null);
        LinenRequest deleted = linenService.deleteRequest(r.getId());
        assertThat(deleted.getStatus()).isFalse();
        assertThat(deleted.getRequestStatus()).isEqualTo(0L);
        // soft-deleted excluded from list
        assertThat(linenService.listRequests(7L, null, null, null, null, null)).isEmpty();
    }

    @Test
    void list_storeSeesSourceAndDestination() throws Exception {
        // store 10 is source; store 20 is destination
        linenService.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":10,\"toStoreId\":20},\"details\":[]}"), 7L, null);
        // store 10 sees it as source
        assertThat(linenService.listRequests(7L, 10L, null, null, null, null)).hasSize(1);
        // store 20 sees it as destination
        assertThat(linenService.listRequests(7L, 20L, null, null, null, null)).hasSize(1);
        // store 30 sees neither
        assertThat(linenService.listRequests(7L, 30L, null, null, null, null)).isEmpty();
    }

    @Test
    void search_matchesRequisitionNo() throws Exception {
        linenService.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), 7L, "RISL");
        assertThat(linenService.listRequests(7L, null, null, "RISL/LR", null, null)).hasSize(1);
        assertThat(linenService.listRequests(7L, null, null, "ZZZZ", null, null)).isEmpty();
    }

    @Test
    void forwardOnlyGuard_rejectsBackwardTransition() throws Exception {
        LinenRequest r = linenService.raiseRequest(mapper.readTree(
                "{\"request\":{\"storeId\":1},\"details\":[]}"), 7L, null);
        linenService.approve(r.getId());
        em.clear();
        // cannot approve again (already ACCEPTED)
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> linenService.approve(r.getId()))
                .isInstanceOf(com.hims.linen.application.exceptions.InvalidLinenRequestException.class);
    }
}

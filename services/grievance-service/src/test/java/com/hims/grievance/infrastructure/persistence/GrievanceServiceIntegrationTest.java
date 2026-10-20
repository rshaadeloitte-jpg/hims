package com.hims.grievance.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.grievance.GrievanceServiceApplication;
import com.hims.grievance.application.GrievanceMasterService;
import com.hims.grievance.application.GrievanceTicketService;
import com.hims.grievance.domain.model.GrievanceCategory;
import com.hims.grievance.domain.model.GrievanceComment;
import com.hims.grievance.domain.model.GrievanceFeedback;
import com.hims.grievance.domain.model.GrievanceSeverity;
import com.hims.grievance.domain.model.GrievanceSubCategory;
import com.hims.grievance.domain.model.GrievanceTicket;
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
 * L2 — MIG-020 grievance service on real PG16: V1 schema applies (ihms_grievance),
 * masters via sequences, and the full ticket lifecycle (raise -> comment -> assign ->
 * resolve -> reopen -> feedback-close) with the legacy status/close semantics.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = GrievanceServiceApplication.class)
@DirtiesContext
@Transactional
class GrievanceServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GrievanceMasterService masterService;

    @Autowired
    private GrievanceTicketService ticketService;

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
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_category_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_sub_category_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_severity_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_ticket_seq\"', 4000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_ticket_no_seq\"', 5000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_comment_seq\"', 6000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_feedback_seq\"', 7000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_grievance.\"grievance_document_seq\"', 8000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> em.createNativeQuery("""
                TRUNCATE ihms_grievance.grievance_document, ihms_grievance.grievance_feedback,
                         ihms_grievance.grievance_comment, ihms_grievance.grievance_ticket,
                         ihms_grievance.grievance_sub_category, ihms_grievance.grievance_severity,
                         ihms_grievance.grievance_category CASCADE""").executeUpdate());
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_grievance' AND table_name IN
                        ('grievance_category','grievance_sub_category','grievance_severity',
                         'grievance_ticket','grievance_comment','grievance_feedback','grievance_document')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("grievance_category", "grievance_comment", "grievance_document",
                "grievance_feedback", "grievance_severity", "grievance_sub_category", "grievance_ticket");
        List<String> seqs = em.createNativeQuery("""
                        SELECT sequencename FROM pg_sequences
                        WHERE schemaname = 'ihms_grievance' ORDER BY sequencename""").getResultList();
        assertThat(seqs).contains("grievance_ticket_seq", "grievance_ticket_no_seq");
    }

    @Test
    void masters_createAndHierarchy() throws Exception {
        GrievanceCategory cat = masterService.createCategory(
                mapper.readTree("{\"code\":\"FACILITY\",\"description\":\"Facility related\",\"isCitizen\":true}"), 7L);
        assertThat(cat.getId()).isEqualTo(1001L);
        assertThat(cat.getIsCitizen()).isTrue();

        GrievanceSubCategory sub = masterService.createSubCategory(
                mapper.readTree("{\"categoryId\":1001,\"code\":\"CLEAN\",\"description\":\"Cleanliness\"}"), 7L);
        assertThat(sub.getId()).isEqualTo(2001L);
        assertThat(sub.getCategoryId()).isEqualTo(1001L);

        GrievanceSeverity sev = masterService.createSeverity(
                mapper.readTree("{\"code\":\"HIGH\",\"description\":\"High priority\"}"), 7L);
        assertThat(sev.getId()).isEqualTo(3001L);
    }

    @Test
    void ticketLifecycle_raiseCommentAssignResolveReopenFeedback() throws Exception {
        // raise
        GrievanceTicket t = ticketService.raiseTicket(
                mapper.readTree("{\"name\":\"Sita\",\"issue\":\"Ward cleanliness\",\"mobileNo\":9123456780}"),
                7L, false);
        assertThat(t.getId()).isEqualTo(4001L);
        assertThat(t.getTicketNo()).matches("IHMS/G/\\d{4}/05001");
        assertThat(t.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_NEW);

        // comment
        GrievanceComment c = ticketService.addComment(t.getId(), "Housekeeping notified");
        assertThat(c.getId()).isEqualTo(6001L);
        assertThat(ticketService.getComments(t.getId())).hasSize(1);

        // assign (status stays NEW until explicit status change — legacy parity)
        GrievanceTicket assigned = ticketService.assignTicket(t.getId(), 42L,
                java.sql.Timestamp.valueOf(java.time.LocalDate.of(2027, 2, 1).atStartOfDay()),
                "GRIEVANCE", "IHMSSUPPORT");
        assertThat(assigned.getAssignedUserId()).isEqualTo(42L);
        assertThat(assigned.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_NEW);

        // explicit status -> ASSIGNED
        GrievanceTicket inProgress = ticketService.changeStatus(t.getId(), GrievanceTicketService.STATUS_ASSIGNED, false);
        assertThat(inProgress.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_ASSIGNED);

        // resolve -> closeStatus COMPLETED + resolvedDate
        GrievanceTicket resolved = ticketService.changeStatus(t.getId(), GrievanceTicketService.STATUS_RESOLVED, false);
        assertThat(resolved.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_RESOLVED);
        assertThat(resolved.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_COMPLETED);
        assertThat(resolved.getResolvedDate()).isNotNull();

        // reopen (raise-style) -> ASSIGNED + PENDING + reopened audit
        GrievanceTicket reopened = ticketService.reopenTicket(t.getId(), "Still dirty", true);
        assertThat(reopened.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_ASSIGNED);
        assertThat(reopened.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_PENDING);
        assertThat(reopened.getReopenedBy()).isNotNull();
        assertThat(ticketService.getComments(t.getId())).hasSize(2);

        // feedback -> closes ticket (CLOSED + feedbackId)
        GrievanceFeedback f = ticketService.addFeedback(
                mapper.readTree("{\"grievanceId\":" + t.getId() + ",\"question1\":4,\"question2\":5,\"freeTextFeedback\":\"ok\"}"));
        assertThat(f.getId()).isEqualTo(7001L);
        GrievanceTicket closed = ticketService.getTicket(t.getId());
        assertThat(closed.getFeedbackId()).isEqualTo(f.getId());
        assertThat(closed.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_CLOSED);
    }

    @Test
    void dashboardCounts_matchLegacyBuckets() throws Exception {
        ticketService.raiseTicket(mapper.readTree("{\"name\":\"A\",\"issue\":\"i1\"}"), 7L, false);
        GrievanceTicket t2 = ticketService.raiseTicket(mapper.readTree("{\"name\":\"B\",\"issue\":\"i2\"}"), 7L, true);
        ticketService.changeStatus(t2.getId(), GrievanceTicketService.STATUS_ASSIGNED, false);

        Map<String, Long> counts = ticketService.getTicketCounts(42L);
        assertThat(counts.get("TOTAL")).isEqualTo(2L);
        assertThat(counts.get("NEW")).isEqualTo(1L);
        assertThat(counts.get("ASSIGNED")).isEqualTo(1L);
        assertThat(counts.get("RESOLVED")).isEqualTo(0L);
        assertThat(counts.get("RAISED-BY-CITIZEN")).isEqualTo(1L);
        assertThat(counts.get("TODAYS-TICKET")).isEqualTo(2L);
    }

    @Test
    void listScoping_byUnitAndMobile() throws Exception {
        ticketService.raiseTicket(mapper.readTree("{\"name\":\"U7\",\"issue\":\"i\",\"mobileNo\":1111111111}"), 7L, false);
        ticketService.raiseTicket(mapper.readTree("{\"name\":\"U9\",\"issue\":\"i\",\"mobileNo\":2222222222}"), 9L, false);

        assertThat(ticketService.listTickets(7L, null)).hasSize(1);
        assertThat(ticketService.listTickets(null, 2222222222L)).hasSize(1);
        assertThat(ticketService.listTickets(null, null)).hasSize(2);
    }
}

package com.hims.grievance.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.application.exceptions.InvalidGrievanceRequestException;
import com.hims.grievance.domain.model.GrievanceComment;
import com.hims.grievance.domain.model.GrievanceFeedback;
import com.hims.grievance.domain.model.GrievanceTicket;
import com.hims.grievance.domain.repository.GrievanceCommentRepository;
import com.hims.grievance.domain.repository.GrievanceFeedbackRepository;
import com.hims.grievance.domain.repository.GrievanceTicketRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/** L1 — ticket lifecycle business rules (status machine, ticket number, reopen, feedback close). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrievanceTicketServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private GrievanceTicketRepository ticketRepository;
    @Mock private GrievanceCommentRepository commentRepository;
    @Mock private GrievanceFeedbackRepository feedbackRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private GrievanceTicketService service;

    @BeforeEach
    void setUp() {
        service = new GrievanceTicketService(ticketRepository, commentRepository, feedbackRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence stubs
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        // save() returns the entity
        when(ticketRepository.save(any(GrievanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.save(any(GrievanceComment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(feedbackRepository.save(any(GrievanceFeedback.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void raiseTicket_stampsTicketNoStatusAndAudit() throws Exception {
        GrievanceTicket t = service.raiseTicket(
                mapper.readTree("{\"name\":\"Ramesh\",\"issue\":\"Long OPD wait\",\"mobileNo\":9876543210}"),
                7L, false);
        assertThat(t.getTicketNo()).matches("IHMS/G/\\d{4}/\\d{5}");
        assertThat(t.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_NEW);
        assertThat(t.getHospitalTicket()).isEqualTo("IHMSSUPPORT");
        assertThat(t.getIssueDate()).isNotNull();
        assertThat(t.getIsCitizen()).isFalse();
        assertThat(t.getAddedBy()).isEqualTo(GrievanceTicketService.AUDIT_ACTOR_ID);
        assertThat(t.getUnitId()).isEqualTo(7L);
    }

    @Test
    void raiseTicket_citizenFlag() throws Exception {
        GrievanceTicket t = service.raiseTicket(
                mapper.readTree("{\"name\":\"Citizen\",\"issue\":\"Portal problem\"}"), null, true);
        assertThat(t.getIsCitizen()).isTrue();
        assertThat(t.getUnitId()).isNull();
    }

    @Test
    void raiseTicket_requiresNameAndIssue() {
        assertThatThrownBy(() -> service.raiseTicket(mapper.readTree("{\"name\":\"X\"}"), null, false))
                .isInstanceOf(InvalidGrievanceRequestException.class)
                .hasMessageContaining("Invalid input data");
        assertThatThrownBy(() -> service.raiseTicket(null, null, false))
                .isInstanceOf(InvalidGrievanceRequestException.class);
    }

    @Test
    void changeStatus_resolved_setsCompletedAndResolvedDate() {
        GrievanceTicket existing = ticket(5L);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(5L);
        GrievanceTicket r = service.changeStatus(5L, GrievanceTicketService.STATUS_RESOLVED, false);
        assertThat(r.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_RESOLVED);
        assertThat(r.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_COMPLETED);
        assertThat(r.getResolvedDate()).isNotNull();
    }

    @Test
    void changeStatus_assignToCreator_setsClosed() {
        GrievanceTicket existing = ticket(6L);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(6L);
        GrievanceTicket r = service.changeStatus(6L, GrievanceTicketService.STATUS_ASSIGN_TO_CREATOR, true);
        assertThat(r.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_CLOSED);
        assertThat(r.getResolvedDate()).isNotNull();
    }

    @Test
    void changeStatus_invalidStatus_rejected() {
        assertThatThrownBy(() -> service.changeStatus(1L, (short) 9, false))
                .isInstanceOf(InvalidGrievanceRequestException.class);
        assertThatThrownBy(() -> service.changeStatus(1L, null, false))
                .isInstanceOf(InvalidGrievanceRequestException.class);
    }

    @Test
    void changeStatus_missingTicket_404() {
        doReturn(Optional.empty()).when(ticketRepository).findById(99L);
        assertThatThrownBy(() -> service.changeStatus(99L, (short) 2, false))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    @Test
    void reopen_raiseTicket_setsAssignedPendingAndComment() {
        GrievanceTicket existing = ticket(7L);
        existing.setIssueStatus(GrievanceTicketService.STATUS_RESOLVED);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(7L);
        GrievanceTicket r = service.reopenTicket(7L, "Not actually fixed", true);
        assertThat(r.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_ASSIGNED);
        assertThat(r.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_PENDING);
        assertThat(r.getReopenedBy()).isNotNull();
        assertThat(r.getReopenedDatetime()).isNotNull();
    }

    @Test
    void reopen_plain_setsNew() {
        GrievanceTicket existing = ticket(8L);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(8L);
        GrievanceTicket r = service.reopenTicket(8L, null, false);
        assertThat(r.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_NEW);
    }

    @Test
    void addFeedback_closesTicket() throws Exception {
        GrievanceTicket existing = ticket(9L);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(9L);
        GrievanceFeedback f = service.addFeedback(
                mapper.readTree("{\"grievanceId\":9,\"question1\":5,\"freeTextFeedback\":\"Good service\"}"));
        assertThat(f.getGrievanceId()).isEqualTo(9L);
        assertThat(existing.getFeedbackId()).isNotNull();
        assertThat(existing.getTicketCloseStatus()).isEqualTo(GrievanceTicketService.CLOSE_CLOSED);
    }

    @Test
    void assign_setsUserAndExpectedDate_doesNotChangeStatus() {
        GrievanceTicket existing = ticket(10L);
        existing.setIssueStatus(GrievanceTicketService.STATUS_NEW);
        doReturn(Optional.of(existing)).when(ticketRepository).findById(10L);
        java.sql.Timestamp exp = java.sql.Timestamp.valueOf(java.time.LocalDate.of(2027, 1, 15).atStartOfDay());
        GrievanceTicket r = service.assignTicket(10L, 42L, exp, "SERVICE REQUEST", "IHMSSUPPORT");
        assertThat(r.getAssignedUserId()).isEqualTo(42L);
        assertThat(r.getExpectedResolutionDate()).isEqualTo(exp);
        assertThat(r.getIssueStatus()).isEqualTo(GrievanceTicketService.STATUS_NEW); // unchanged
    }

    @Test
    void addComment_requiresTicketAndText() {
        assertThatThrownBy(() -> service.addComment(1L, "  "))
                .isInstanceOf(InvalidGrievanceRequestException.class);
        doReturn(Optional.empty()).when(ticketRepository).findById(123L);
        assertThatThrownBy(() -> service.addComment(123L, "hello"))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    private GrievanceTicket ticket(long id) {
        GrievanceTicket t = new GrievanceTicket();
        t.setId(id);
        t.setUnitId(7L);
        t.setIssueStatus(GrievanceTicketService.STATUS_NEW);
        return t;
    }
}

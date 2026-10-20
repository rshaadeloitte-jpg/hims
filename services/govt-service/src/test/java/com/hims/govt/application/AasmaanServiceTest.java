package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.AasmaanCase;
import com.hims.govt.domain.model.AasmaanCaseNote;
import com.hims.govt.domain.repository.AasmaanCaseNoteRepository;
import com.hims.govt.domain.repository.AasmaanCaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — Aasmaan forward-only status machine + closed-case note guard
 * (register WP-T4), no Spring context.
 * EVENT POLICY note (cssd-service convention): no outbox emitter exists in the
 * constructor graph — nothing here can write an outbox row; CaseUpdated stays
 * DEFERRED. L2 asserts the negative outbox contract on real PG16.
 */
class AasmaanServiceTest {

    private AasmaanCaseRepository caseRepository;
    private AasmaanCaseNoteRepository noteRepository;
    private AasmaanService service;
    private final AtomicLong seq = new AtomicLong(200);

    @BeforeEach
    void setUp() {
        caseRepository = mock(AasmaanCaseRepository.class);
        noteRepository = mock(AasmaanCaseNoteRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new AasmaanService(caseRepository, noteRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static AasmaanCase saved(long id, short status) {
        AasmaanCase c = new AasmaanCase();
        c.setCaseId(id);
        c.setPatientId(11L);
        c.setCaseType("ANC");
        c.setStatus(status);
        return c;
    }

    // ------------------------------------------------------------- create

    @Test
    void create_requiresPatientIdAndCaseType_bexa003() {
        assertThatThrownBy(() -> service.create(null, "ANC"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("patientId is required");
        assertThatThrownBy(() -> service.create(11L, ""))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("caseType is required");
        assertThatThrownBy(() -> service.create(11L, "x".repeat(41)))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("at most 40 characters");
    }

    @Test
    void create_newCaseOpensOpen_withOpenedOnToday() {
        AasmaanCase c = service.create(11L, "ANC");

        assertThat(c.getStatus()).isEqualTo(AasmaanCase.STATUS_OPEN);
        assertThat(c.getOpenedOn()).isNotNull();
        assertThat(c.getClosedOn()).isNull();
    }

    // ------------------------------------------------------------- notes guard

    @Test
    void addNote_onOpenCase_allowed() {
        AasmaanCase c = saved(1L, AasmaanCase.STATUS_OPEN);
        when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

        AasmaanCaseNote n = service.addNote(1L, "follow-up done", "asha-7");

        assertThat(n.getCaseId()).isEqualTo(1L);
        assertThat(n.getNote()).isEqualTo("follow-up done");
        verify(noteRepository).save(any());
    }

    @Test
    void addNote_blankNote_bexa003() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_OPEN)));

        assertThatThrownBy(() -> service.addNote(1L, " ", "asha-7"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("note is required");
    }

    @Test
    void addNote_onClosedCase_rejected_govtCaseClosed() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_CLOSED)));

        assertThatThrownBy(() -> service.addNote(1L, "late note", "asha-7"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_CASE_CLOSED");
    }

    @Test
    void addNote_unknownCase_mapsTo404() {
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNote(99L, "n", "a"))
                .isInstanceOf(GovtNotFoundException.class)
                .extracting(e -> ((GovtNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    // ------------------------------------------------------------- status machine

    @Test
    void status_forwardOnly_adjacentStepsAllowed() {
        AasmaanCase c = saved(1L, AasmaanCase.STATUS_OPEN);
        when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

        AasmaanCase inProgress = service.updateStatus(1L, "IN_PROGRESS");
        assertThat(inProgress.getStatus()).isEqualTo(AasmaanCase.STATUS_IN_PROGRESS);
        assertThat(inProgress.getClosedOn()).isNull();   // not closed yet

        AasmaanCase closed = service.updateStatus(1L, "CLOSED");
        assertThat(closed.getStatus()).isEqualTo(AasmaanCase.STATUS_CLOSED);
        assertThat(closed.getClosedOn()).isNotNull();    // closing stamps closed_on
    }

    @Test
    void status_backwardMove_rejected_govtIllegalTransition() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_IN_PROGRESS)));

        assertThatThrownBy(() -> service.updateStatus(1L, "OPEN"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
    }

    @Test
    void status_repeatMove_rejected_govtIllegalTransition() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_IN_PROGRESS)));

        assertThatThrownBy(() -> service.updateStatus(1L, "IN_PROGRESS"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
    }

    @Test
    void status_skipFromOpenToClosed_rejected_forwardOnlyAdjacentSteps() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_OPEN)));

        assertThatThrownBy(() -> service.updateStatus(1L, "CLOSED"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
    }

    @Test
    void status_fromTerminalClosed_anythingIsRejected() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_CLOSED)));

        for (String target : new String[]{"OPEN", "IN_PROGRESS", "CLOSED", "1", "2", "3"}) {
            assertThatThrownBy(() -> service.updateStatus(1L, target))
                    .isInstanceOf(InvalidGovtRequestException.class)
                    .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                    .isEqualTo("GOVT_ILLEGAL_TRANSITION");
        }
    }

    @Test
    void status_unknownOrOutOfRange_bexa003() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_OPEN)));

        assertThatThrownBy(() -> service.updateStatus(1L, "MAYBE"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("Unknown status");
        assertThatThrownBy(() -> service.updateStatus(1L, "4"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("Unknown status");
        assertThatThrownBy(() -> service.updateStatus(1L, null))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("status is required");
    }

    @Test
    void status_numericFiltersAccepted() {
        when(caseRepository.findById(1L)).thenReturn(Optional.of(saved(1L, AasmaanCase.STATUS_OPEN)));
        assertThat(service.updateStatus(1L, "2").getStatus()).isEqualTo((short) 2);
    }

    @Test
    void updateStatus_unknownCase_mapsTo404() {
        when(caseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, "OPEN"))
                .isInstanceOf(GovtNotFoundException.class)
                .extracting(e -> ((GovtNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }
}

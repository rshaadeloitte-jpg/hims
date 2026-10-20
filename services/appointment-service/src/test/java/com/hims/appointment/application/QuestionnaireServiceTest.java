package com.hims.appointment.application;

import com.hims.appointment.domain.model.Questionnaire;
import com.hims.appointment.domain.repository.QuestionnaireRepository;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireServiceTest {

    @Mock private QuestionnaireRepository questionnaireRepository;
    @Mock private SequenceGateway sequences;

    private QuestionnaireService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireService(questionnaireRepository, sequences);
    }

    @Test
    void submitAnswers_validInput_returnsSaved() {
        when(sequences.next(anyString())).thenReturn(500L);
        when(questionnaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Questionnaire result = service.submitAnswers(1L, 10L, 20L, 30L, "PRE_CONSULT", "FORM", "[{\"q1\":\"a1\"}]", false);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getAppointmentId()).isEqualTo(1L);
        assertThat(result.getModule()).isEqualTo("PRE_CONSULT");
    }

    @Test
    void getByAppointmentAndModule_existing_returnsQuestionnaire() {
        Questionnaire q = new Questionnaire();
        q.setId(500L);
        q.setAppointmentId(1L);
        q.setModule("Patient Assessment");
        when(questionnaireRepository.findByAppointmentIdAndModule(1L, "Patient Assessment"))
                .thenReturn(Optional.of(q));

        Optional<Questionnaire> result = service.getByAppointmentAndModule(1L, "Patient Assessment");

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getId()).isEqualTo(500L);
    }

    @Test
    void getByAppointmentAndModule_missing_returnsEmpty() {
        when(questionnaireRepository.findByAppointmentIdAndModule(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        Optional<Questionnaire> result = service.getByAppointmentAndModule(99L, "Unknown");

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void listPendingReview_returnsList() {
        Questionnaire q = new Questionnaire();
        q.setId(600L);
        q.setClinicianReviewFlag(false);
        when(questionnaireRepository.findByClinicianReviewFlagTrueAndStatusTrue())
                .thenReturn(List.of(q));

        List<Questionnaire> result = service.listPendingReview();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(600L);
    }

    @Test
    void markReviewed_updatesFlag() {
        Questionnaire q = new Questionnaire();
        q.setId(700L);
        q.setClinicianReviewFlag(false);
        when(questionnaireRepository.findById(700L)).thenReturn(Optional.of(q));
        when(questionnaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Questionnaire result = service.markReviewed(700L, 50L);

        assertThat(result.getClinicianReviewFlag()).isTrue();
        assertThat(result.getReviewedBy()).isEqualTo(50L);
    }
}

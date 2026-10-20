package com.hims.forms.infrastructure.persistence;

import com.hims.forms.FormsServiceApplication;
import com.hims.forms.application.FormService;
import com.hims.forms.domain.model.FormDefinition;
import com.hims.forms.domain.model.FormSubmission;
import com.hims.forms.domain.repository.FormDefinitionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — forms service on real PG16: V1 schema applies (ihms_forms),
 * 5 seeded forms + schema fetch + submit + validation.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = FormsServiceApplication.class)
@DirtiesContext
@Transactional
class FormServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired FormService formService;
    @Autowired FormDefinitionRepository formRepo;
    @PersistenceContext EntityManager em;

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_forms' AND table_name IN
                          ('t_form_schema','t_form_submission')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("t_form_schema", "t_form_submission");
    }

    @Test
    void seed_fiveForms_present() {
        List<FormDefinition> forms = formService.listForms();
        assertThat(forms).hasSize(5);
        assertThat(forms.stream().map(FormDefinition::getCode).toList())
                .containsExactlyInAnyOrder(
                        "NURSING_INITIAL_ASSESSMENT",
                        "EMERGENCY_TRIAGE",
                        "DISCHARGE_SUMMARY",
                        "PRE_ANESTHETIC_CHECK",
                        "DAILY_NURSING_REPORT");
    }

    @Test
    void getSchema_byId_returnsJson() {
        FormDefinition f = formService.listForms().get(0);
        FormDefinition fetched = formService.getSchema(f.getId());
        assertThat(fetched.getSchemaJson()).contains("\"type\"");
        assertThat(fetched.getSpecialty()).isNotBlank();
    }

    @Test
    void submit_validPayload_persistsAndLists() {
        FormDefinition f = formRepo.findByCode("NURSING_INITIAL_ASSESSMENT").orElseThrow();
        Map<String, Object> payload = Map.of("chiefComplaint", "chest pain", "painScore", 7);
        FormSubmission sub = formService.submit(f.getId(), 123L, 456L, payload, 1L);
        assertThat(sub.getId()).isNotNull();
        assertThat(sub.getSubmissionJson()).contains("chest pain");

        List<FormSubmission> list = formService.listSubmissions(f.getId());
        assertThat(list).isNotEmpty();
    }

    @Test
    void submit_missingRequired_throws400() {
        FormDefinition f = formRepo.findByCode("NURSING_INITIAL_ASSESSMENT").orElseThrow();
        Map<String, Object> bad = Map.of("chiefComplaint", "fever"); // missing painScore
        assertThatThrownBy(() -> formService.submit(f.getId(), null, null, bad, null))
                .hasMessageContaining("painScore");
    }

    @Test
    void submit_unknownForm_throws404() {
        assertThatThrownBy(() -> formService.submit(9999L, null, null, Map.of("x", 1), null))
                .hasMessageContaining("Form not found");
    }
}

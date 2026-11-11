package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabDuplicateException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.ReportPrintTemplate;
import com.hims.lab.domain.repository.ReportPrintTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintTemplateServiceTest {

    private ReportPrintTemplateRepository templateRepository;
    private EntityManager em;
    private PrintTemplateService service;

    @BeforeEach
    void setUp() {
        templateRepository = mock(ReportPrintTemplateRepository.class);
        em = mock(EntityManager.class);
        service = new PrintTemplateService(templateRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> 300L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_requiresCode() {
        assertThatThrownBy(() -> service.create(null, "desc", "content", null, null, null, null, false, "REPORT"))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_REQ_INVALID");
    }

    @Test
    void create_duplicateCode_rejected() {
        when(templateRepository.existsByCode("CBC-STD")).thenReturn(true);

        assertThatThrownBy(() -> service.create("CBC-STD", "desc", "content", null, null, null, null, false, "REPORT"))
                .isInstanceOf(LabDuplicateException.class)
                .extracting(e -> ((LabDuplicateException) e).getErrorCode())
                .isEqualTo("LAB_REQ_DUPLICATE");
    }

    @Test
    void create_invalidType_rejected() {
        when(templateRepository.existsByCode("CBC-STD")).thenReturn(false);

        assertThatThrownBy(() -> service.create("CBC-STD", "desc", "content", null, null, null, null, false, "INVALID"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("templateType must be one of");
    }

    @Test
    void create_valid_storesTemplate() {
        when(templateRepository.existsByCode("CBC-STD")).thenReturn(false);

        ReportPrintTemplate t = service.create("CBC-STD", "CBC Standard", "<html>template</html>", null, 1L, 100L, null, false, "REPORT");
        assertThat(t.getTemplateId()).isEqualTo(300L);
        assertThat(t.getCode()).isEqualTo("CBC-STD");
        assertThat(t.getTemplateType()).isEqualTo("REPORT");
        assertThat(t.getIsClinical()).isFalse();
    }

    @Test
    void create_defaultType_whenNull() {
        when(templateRepository.existsByCode("T1")).thenReturn(false);

        ReportPrintTemplate t = service.create("T1", "desc", "content", null, null, null, null, null, null);
        assertThat(t.getTemplateType()).isEqualTo("REPORT");
    }

    @Test
    void update_invalidType_rejected() {
        ReportPrintTemplate existing = new ReportPrintTemplate();
        existing.setTemplateId(5L);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, null, null, null, null, null, null, null, "BADTYPE"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("templateType must be one of");
    }

    @Test
    void getByCode_unknown_throws404() {
        when(templateRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode("UNKNOWN"))
                .isInstanceOf(LabEntityNotFoundException.class)
                .extracting(e -> ((LabEntityNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void listByType_invalidType_rejected() {
        assertThatThrownBy(() -> service.listByType("BAD"))
                .isInstanceOf(InvalidLabRequestException.class);
    }

    @Test
    void listByType_valid_returnsList() {
        when(templateRepository.findByTemplateTypeOrderByCodeAsc("REPORT")).thenReturn(List.of(new ReportPrintTemplate()));

        List<ReportPrintTemplate> result = service.listByType("REPORT");
        assertThat(result).hasSize(1);
    }

    @Test
    void delete_removesTemplate() {
        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(5L);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(t));

        service.delete(5L);
        verify(templateRepository).delete(t);
    }

    @Test
    void delete_unknownId_throws404() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(LabEntityNotFoundException.class);
    }
}

package com.hims.reference.application;

import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.Icd11Mms;
import com.hims.reference.domain.repository.Icd11Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Icd11ServiceTest {

    @Mock
    private Icd11Repository repository;

    private Icd11Service service;

    @BeforeEach
    void setUp() {
        service = new Icd11Service(repository);
    }

    private Icd11Mms createEntity(Long id, String code, String title, String classKind) {
        Icd11Mms e = new Icd11Mms();
        e.setId(id);
        e.setCode(code);
        e.setTitle(title);
        e.setClassKind(classKind);
        e.setLinearizationUri("http://id.who.int/icd/release/11/mms/" + id);
        e.setReleaseVersion("2025-01");
        return e;
    }

    @Test
    void search_byCodePrefix_delegatesToRepository() {
        Icd11Mms item = createEntity(1L, "1A00", "Cholera", "category");
        when(repository.searchByQuery(eq("1A"), any(PageRequest.class))).thenReturn(List.of(item));

        List<Icd11Mms> results = service.search("1A", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCode()).isEqualTo("1A00");
        assertThat(results.get(0).getAttribution()).isEqualTo("ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repository).searchByQuery(eq("1A"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(Icd11Service.DEFAULT_LIMIT);
    }

    @Test
    void search_byTitle_delegatesToRepository() {
        Icd11Mms item = createEntity(1L, "1A00", "Cholera", "category");
        when(repository.searchByQuery(eq("cholera"), any(PageRequest.class))).thenReturn(List.of(item));

        List<Icd11Mms> results = service.search("cholera", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Cholera");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repository).searchByQuery(eq("cholera"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void search_limitCap_defaultsAndMax() {
        // null limit -> default 20
        assertThat(Icd11Service.resolveLimit(null)).isEqualTo(20);
        // zero or negative -> default 20
        assertThat(Icd11Service.resolveLimit(0)).isEqualTo(20);
        assertThat(Icd11Service.resolveLimit(-5)).isEqualTo(20);
        // valid limit -> preserved
        assertThat(Icd11Service.resolveLimit(15)).isEqualTo(15);
        assertThat(Icd11Service.resolveLimit(50)).isEqualTo(50);
        // over 50 -> capped at 50
        assertThat(Icd11Service.resolveLimit(51)).isEqualTo(50);
        assertThat(Icd11Service.resolveLimit(200)).isEqualTo(50);
    }

    @Test
    void getByCode_found() {
        Icd11Mms item = createEntity(1L, "1A00", "Cholera", "category");
        when(repository.findByCodeIgnoreCase("1A00")).thenReturn(Optional.of(item));

        Icd11Mms found = service.getByCode("1A00");

        assertThat(found).isNotNull();
        assertThat(found.getCode()).isEqualTo("1A00");
        assertThat(found.getReleaseVersion()).isEqualTo("2025-01");
        assertThat(found.getAttribution()).isEqualTo("ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO");
    }

    @Test
    void getByCode_unknown_throwsMasterNotFoundException() {
        when(repository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode("UNKNOWN"))
                .isInstanceOf(MasterNotFoundException.class)
                .hasMessageContaining("ICD-11 code not found: UNKNOWN");
    }
}

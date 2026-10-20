package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.ConcessionTemplate;
import com.hims.reference.domain.repository.ConcessionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConcessionTemplateServiceTest {

    @Mock private ConcessionTemplateRepository repository;
    private ConcessionTemplateService service;

    @BeforeEach void setUp() { service = new ConcessionTemplateService(repository); }

    @Test void create_ok() {
        when(repository.nextId()).thenReturn(7L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ConcessionTemplate t = service.create("PWD", "PWD Discount", "PWD", "desc",
                new BigDecimal("25.00"), true);
        assertThat(t.getId()).isEqualTo(7L);
        assertThat(t.getDocStatus()).isEqualTo(ConcessionTemplate.STATUS_ACTIVE);
        assertThat(t.getRequiresApproval()).isTrue();
    }

    @Test void create_outOfRange_throws() {
        assertThatThrownBy(() -> service.create("X", "X", "X", null,
                new BigDecimal("150"), false))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test void create_negative_throws() {
        assertThatThrownBy(() -> service.create("X", "X", "X", null,
                new BigDecimal("-1"), false))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test void deactivate_ok() {
        ConcessionTemplate t = new ConcessionTemplate(); t.setId(7L); t.setDocStatus(ConcessionTemplate.STATUS_ACTIVE);
        when(repository.findById(7L)).thenReturn(Optional.of(t));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.deactivate(7L).getDocStatus()).isEqualTo(ConcessionTemplate.STATUS_INACTIVE);
    }

    @Test void get_unknown_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(MasterNotFoundException.class);
    }
}

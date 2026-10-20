package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.domain.model.AppConfig;
import com.hims.reference.domain.model.AuditLog;
import com.hims.reference.domain.repository.AppConfigRepository;
import com.hims.reference.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonsServiceTest {
    @Mock private AppConfigRepository appConfigRepository;
    @Mock private AuditLogRepository auditLogRepository;
    private CommonsService service;
    @BeforeEach void setUp() { service = new CommonsService(appConfigRepository, auditLogRepository); }

    @Test void setConfig_create_whenMissing() {
        when(appConfigRepository.findByUnitIdAndConfigKey(1L, "X")).thenReturn(Optional.empty());
        when(appConfigRepository.nextId()).thenReturn(9L);
        when(appConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AppConfig c = service.setConfig(1L, "X", "v", "d");
        assertThat(c.getId()).isEqualTo(9L);
        assertThat(c.getConfigValue()).isEqualTo("v");
    }

    @Test void setConfig_update_whenPresent() {
        AppConfig existing = new AppConfig(); existing.setId(1L); existing.setConfigKey("X");
        existing.setConfigValue("old");
        when(appConfigRepository.findByUnitIdAndConfigKey(null, "X")).thenReturn(Optional.of(existing));
        when(appConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AppConfig c = service.setConfig(null, "X", "new", null);
        assertThat(c.getConfigValue()).isEqualTo("new");
    }

    @Test void setConfig_blankKey_throws() {
        assertThatThrownBy(() -> service.setConfig(1L, " ", "v", null))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test void record_audit_ok() {
        when(auditLogRepository.nextId()).thenReturn(1L);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuditLog a = service.record("Patient", 7L, "UPDATE", 99L, "alice", java.util.Map.of("k", "v"));
        assertThat(a.getEntityType()).isEqualTo("Patient");
        assertThat(a.getDiff()).contains("k");
    }

    @Test void record_blankAction_throws() {
        assertThatThrownBy(() -> service.record("X", 1L, " ", null, null, null))
                .isInstanceOf(InvalidMasterRequestException.class);
    }
}

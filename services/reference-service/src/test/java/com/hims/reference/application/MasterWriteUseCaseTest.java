package com.hims.reference.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.DepartmentMaster;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.UnitMaster;
import com.hims.reference.domain.repository.MasterReadRepository;
import com.hims.reference.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MasterWriteUseCaseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private MasterReadRepository repository;

    @Mock
    private OutboxService outboxService;

    private MasterWriteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MasterWriteUseCase(new MasterEntityRegistry(), repository, outboxService, mapper);
    }

    private JsonNode json(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_assignsSequenceIdWhenAbsent_stampsAudit_defaultsStatus() {
        doReturn(42L).when(repository).nextId("M_UNITMASTER_SEQ");
        UnitMaster saved = (UnitMaster) useCase.create("unitmaster", json("{\"code\":\"U1\",\"name\":\"Unit One\"}"), null);

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getStatus()).isTrue();
        assertThat(saved.getAddedby()).isEqualTo(MasterWriteUseCase.AUDIT_ACTOR_ID);
        assertThat(saved.getAddeddatetime()).isNotNull();
        assertThat(saved.getSynchronizedd()).isFalse();
        verify(repository).save(saved);
    }

    @Test
    void create_unitScopedEntity_requiresUnitId() {
        assertThatThrownBy(() -> useCase.create("departmentmaster", json("{\"code\":\"D1\",\"description\":\"D\"}"), null))
                .isInstanceOf(InvalidMasterRequestException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_unitScopedEntity_usesBodyUnitIdWhenParamMissing() {
        DepartmentMaster saved = (DepartmentMaster) useCase.create(
                "departmentmaster", json("{\"code\":\"D1\",\"description\":\"D\",\"unitId\":7}"), null);
        assertThat(saved.getUnitId()).isEqualTo(7L);
    }

    @Test
    void create_rejectsClientSuppliedId() {
        // F-3 (AGT-10): ids are server-assigned; a client id could overwrite rows via merge
        assertThatThrownBy(() -> useCase.create("unitmaster", json("{\"id\":9001,\"code\":\"U1\",\"name\":\"N\"}"), null))
                .isInstanceOf(InvalidMasterRequestException.class);
        verify(repository, never()).nextId(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void create_requiredFieldsEnforced() {
        // F-12 (AGT-10): registry-driven required fields -> BEXA003
        assertThatThrownBy(() -> useCase.create("unitmaster", json("{\"code\":\"U1\"}"), null))
                .isInstanceOf(InvalidMasterRequestException.class);
        assertThatThrownBy(() -> useCase.create("unitmaster", json("{\"name\":\"N\"}"), null))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void create_auditFieldsAreServerOwned() {
        doReturn(44L).when(repository).nextId("M_UNITMASTER_SEQ");
        UnitMaster saved = (UnitMaster) useCase.create("unitmaster",
                json("{\"code\":\"U1\",\"name\":\"N\",\"addedby\":999,\"updatedby\":999,\"createdunitid\":888}"), null);
        assertThat(saved.getAddedby()).isEqualTo(MasterWriteUseCase.AUDIT_ACTOR_ID);
        assertThat(saved.getUpdatedby()).isNull();
        assertThat(saved.getCreatedunitid()).isNull();
        assertThat(saved.getUpdateddatetime()).isNull();
    }

    @Test
    void create_nullPayload_isBexa003() {
        assertThatThrownBy(() -> useCase.create("unitmaster", null, null))
                .isInstanceOf(InvalidMasterRequestException.class)
                .hasMessage("Invalid input data !");
    }

    @Test
    void create_emitsOutboxEvent_opCreate() {
        doReturn(43L).when(repository).nextId("M_UNITMASTER_SEQ");
        useCase.create("unitmaster", json("{\"code\":\"U1\",\"name\":\"N\"}"), null);
        verify(outboxService).append(eq("MasterDataChanged"), eq("UnitMaster"), eq(43L),
                eq("43"), anyString(), anyString());
    }

    @Test
    void update_missingRecord_isBexa031NotFound() {
        doReturn(Optional.empty()).when(repository).findByIdScoped(any(), anyLong(), any(), any());
        assertThatThrownBy(() -> useCase.update("unitmaster", 99L, json("{\"name\":\"X\"}"), true, null, null))
                .isInstanceOf(MasterNotFoundException.class)
                .hasMessage("Not Authorized for editing !");
    }

    @Test
    void update_nonNullSkipsNulls_butCopiesPresentFields() {
        UnitMaster existing = unitMaster(5L, 2L, "OLD", "Old Name");
        existing.setDescription("Keep me");
        existing.setCreatedunitid(2L);
        doReturn(Optional.of(existing)).when(repository).findByIdScoped(any(), anyLong(), any(), any());

        UnitMaster updated = (UnitMaster) useCase.update(
                "unitmaster", 5L, json("{\"name\":\"New Name\",\"description\":null,\"createdunitid\":777}"), true, null, null);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("Keep me");   // null skipped (nonNull=true)
        assertThat(updated.getCreatedunitid()).isEqualTo(2L);         // audit fields server-owned (F-4)
        assertThat(updated.getId()).isEqualTo(5L);                    // id never overwritten
        assertThat(updated.getUnitId()).isEqualTo(2L);                // unitId never overwritten
        assertThat(updated.getUpdatedby()).isEqualTo(MasterWriteUseCase.AUDIT_ACTOR_ID);
        assertThat(updated.getUpdateddatetime()).isNotNull();
    }

    @Test
    void update_nonNullFalse_overwritesWithNulls() {
        UnitMaster existing = unitMaster(5L, 2L, "OLD", "Old Name");
        existing.setDescription("Keep me");
        doReturn(Optional.of(existing)).when(repository).findByIdScoped(any(), anyLong(), any(), any());

        UnitMaster updated = (UnitMaster) useCase.update(
                "unitmaster", 5L, json("{\"description\":null}"), false, null, null);

        assertThat(updated.getDescription()).isNull();           // legacy full-overwrite semantics
    }

    @Test
    void update_updFldsRestrictsCopiedFields() {
        UnitMaster existing = unitMaster(5L, 2L, "OLD", "Old Name");
        doReturn(Optional.of(existing)).when(repository).findByIdScoped(any(), anyLong(), any(), any());

        UnitMaster updated = (UnitMaster) useCase.update(
                "unitmaster", 5L, json("{\"name\":\"New\",\"code\":\"NEW\"}"), true, Set.of("name"), null);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getCode()).isEqualTo("OLD");          // code not in updFlds
    }

    @Test
    void update_emitsOutboxEvent_opUpdate() {
        UnitMaster existing = unitMaster(5L, 2L, "OLD", "Old Name");
        doReturn(Optional.of(existing)).when(repository).findByIdScoped(any(), anyLong(), any(), any());
        useCase.update("unitmaster", 5L, json("{\"name\":\"New\"}"), true, null, null);
        verify(outboxService).append(eq("MasterDataChanged"), eq("UnitMaster"), eq(5L),
                eq("5"), anyString(), anyString());
    }

    @Test
    void remove_missing_isNotFound() {
        doReturn(Optional.empty()).when(repository).findByIdScoped(any(), anyLong(), any(), any());
        assertThatThrownBy(() -> useCase.remove("unitmaster", 999L, null))
                .isInstanceOf(MasterNotFoundException.class);
    }

    @Test
    void remove_deletesAndEmits_opRemove() {
        UnitMaster existing = unitMaster(5L, 2L, "OLD", "Old Name");
        doReturn(Optional.of(existing)).when(repository).findByIdScoped(any(), anyLong(), any(), any());
        useCase.remove("unitmaster", 5L, null);
        verify(repository).delete(existing);
        verify(outboxService).append(eq("MasterDataChanged"), eq("UnitMaster"), eq(5L),
                eq("5"), anyString(), anyString());
    }

    private UnitMaster unitMaster(Long id, Long unitId, String code, String name) {
        UnitMaster u = new UnitMaster();
        u.setId(id);
        u.setUnitId(unitId);
        u.setCode(code);
        u.setName(name);
        u.setStatus(true);
        u.setAddeddatetime(Timestamp.valueOf("2026-01-01 00:00:00"));
        u.setSynchronizedd(false);
        return u;
    }
}

package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.DepartmentMaster;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.UnitMaster;
import com.hims.reference.domain.repository.MasterReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterReadUseCaseTest {

    @Mock
    private MasterReadRepository repository;

    private MasterReadUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MasterReadUseCase(new MasterEntityRegistry(), repository);
    }

    private UnitMaster unitMaster(Long id, Long unitId) {
        UnitMaster u = new UnitMaster();
        u.setId(id);
        u.setUnitId(unitId);
        u.setCode("U" + id);
        u.setName("Unit " + id);
        u.setAddeddatetime(Timestamp.valueOf("2026-01-01 00:00:00"));
        u.setStatus(true);
        return u;
    }

    @Test
    void getById_unknownEntity_rejects() {
        assertThatThrownBy(() -> useCase.getById("bogus", 1L, 1L, Map.of()))
                .isInstanceOf(InvalidMasterRequestException.class)
                .hasMessageContaining("Unknown master entity");
    }

    @Test
    void getById_invalidId_throwsBexa003() {
        assertThatThrownBy(() -> useCase.getById("unitmaster", 0L, 1L, Map.of()))
                .isInstanceOf(InvalidMasterRequestException.class)
                .extracting(e -> ((InvalidMasterRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void getById_notFound_throwsNotFound() {
        doReturn(Optional.empty()).when(repository).findByIdScoped(eq(UnitMaster.class), eq(42L), eq(1L), any());
        assertThatThrownBy(() -> useCase.getById("unitmaster", 42L, 1L, Map.of()))
                .isInstanceOf(MasterNotFoundException.class);
    }

    @Test
    void getById_returnsRecord() {
        doReturn(Optional.of(unitMaster(7L, 1L))).when(repository)
                .findByIdScoped(eq(UnitMaster.class), eq(7L), eq(1L), any());
        MasterEntityBase found = useCase.getById("unitmaster", 7L, 1L, Map.of());
        assertThat(found.getId()).isEqualTo(7L);
        assertThat(found.getUnitId()).isEqualTo(1L);
    }

    @Test
    void getById_departmentIsUnitScoped() {
        doReturn(Optional.of(new DepartmentMaster())).when(repository)
                .findByIdScoped(eq(DepartmentMaster.class), eq(3L), eq(9L), any());
        MasterEntityBase found = useCase.getById("departmentmaster", 3L, 9L, Map.of());
        assertThat(found).isNotNull();
    }

    @Test
    void getById_filterWhitelistApplied() {
        // status is an allowed filter on unitmaster; bogusfilter must be dropped before the repo
        doReturn(Optional.of(unitMaster(1L, 1L))).when(repository)
                .findByIdScoped(eq(UnitMaster.class), eq(1L), eq(1L), any());
        useCase.getById("unitmaster", 1L, 1L, Map.of("status", "true", "bogusFilter", "x"));
    }

    @Test
    void getAll_allowedFiltersOnly_paginatesWithLegacyOffset() {
        // legacy: page=250, count=10 -> service page=250/10=25 -> DAO setFirstResult(25*10=250)
        doReturn(List.of(unitMaster(1L, 1L))).when(repository)
                .findAll(eq(UnitMaster.class), any(), any(), any(), eq(250), eq(10), any());
        List<? extends MasterEntityBase> result =
                useCase.getAll("unitmaster", Map.of("status", "true"), "delhi", "name", 250, 10);
        assertThat(result).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({
            "250,10,250",
            "40,20,40",
            "10,5,10",
            "7,5,5",
            "0,20,0",
            "5,0,0"
    })
    void getAll_legacyOffsetFormula_isPageDividedCountTimesCount(int page, int count, int expectedOffset) {
        doReturn(List.of()).when(repository)
                .findAll(eq(UnitMaster.class), any(), any(), any(), eq(expectedOffset), eq(count), any());
        useCase.getAll("unitmaster", Map.of(), null, null, page, count);
    }

    @ParameterizedTest
    @CsvSource({
            "250,10,250",
            "10,5,10",
            "7,5,5",
            "0,20,0",
            "5,0,0"
    })
    void getScoped_legacyOffsetFormula_isPageDividedCountTimesCount(int page, int count, int expectedOffset) {
        doReturn(List.of()).when(repository)
                .findScoped(eq(UnitMaster.class), eq(1L), any(), any(), any(), eq(expectedOffset), eq(count), any());
        useCase.getScoped("unitmaster", 1L, Map.of(), null, null, page, count);
    }

    @Test
    void getAll_filterNotAllowed_dropped() {
        doReturn(List.of()).when(repository)
                .findAll(eq(UnitMaster.class), any(), any(), any(), eq(0), eq(0), any());
        useCase.getAll("unitmaster", Map.of("notAllowed", "1"), null, null, 0, 0);
    }

    @Test
    void getScoped_requiresUnitId() {
        assertThatThrownBy(() -> useCase.getScoped("unitmaster", null, Map.of(), null, null, 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void getAll_unknownSearchField_isRejected() {
        // F-2 (AGT-10): searchFld is validated against persisted fields before JPQL
        assertThatThrownBy(() -> useCase.getAll("unitmaster", Map.of(), "x", "bogusField; DROP", 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class)
                .hasMessage("Invalid input data !");
    }

    @Test
    void getAll_unknownOrderFieldViaSearchFld_isRejected() {
        // orderField == searchFld (legacy semantic); unknown -> BEXA003
        assertThatThrownBy(() -> useCase.getAll("unitmaster", Map.of(), "5", "bogusField", 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void getScoped_unknownSearchField_isRejected() {
        assertThatThrownBy(() -> useCase.getScoped("unitmaster", 1L, Map.of(), "x", "name; DROP", 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void getAll_idSearchWithNonNumericTerm_isRejected() {
        assertThatThrownBy(() -> useCase.getAll("unitmaster", Map.of(), "abc OR 1=1", "id", 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }
}

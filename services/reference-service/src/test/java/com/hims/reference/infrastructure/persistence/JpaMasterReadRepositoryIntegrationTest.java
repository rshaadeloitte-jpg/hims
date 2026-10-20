package com.hims.reference.infrastructure.persistence;

import com.hims.reference.ReferenceServiceApplication;
import com.hims.reference.domain.model.CompanyMaster;
import com.hims.reference.domain.model.DepartmentMaster;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.UnitMaster;
import com.hims.reference.domain.repository.MasterReadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L2 integration: Flyway migrations + repository read semantics + observability
 * probes on real PostgreSQL 16. Requires a running Docker daemon (BLK-003);
 * skipped automatically when unavailable. Class-level transaction: each test
 * runs in its own tx, rolled back (seed visible to same-thread MockMvc calls).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReferenceServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext
@Transactional
class JpaMasterReadRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MasterReadRepository repository;

    @Autowired
    private com.hims.reference.application.MasterReadUseCase useCase;

    @Autowired
    private com.hims.reference.application.MasterWriteUseCase writeUseCase;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void seed() {
        UnitMaster u1 = unit(1L, 1L, "U001", "Jaipur Hospital");
        UnitMaster u2 = unit(2L, 2L, "U002", "Delhi Hospital");
        em.persist(u1);
        em.persist(u2);
        em.persist(company(10L, 1L, "C001", "Rajasthan Govt"));
    }

    private UnitMaster unit(Long id, Long unitId, String code, String name) {
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

    private CompanyMaster company(Long id, Long unitId, String code, String description) {
        CompanyMaster c = new CompanyMaster();
        c.setId(id);
        c.setUnitId(unitId);
        c.setCode(code);
        c.setDescription(description);
        c.setStatus(true);
        return c;
    }

    @Test
    void flywayCreatedQuotedTables() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_master' ORDER BY table_name
                        """).getResultList();
        assertThat(tables).contains("M_UNITMASTER", "M_DEPARTMENTMASTER", "M_COMPANYMASTER", "outbox");
    }

    @Test
    void sequencesExist() {
        List<String> seqs = em.createNativeQuery("""
                        SELECT sequence_name FROM information_schema.sequences
                        WHERE sequence_schema = 'ihms_master' ORDER BY sequence_name
                        """).getResultList();
        assertThat(seqs).contains("M_UNITMASTER_SEQ", "M_DEPARTMENTMASTER_SEQ", "M_COMPANYMASTER_SEQ");
    }

    @Test
    void findByIdScoped_unitmasterIsNotUnitScoped() {
        Optional<? extends MasterEntityBase> found = repository.findByIdScoped(UnitMaster.class, 1L, 999L, Map.of());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByIdScoped_companymasterIsUnitScoped() {
        Optional<? extends MasterEntityBase> found =
                repository.findByIdScoped(CompanyMaster.class, 10L, 1L, Map.of());
        assertThat(found).isPresent();

        Optional<? extends MasterEntityBase> wrongUnit =
                repository.findByIdScoped(CompanyMaster.class, 10L, 777L, Map.of());
        assertThat(wrongUnit).isEmpty();
    }

    @Test
    void findAll_appliesFilters() {
        List<? extends MasterEntityBase> result =
                repository.findAll(UnitMaster.class, Map.of("status", true), List.of(), null, 0, 0, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_searchSingleTermAcrossFields() {
        List<? extends MasterEntityBase> result =
                repository.findAll(UnitMaster.class, Map.of(), List.of("name", "code"), "delhi", 0, 0, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void findAll_pagination() {
        List<? extends MasterEntityBase> result =
                repository.findAll(UnitMaster.class, Map.of(), List.of(), null, 0, 1, "id");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findScoped_unitScoped() {
        List<? extends MasterEntityBase> result =
                repository.findScoped(UnitMaster.class, 1L, Map.of(), List.of(), List.of(), 0, 0, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnitId()).isEqualTo(1L);
    }

    @Test
    void healthProbes_areOpenAndGreen() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void correlationId_isEchoed() throws Exception {
        mockMvc.perform(get("/api/v1/masters")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tester"))
                        .header("X-Request-ID", "corr-test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "corr-test-123"));
    }

    @Test
    void securedEndpoints_requireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/masters/unitmaster/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void useCase_legacyOffsetWindowSemantics_onRealPostgres() {
        // Full chain through the use case: legacy formula offset=(page/count)*count.
        //   page=2,count=3  -> offset (2/3)*3=0      -> rows 101,102,103
        //   page=6,count=3  -> offset (6/3)*3=6      -> rows 107,108,109
        //   page=10,count=4 -> offset (10/4)*4=8     -> rows 109,110
        for (long id = 101L; id <= 110L; id++) {
            em.persist(unit(id, 5L, "WIN", "WH"));
        }
        List<? extends MasterEntityBase> w1 =
                useCase.getScoped("unitmaster", 5L, Map.of(), null, null, 2, 3);
        assertThat(w1).extracting(e -> e.getId()).containsExactly(101L, 102L, 103L);

        List<? extends MasterEntityBase> w2 =
                useCase.getScoped("unitmaster", 5L, Map.of(), null, null, 6, 3);
        assertThat(w2).extracting(e -> e.getId()).containsExactly(107L, 108L, 109L);

        List<? extends MasterEntityBase> w3 =
                useCase.getScoped("unitmaster", 5L, Map.of(), null, null, 10, 4);
        assertThat(w3).extracting(e -> e.getId()).containsExactly(109L, 110L);
    }

    @Test
    void findScoped_addeddatetimeRangeAndsWithOrGroup() {
        // Legacy crudgetAll (GenericServiceImpl.internalCrudgetAll): the addeddatetime
        // range is appended as TOP-LEVEL AND clauses and other search fields form a
        // separate parenthesized OR group -> range AND (code LIKE ...).
        // Rows: A in-range but code not matching; B out-of-range but code matching;
        //       C matching both. Only C must be returned.
        UnitMaster a = unit(201L, 1L, "ABC", "Alpha");
        a.setAddeddatetime(Timestamp.valueOf("2026-06-01 00:00:00"));
        UnitMaster b = unit(202L, 1L, "xyz", "Beta");
        b.setAddeddatetime(Timestamp.valueOf("2025-01-01 00:00:00"));
        UnitMaster c = unit(203L, 1L, "xyz", "Gamma");
        c.setAddeddatetime(Timestamp.valueOf("2026-06-02 00:00:00"));
        em.persist(a);
        em.persist(b);
        em.persist(c);

        List<? extends MasterEntityBase> result = repository.findScoped(
                UnitMaster.class, 1L, Map.of(),
                List.of("addeddatetime", "code"), List.of("2026-01-01_2026-12-31", "xyz"),
                0, 0, null);
        assertThat(result).extracting(e -> e.getId()).containsExactly(203L);
    }

    @Test
    void unitmasterParityColumns_exist_afterV3() {
        List<String> cols = em.createNativeQuery(
                "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'ihms_master' AND table_name = 'M_UNITMASTER' "
                + "AND column_name IN ('ISCONFIRMATORY', 'ISADMINISTRATIVE', 'ISOFFLINE') "
                + "ORDER BY column_name").getResultList();
        assertThat(cols).containsExactlyInAnyOrder("ISADMINISTRATIVE", "ISCONFIRMATORY", "ISOFFLINE");
    }

    // ---------------------------------------------------------------- MIG-010 write path

    @Test
    void writePath_create_failure_rollsBackBusinessRowAndOutboxRow() throws Exception {
        // F-11 (AGT-10): prove same-tx atomicity — a failing create (DB constraint
        // violation at commit) must roll back the business row AND the outbox row.
        // Write ops run REQUIRES_NEW (legacy TxType.REQUIRES_NEW parity), so the
        // create's tx boundary is independent of the test tx.
        em.createNativeQuery("SELECT setval('ihms_master.\"M_UNITMASTER_SEQ\"', 8000)")
                .getSingleResult();
        String longCode = "X".repeat(300);   // exceeds VARCHAR(255) -> flush/commit failure

        assertThatThrownBy(() -> writeUseCase.create(
                "unitmaster", mapper.readTree("{\"code\":\"" + longCode + "\",\"name\":\"Boom\"}"), null))
                .isInstanceOf(Exception.class);

        Number outboxRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_master.outbox WHERE aggregate_id = 8001")
                .getSingleResult();
        assertThat(outboxRows.longValue()).isZero();
        var gone = repository.findByIdScoped(UnitMaster.class, 8001L, null, Map.of());
        assertThat(gone).isEmpty();
    }

    @Test
    void writePath_create_readsBack_andEmitsOutbox() throws Exception {
        // advance the per-table sequence so nextval cannot collide with seeded ids
        em.createNativeQuery("SELECT setval('ihms_master.\"M_UNITMASTER_SEQ\"', 5000)")
                .getSingleResult();

        com.hims.reference.domain.model.MasterEntityBase created = writeUseCase.create(
                "unitmaster", mapper.readTree("{\"code\":\"W1\",\"name\":\"Write Test Unit\",\"unitId\":5}"), null);

        assertThat(created.getId()).isEqualTo(5001L);
        assertThat(created.getStatus()).isTrue();

        // read back through the read API
        var readBack = repository.findByIdScoped(UnitMaster.class, 5001L, null, Map.of());
        assertThat(readBack).isPresent();
        assertThat(((UnitMaster) readBack.get()).getName()).isEqualTo("Write Test Unit");

        // outbox row appended in the same transaction
        Number outboxRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_master.outbox WHERE aggregate_id = 5001 AND event_type = 'MasterDataChanged'")
                .getSingleResult();
        assertThat(outboxRows.longValue()).isEqualTo(1L);
    }

    @Test
    void writePath_update_preservesIdAndUnitId_andEmitsOutbox() throws Exception {
        em.createNativeQuery("SELECT setval('ihms_master.\"M_DEPARTMENTMASTER_SEQ\"', 6000)")
                .getSingleResult();
        var created = writeUseCase.create(
                "departmentmaster", mapper.readTree("{\"code\":\"DEP1\",\"description\":\"Dept\",\"unitId\":3}"), null);

        DepartmentMaster updated = (DepartmentMaster) writeUseCase.update(
                "departmentmaster", created.getId(), mapper.readTree("{\"description\":\"Updated desc\",\"unitId\":999}"),
                true, null, 3L);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getUnitId()).isEqualTo(3L);            // unitId never overwritten
        assertThat(updated.getDescription()).isEqualTo("Updated desc");
        Number outboxRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_master.outbox WHERE aggregate_id = " + created.getId()
                                + " AND event_type = 'MasterDataChanged' AND payload ->> 'op' = 'update'")
                .getSingleResult();
        assertThat(outboxRows.longValue()).isEqualTo(1L);
    }

    @Test
    void writePath_remove_deletesAndEmitsOutbox() throws Exception {
        em.createNativeQuery("SELECT setval('ihms_master.\"M_COMPANYMASTER_SEQ\"', 7000)")
                .getSingleResult();
        var created = writeUseCase.create(
                "companymaster", mapper.readTree("{\"code\":\"C1\",\"description\":\"Co\",\"unitId\":1}"), null);

        var removed = writeUseCase.remove("companymaster", created.getId(), 1L);
        assertThat(removed.getId()).isEqualTo(created.getId());

        var readBack = repository.findByIdScoped(CompanyMaster.class, created.getId(), 1L, Map.of());
        assertThat(readBack).isEmpty();
        Number outboxRows = (Number) em.createNativeQuery(
                        "SELECT count(*) FROM ihms_master.outbox WHERE aggregate_id = " + created.getId()
                                + " AND event_type = 'MasterDataChanged' AND payload ->> 'op' = 'remove'")
                .getSingleResult();
        assertThat(outboxRows.longValue()).isEqualTo(1L);
    }
}

package com.hims.reference.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.ReferenceServiceApplication;
import com.hims.reference.application.MasterReadUseCase;
import com.hims.reference.application.MasterWriteUseCase;
import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.domain.model.GeoArea;
import com.hims.reference.domain.model.GeoCity;
import com.hims.reference.domain.model.GeoCountry;
import com.hims.reference.domain.model.GeoDistrict;
import com.hims.reference.domain.model.GeoState;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.Subunit;
import com.hims.reference.domain.repository.MasterReadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-011 follow-up: subunit + geo reference masters (V5 tables, D-6 global
 * scope). Covers the generic read/write machinery for the remaining V5 entities:
 * subunit (unit-scoped), geo_country/state/district/city/area (global scope —
 * unit_id NULL, listed without unitId, parent-FK filters bound as Long).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReferenceServiceApplication.class)
@DirtiesContext
@Transactional
class Mig011GeoSubunitIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MasterReadRepository repository;

    @Autowired
    private MasterReadUseCase readUseCase;

    @Autowired
    private MasterWriteUseCase writeUseCase;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void advanceSequences() {
        // setval is non-transactional in PG: survives the test-tx rollback, so each
        // test deterministically starts ids at 5001/6001/7001/8001/9001/10001.
        em.createNativeQuery("SELECT setval('ihms_master.\"subunit_seq\"', 5000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"geo_country_seq\"', 6000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"geo_state_seq\"', 7000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"geo_district_seq\"', 8000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"geo_city_seq\"', 9000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"geo_area_seq\"', 10000)").getSingleResult();
        // Write ops run REQUIRES_NEW (commit independently of the @Transactional test,
        // which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation (CASCADE handles the geo FK chain area->city->district->state->country).
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> em.createNativeQuery("""
                TRUNCATE ihms_master.geo_area, ihms_master.geo_city, ihms_master.geo_district,
                         ihms_master.geo_state, ihms_master.geo_country, ihms_master.subunit CASCADE""")
                .executeUpdate());
    }

    @Test
    void subunit_createReadUpdate_unitScoped() throws Exception {
        MasterEntityBase created = writeUseCase.create("subunit",
                mapper.readTree("{\"code\":\"OPD\",\"description\":\"OPD Subunit\"}"), 7L);
        assertThat(created.getId()).isEqualTo(5001L);
        assertThat(created.getUnitId()).isEqualTo(7L);

        Optional<? extends MasterEntityBase> readBack =
                repository.findByIdScoped(Subunit.class, 5001L, 7L, Map.of());
        assertThat(readBack).isPresent();
        assertThat(((Subunit) readBack.get()).getCode()).isEqualTo("OPD");

        // wrong unit scope -> not found (unit-scoped)
        assertThat(repository.findByIdScoped(Subunit.class, 5001L, 99L, Map.of())).isEmpty();

        MasterEntityBase updated = writeUseCase.update("subunit", 5001L,
                mapper.readTree("{\"description\":\"OPD Subunit (renamed)\"}"), true, null, 7L);
        assertThat(((Subunit) updated).getDescription()).isEqualTo("OPD Subunit (renamed)");

        // subunit requires unitId on the scoped list surface
        assertThatThrownBy(() -> readUseCase.getScoped("subunit", null, Map.of(), null, null, 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void geoHierarchy_create_globalScope_unitIdNull() throws Exception {
        MasterEntityBase country = writeUseCase.create("geo-country",
                mapper.readTree("{\"code\":\"IN\",\"name\":\"India\",\"isDefault\":true,\"nationalityName\":\"Indian\"}"), null);
        assertThat(country.getId()).isEqualTo(6001L);
        assertThat(country.getUnitId()).isNull();          // D-6 global scope

        MasterEntityBase state = writeUseCase.create("geo-state",
                mapper.readTree("{\"countryId\":6001,\"code\":\"RJ\",\"name\":\"Rajasthan\",\"nameHindi\":\"राजस्थान\",\"abdmCode\":\"AB-RJ\"}"), null);
        assertThat(state.getId()).isEqualTo(7001L);
        assertThat(state.getUnitId()).isNull();

        MasterEntityBase district = writeUseCase.create("geo-district",
                mapper.readTree("{\"stateId\":7001,\"code\":\"JAIPUR\",\"name\":\"Jaipur\",\"census2011Code\":\"112\",\"zoneId\":3}"), null);
        assertThat(district.getId()).isEqualTo(8001L);

        MasterEntityBase city = writeUseCase.create("geo-city",
                mapper.readTree("{\"districtId\":8001,\"stateId\":7001,\"code\":\"JPR\",\"name\":\"Jaipur City\",\"censusCode\":\"112345\"}"), null);
        assertThat(city.getId()).isEqualTo(9001L);

        MasterEntityBase area = writeUseCase.create("geo-area",
                mapper.readTree("{\"cityId\":9001,\"code\":\"MI\",\"name\":\"Malviya Nagar\"}"), null);
        assertThat(area.getId()).isEqualTo(10001L);

        // global read-back: id-only scope (no unitId predicate)
        assertThat(repository.findByIdScoped(GeoCountry.class, 6001L, null, Map.of())).isPresent();
        assertThat(((GeoState) repository.findByIdScoped(GeoState.class, 7001L, null, Map.of())
                .orElseThrow()).getAbdmCode()).isEqualTo("AB-RJ");
        assertThat(((GeoDistrict) repository.findByIdScoped(GeoDistrict.class, 8001L, null, Map.of())
                .orElseThrow()).getCensus2011Code()).isEqualTo("112");
        assertThat(((GeoCity) repository.findByIdScoped(GeoCity.class, 9001L, null, Map.of())
                .orElseThrow()).getCensusCode()).isEqualTo("112345");
        assertThat(((GeoArea) repository.findByIdScoped(GeoArea.class, 10001L, null, Map.of())
                .orElseThrow()).getName()).isEqualTo("Malviya Nagar");
    }

    @Test
    void geoList_noUnitIdRequired_parentFkFiltersBoundAsLong() throws Exception {
        writeUseCase.create("geo-country", mapper.readTree("{\"code\":\"IN\",\"name\":\"India\"}"), null);
        writeUseCase.create("geo-state", mapper.readTree("{\"countryId\":6001,\"code\":\"RJ\",\"name\":\"Rajasthan\"}"), null);
        writeUseCase.create("geo-state", mapper.readTree("{\"countryId\":6001,\"code\":\"GJ\",\"name\":\"Gujarat\"}"), null);

        // scoped surface WITHOUT unitId works for global masters (D-6)
        List<? extends MasterEntityBase> states =
                readUseCase.getScoped("geo-state", null, Map.of("countryId", "6001"), null, null, 0, 0);
        assertThat(states).hasSize(2);
        assertThat(states).extracting(e -> ((GeoState) e).getCode()).containsExactlyInAnyOrder("RJ", "GJ");

        // unscoped /all surface with the same Long-typed filter
        List<? extends MasterEntityBase> allStates =
                readUseCase.getAll("geo-state", Map.of("countryId", "6001"), null, null, 0, 0);
        assertThat(allStates).hasSize(2);

        // default search field for geo = name (entity has no description)
        List<? extends MasterEntityBase> searched =
                readUseCase.getScoped("geo-state", null, Map.of(), "guj", null, 0, 0);
        assertThat(searched).extracting(e -> ((GeoState) e).getCode()).containsExactly("GJ");

        // non-numeric Long filter -> BEXA003
        assertThatThrownBy(() -> readUseCase.getScoped("geo-state", null, Map.of("countryId", "abc"), null, null, 0, 0))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void geoUpdate_globalScope_roundTrip() throws Exception {
        writeUseCase.create("geo-country", mapper.readTree("{\"code\":\"IN\",\"name\":\"India\"}"), null);
        MasterEntityBase updated = writeUseCase.update("geo-country", 6001L,
                mapper.readTree("{\"nationalityName\":\"Indian\"}"), true, null, null);
        assertThat(((GeoCountry) updated).getNationalityName()).isEqualTo("Indian");
        assertThat(((GeoCountry) updated).getName()).isEqualTo("India");   // nonNull keeps it

        writeUseCase.remove("geo-country", 6001L, null);
        assertThat(repository.findByIdScoped(GeoCountry.class, 6001L, null, Map.of())).isEmpty();
    }
}

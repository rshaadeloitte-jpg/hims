package com.hims.reference.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.ReferenceServiceApplication;
import com.hims.reference.application.MasterReadUseCase;
import com.hims.reference.application.MasterWriteUseCase;
import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.domain.model.Doctor;
import com.hims.reference.domain.model.DoctorType;
import com.hims.reference.domain.model.FacilityLocation;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.Specialization;
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
 * L2 — MIG-011 clean target masters (V5): Flyway applies the clean tables,
 * the generic read/write machinery serves doctor/doctor-type/specialization/
 * facility-location with per-table sequences, required fields, unit scope.
 * NOTE: the ihms_item schema contract moved to the item service (MIG-012
 * ownership transfer, ONE-TABLE-ONE-OWNER §56) — asserted by item-service L2.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReferenceServiceApplication.class)
@DirtiesContext
@Transactional
class Mig011CleanMastersIntegrationTest {

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

    @BeforeEach
    void advanceSequences() {
        // keep nextval far from any seed ids
        em.createNativeQuery("SELECT setval('ihms_master.\"doctor_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"doctor_type_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"specialization_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_master.\"facility_location_seq\"', 4000)").getSingleResult();
    }

    @Test
    void v5CleanTables_exist() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_master' AND table_name IN
                        ('unit','unit_config','subunit','facility_location',
                         'geo_country','geo_state','geo_district','geo_city','geo_area',
                         'doctor','doctor_type','specialization','doctor_unit','doctor_department','concession_template')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactlyInAnyOrder(
                "unit", "unit_config", "subunit", "facility_location",
                "geo_country", "geo_state", "geo_district", "geo_city", "geo_area",
                "doctor", "doctor_type", "specialization", "doctor_unit", "doctor_department", "concession_template");
    }


    @Test
    void doctor_createReadUpdateDelete_roundTrip() throws Exception {
        MasterEntityBase created = writeUseCase.create("doctor",
                mapper.readTree("{\"firstName\":\"Anita\",\"lastName\":\"Sharma\",\"mobileNo\":\"9876500001\",\"emailId\":\"anita@example.org\",\"unitId\":5}"),
                null);
        assertThat(created.getId()).isEqualTo(1001L);
        assertThat(created.getUnitId()).isEqualTo(5L);
        assertThat(created.getStatus()).isTrue();

        Optional<? extends MasterEntityBase> readBack = repository.findByIdScoped(Doctor.class, 1001L, 5L, Map.of());
        assertThat(readBack).isPresent();
        assertThat(((Doctor) readBack.get()).getFirstName()).isEqualTo("Anita");

        // generic list with search on lastName
        List<? extends MasterEntityBase> list =
                readUseCase.getScoped("doctor", 5L, Map.of(), "Sharma", "lastName", 0, 0);
        assertThat(list).extracting(e -> e.getId()).contains(1001L);

        MasterEntityBase updated = writeUseCase.update("doctor", 1001L,
                mapper.readTree("{\"lastName\":\"Sharma-Rao\",\"mobileNo\":null}"), true, null, 5L);
        assertThat(((Doctor) updated).getLastName()).isEqualTo("Sharma-Rao");
        assertThat(((Doctor) updated).getMobileNo()).isEqualTo("9876500001");  // nonNull=true keeps it

        writeUseCase.remove("doctor", 1001L, 5L);
        assertThat(repository.findByIdScoped(Doctor.class, 1001L, 5L, Map.of())).isEmpty();
    }

    @Test
    void doctor_requiredFieldsEnforced() throws Exception {
        assertThatThrownBy(() -> writeUseCase.create("doctor",
                mapper.readTree("{\"lastName\":\"OnlyLast\",\"unitId\":1}"), null))
                .isInstanceOf(InvalidMasterRequestException.class);
    }

    @Test
    void doctorType_andSpecialization_create() throws Exception {
        MasterEntityBase dt = writeUseCase.create("doctor-type",
                mapper.readTree("{\"code\":\"RMO\",\"description\":\"Resident Medical Officer\",\"unitId\":1}"), null);
        assertThat(dt.getId()).isEqualTo(2001L);

        MasterEntityBase spec = writeUseCase.create("specialization",
                mapper.readTree("{\"code\":\"CARD\",\"description\":\"Cardiology\",\"unitId\":1}"), null);
        assertThat(spec.getId()).isEqualTo(3001L);
        assertThat(((Specialization) spec).getIsDoctorShow()).isTrue();
    }

    @Test
    void facilityLocation_hierarchy_create() throws Exception {
        MasterEntityBase building = writeUseCase.create("facility-location",
                mapper.readTree("{\"locationType\":\"BUILDING\",\"code\":\"B1\",\"name\":\"Main Building\",\"unitId\":1}"), null);
        assertThat(building.getId()).isEqualTo(4001L);

        MasterEntityBase floor = writeUseCase.create("facility-location",
                mapper.readTree("{\"locationType\":\"FLOOR\",\"code\":\"F1\",\"name\":\"First Floor\",\"parentId\":4001,\"unitId\":1}"), null);
        assertThat(floor.getId()).isEqualTo(4002L);

        List<? extends MasterEntityBase> list =
                readUseCase.getScoped("facility-location", 1L, Map.of("locationType", "BUILDING"), null, null, 0, 0);
        assertThat(list).extracting(e -> e.getId()).containsExactly(4001L);
    }
}

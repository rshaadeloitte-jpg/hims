package com.hims.patient.infrastructure.persistence;

import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.repository.PatientRegistrationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientSearchH2Test {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> "jdbc:h2:mem:ihms_patient_search;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS \"ihms_patient\"\\;CREATE SCHEMA IF NOT EXISTS \"ihms_pmrd\"");
    }

    @Autowired EntityManager em;
    @Autowired PatientRegistrationRepository repo;

    private long nextId() {
        return ((Number) em.createNativeQuery("SELECT nextval('\"ihms_patient\".\"patient_reg_seq\"')").getSingleResult()).longValue();
    }

    private PatientRegistration seed(String first, String last, String mrno, String mobile, String aadhaar, String abha) {
        PatientRegistration p = new PatientRegistration();
        p.setId(nextId());
        p.setMrno(mrno);
        p.setFirstName(first);
        p.setLastName(last);
        p.setGender("M");
        p.setMobileNo(mobile);
        p.setAadhaarNo(aadhaar);
        p.setHealthIdNumber(abha);
        p.setStatus(true);
        p.setAddedDatetime(Timestamp.from(Instant.now()));
        return repo.saveAndFlush(p);
    }

    @Test
    void searchByNameMrnMobileTypeRestricted() {
        seed("TESTCLAUDE", "USER", "MRN001", "9000000001", "123456789012", "abha@abdm");
        seed("Other", "Person", "MRN002", "9000000002", "999999999999", "other@abdm");

        assertThat(repo.search("TESTCLAUDE", "ALL")).hasSize(1);
        assertThat(repo.search("TESTCLAUDE", "NAME")).hasSize(1);
        assertThat(repo.search("9000000001", "MOBILE")).hasSize(1);
        assertThat(repo.search("MRN001", "MRN")).hasSize(1);
        assertThat(repo.search("123456789012", "AADHAAR")).hasSize(1);
        assertThat(repo.search("abha@abdm", "ABHA")).hasSize(1);
        // type-restricted should not match other fields
        assertThat(repo.search("TESTCLAUDE", "MOBILE")).isEmpty();
        // blank q returns [] via service, but repo with blank may return all? service handles blank. repo direct not required.
        // full name contains
        assertThat(repo.search("TESTCLAUDE USER", "NAME")).hasSize(1);
    }

    @Test
    void inactiveExcluded() {
        PatientRegistration p = seed("Inactive", "User", "MRNINACT", "9000000099", null, null);
        p.setStatus(false);
        repo.saveAndFlush(p);
        assertThat(repo.search("Inactive", "ALL")).isEmpty();
    }
}
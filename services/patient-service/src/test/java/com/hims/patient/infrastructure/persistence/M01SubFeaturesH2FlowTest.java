package com.hims.patient.infrastructure.persistence;

import com.hims.patient.domain.model.PatientAadhaarVerification;
import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.model.PatientSocialAssessment;
import com.hims.patient.domain.repository.PatientAadhaarVerificationRepository;
import com.hims.patient.domain.repository.PatientRegistrationRepository;
import com.hims.patient.domain.repository.PatientSocialAssessmentRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class M01SubFeaturesH2FlowTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> "jdbc:h2:mem:ihms_patient;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS \"ihms_patient\"");
    }

    @Autowired EntityManager em;
    @Autowired PatientRegistrationRepository patientRepo;
    @Autowired PatientAadhaarVerificationRepository aadhaarRepo;
    @Autowired PatientSocialAssessmentRepository socialRepo;

    private long nextId(String seqName) {
        return ((Number) em.createNativeQuery("SELECT nextval('\"ihms_patient\".\"" + seqName + "\"')").getSingleResult()).longValue();
    }
    private long nextAadhaarId() { return nextId("patient_aadhaar_verif_seq"); }
    private long nextSocialId() { return nextId("patient_social_assess_seq"); }
    private long nextRegId() { return nextId("patient_reg_seq"); }

    @Test
    void aadhaarVerification_persistAndFindByPatientId() {
        PatientAadhaarVerification v = new PatientAadhaarVerification();
        v.setId(nextAadhaarId());
        v.setPatientId(100L);
        v.setAadhaarNo("XXXXXXXX1234");
        v.setVerificationType("OTP");
        v.setVerified(false);
        v.setAttempts(0);
        v.setAddedDatetime(Timestamp.from(Instant.now()));
        aadhaarRepo.saveAndFlush(v);

        List<PatientAadhaarVerification> found = aadhaarRepo.findByPatientIdOrderByIdDesc(100L);
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getAadhaarNo()).isEqualTo("XXXXXXXX1234");
    }

    @Test
    void aadhaarVerification_findTopByPatientIdOrderByIdDesc() {
        for (int i = 0; i < 3; i++) {
            PatientAadhaarVerification v = new PatientAadhaarVerification();
            v.setId(nextAadhaarId());
            v.setPatientId(200L);
            v.setAadhaarNo("XXXX" + i);
            v.setVerificationType("BIOMETRIC");
            v.setVerified(true);
            v.setAttempts(i);
            v.setAddedDatetime(Timestamp.from(Instant.now()));
            aadhaarRepo.saveAndFlush(v);
        }
        Optional<PatientAadhaarVerification> top = aadhaarRepo.findTopByPatientIdOrderByIdDesc(200L);
        assertThat(top).isPresent();
        assertThat(top.get().getAttempts()).isEqualTo(2);
    }

    @Test
    void aadhaarVerification_statusTransition_persistsCorrectly() {
        PatientAadhaarVerification v = new PatientAadhaarVerification();
        v.setId(nextAadhaarId());
        v.setPatientId(300L);
        v.setAadhaarNo("YYYY1234");
        v.setVerificationType("OTP");
        v.setVerified(false);
        v.setAttempts(0);
        v.setAddedDatetime(Timestamp.from(Instant.now()));
        aadhaarRepo.saveAndFlush(v);

        v.setVerified(true);
        v.setAttempts(1);
        v.setVerifiedAt(Timestamp.from(Instant.now()));
        aadhaarRepo.saveAndFlush(v);

        PatientAadhaarVerification reloaded = aadhaarRepo.findById(v.getId()).orElseThrow();
        assertThat(reloaded.getVerified()).isTrue();
        assertThat(reloaded.getVerifiedAt()).isNotNull();
        assertThat(reloaded.getAddedDatetime()).isNotNull();
    }

    @Test
    void socialAssessment_persistsJsonb_andSetsAuditBlock() {
        PatientSocialAssessment sa = new PatientSocialAssessment();
        sa.setId(nextSocialId());
        sa.setPatientId(400L);
        sa.setAssessmentData(Map.of("risk", "high", "notes", "substance abuse"));
        sa.setSocialWorkerId(1L);
        sa.setAddedDatetime(Timestamp.from(Instant.now()));
        socialRepo.saveAndFlush(sa);

        PatientSocialAssessment found = socialRepo.findById(sa.getId()).orElseThrow();
        assertThat(found.getPatientId()).isEqualTo(400L);
        assertThat((String) found.getAssessmentData().get("notes")).contains("substance abuse");
        assertThat(found.getSocialWorkerId()).isEqualTo(1L);
        assertThat(found.getAddedDatetime()).isNotNull();
    }

    @Test
    void socialAssessment_findByPatientId_returnsOrderedList() {
        for (int i = 0; i < 5; i++) {
            PatientSocialAssessment sa = new PatientSocialAssessment();
            sa.setId(nextSocialId());
            sa.setPatientId(500L);
            sa.setAssessmentData(Map.of("idx", String.valueOf(i)));
            sa.setSocialWorkerId((long) i);
            sa.setAddedDatetime(Timestamp.from(Instant.now()));
            socialRepo.saveAndFlush(sa);
        }
        List<PatientSocialAssessment> found = socialRepo.findByPatientIdOrderByIdDesc(500L);
        assertThat(found).hasSize(5);
    }

    @Test
    void aadhaarVerification_auditBlock_addedTimestampAutoSet() {
        PatientAadhaarVerification v = new PatientAadhaarVerification();
        v.setId(nextAadhaarId());
        v.setPatientId(700L);
        v.setAadhaarNo("ZZZZ1234");
        v.setVerificationType("OTP");
        v.setVerified(false);
        v.setAttempts(0);
        v.setAddedDatetime(Timestamp.from(Instant.now()));
        aadhaarRepo.saveAndFlush(v);
        assertThat(v.getAddedDatetime()).isNotNull();
    }

    @Test
    void socialAssessment_auditBlock_addedTimestampAutoSet() {
        PatientSocialAssessment sa = new PatientSocialAssessment();
        sa.setId(nextSocialId());
        sa.setPatientId(800L);
        sa.setAssessmentData(Map.of());
        sa.setSocialWorkerId(5L);
        sa.setAddedDatetime(Timestamp.from(Instant.now()));
        socialRepo.saveAndFlush(sa);
        assertThat(sa.getAddedDatetime()).isNotNull();
    }

    @Test
    void registration_persistAndFindByMrno() {
        long id = nextRegId();
        PatientRegistration p = new PatientRegistration();
        p.setId(id);
        p.setMrno("010126000001");
        p.setFirstName("A");
        p.setGender("M");
        p.setDob(LocalDate.of(1985, 1, 1));
        p.setAddedDatetime(Timestamp.from(Instant.now()));
        patientRepo.saveAndFlush(p);

        Optional<PatientRegistration> found = patientRepo.findByMrno("010126000001");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("A");
    }

    @Test
    void clean() {
        aadhaarRepo.deleteAllInBatch();
        socialRepo.deleteAllInBatch();
        assertThat(aadhaarRepo.count()).isZero();
        assertThat(socialRepo.count()).isZero();
    }
}

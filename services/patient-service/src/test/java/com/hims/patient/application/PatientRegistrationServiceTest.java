package com.hims.patient.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.patient.application.exceptions.InvalidPatientRequestException;
import com.hims.patient.application.exceptions.PatientNotFoundException;
import com.hims.patient.api.PatientMergeResult;
import com.hims.patient.domain.model.PatientAadhaarVerification;
import com.hims.patient.domain.model.PatientDocument;
import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.model.PatientSocialAssessment;
import com.hims.patient.domain.model.PatientSponsor;
import com.hims.patient.domain.repository.PatientAadhaarVerificationRepository;
import com.hims.patient.domain.repository.PatientDocumentRepository;
import com.hims.patient.domain.repository.PatientRegistrationRepository;
import com.hims.patient.domain.repository.PatientSocialAssessmentRepository;
import com.hims.patient.domain.repository.PatientSponsorRepository;
import com.hims.patient.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** L0 — BR-PAT-001 MRN generation, BR-PAT-002 duplicate matrix, server-owned field guards. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientRegistrationServiceTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private long idSeq = 0;

    @Mock private PatientRegistrationRepository repository;
    @Mock private PatientDocumentRepository documentRepository;
    @Mock private PatientSponsorRepository sponsorRepository;
    @Mock private PatientAadhaarVerificationRepository aadhaarVerificationRepository;
    @Mock private PatientSocialAssessmentRepository socialAssessmentRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;

    private PatientRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PatientRegistrationService(repository, documentRepository, sponsorRepository,
                null, null, aadhaarVerificationRepository, socialAssessmentRepository, outboxService, mapper);
        when(sponsorRepository.save(any(PatientSponsor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.repointToTarget(any(), any(), any(), any())).thenReturn(2);
        when(sponsorRepository.repointToTarget(any(), any(), any(), any())).thenReturn(1);
        ReflectionTestUtils.setField(service, "em", em);
        Query regSeq = mock(Query.class);
        when(regSeq.getSingleResult()).thenAnswer(inv -> ++idSeq);
        Query docSeq = mock(Query.class);
        when(docSeq.getSingleResult()).thenAnswer(inv -> 900L);
        Query sponsorSeq = mock(Query.class);
        when(sponsorSeq.getSingleResult()).thenAnswer(inv -> 800L);
        Query aadhaarSeq = mock(Query.class);
        when(aadhaarSeq.getSingleResult()).thenAnswer(inv -> 1000L);
        Query socialSeq = mock(Query.class);
        when(socialSeq.getSingleResult()).thenAnswer(inv -> 2000L);
        when(em.createNativeQuery(contains("patient_reg_seq"))).thenReturn(regSeq);
        when(em.createNativeQuery(contains("patient_document_seq"))).thenReturn(docSeq);
        when(em.createNativeQuery(contains("patient_sponsor_seq"))).thenReturn(sponsorSeq);
        when(em.createNativeQuery(contains("patient_aadhaar_verif_seq"))).thenReturn(aadhaarSeq);
        when(em.createNativeQuery(contains("patient_social_assess_seq"))).thenReturn(socialSeq);
        when(repository.save(any(PatientRegistration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any(PatientDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aadhaarVerificationRepository.save(any(PatientAadhaarVerification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(socialAssessmentRepository.save(any(PatientSocialAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PatientRegistration saved(long id, String mrno, String aadhaar, String healthIdNumber) {
        PatientRegistration p = new PatientRegistration();
        p.setId(id);
        p.setMrno(mrno);
        p.setFirstName("Ramesh");
        p.setGender("M");
        p.setDob(LocalDate.of(1990, 1, 1));
        p.setMobileNo("9876543210");
        p.setAadhaarNo(aadhaar);
        p.setHealthIdNumber(healthIdNumber);
        return p;
    }

    // -------------------------------------------------------------- BR-PAT-001

    @Test
    void register_derivesPermanentMrnFromSequenceId() throws Exception {
        PatientRegistration p = service.register(mapper.readTree(
                "{\"firstName\":\"Ramesh\",\"gender\":\"M\",\"dob\":\"1990-01-01\",\"mobileNo\":\"9876543210\"}"), 7L);
        String expectedSuffix = String.format("%09d", p.getId());
        assertThat(p.getMrno()).matches("\\d{6}" + expectedSuffix);
        assertThat(p.getId()).isPositive();
        assertThat(p.getIsTempMrno()).isFalse();
        assertThat(p.getUnitId()).isEqualTo(7L);
        assertThat(p.getAddedBy()).isEqualTo(PatientRegistrationService.AUDIT_ACTOR_ID);
    }

    @Test
    void generateMrno_formulaMatchesLegacyGenerateMrno() {
        assertThat(PatientRegistrationService.generateMrno(LocalDate.of(2026, 8, 22), 42L))
                .isEqualTo("220826000000042");
        assertThat(PatientRegistrationService.generateMrno(LocalDate.of(2026, 1, 5), 123456789L))
                .isEqualTo("050126123456789");
    }

    @Test
    void register_rejectsCallerSuppliedMrnoAsServerOwned() throws Exception {
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"R\",\"gender\":\"M\",\"mrno\":\"HACKED\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003")
                .hasMessageContaining("server-owned");
    }

    @Test
    void register_requiresFirstNameAndGender() throws Exception {
        assertThatThrownBy(() -> service.register(mapper.readTree("{\"firstName\":\"No Gender\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
        assertThatThrownBy(() -> service.register(null, null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
    }

    // -------------------------------------------------------------- BR-PAT-002

    @Test
    void register_aadhaarDuplicateRejectedBera011() throws Exception {
        when(repository.findFirstByAadhaarNo("111122223333"))
                .thenReturn(Optional.of(saved(9L, "010190000000009", "111122223333", null)));
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"Other\",\"gender\":\"F\",\"aadhaarNo\":\"111122223333\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011")
                .hasMessageContaining("010190000000009");
    }

    @Test
    void register_vidAndJanaadhaarDuplicatesRejected() throws Exception {
        when(repository.findFirstByVid("VID-1")).thenReturn(Optional.of(saved(9L, "M9", null, null)));
        when(repository.findFirstByJanaadhaarId("JAN-1")).thenReturn(Optional.of(saved(8L, "M8", null, null)));

        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"X\",\"gender\":\"M\",\"vid\":\"VID-1\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011");
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"Y\",\"gender\":\"M\",\"janaadhaarId\":\"JAN-1\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011");
    }

    @Test
    void register_abhaHealthIdNumberDuplicateRejectedBexa040WithExistingMrn() throws Exception {
        when(repository.findFirstByHealthIdNumberIgnoreCase("ABHA-777"))
                .thenReturn(Optional.of(saved(5L, "010190000000005", null, "abha-777")));
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"Z\",\"gender\":\"M\",\"healthIdNumber\":\"ABHA-777\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA040")
                .hasMessageContaining("010190000000005");   // existing MRN in message
    }

    @Test
    void register_mobileNameDobHeuristicHitRejected() throws Exception {
        when(repository.findIdentityHits("9876543210", "Ramesh", LocalDate.of(1990, 1, 1)))
                .thenReturn(List.of(saved(3L, "M3", null, null)));
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"firstName\":\"Ramesh\",\"gender\":\"M\",\"dob\":\"1990-01-01\",\"mobileNo\":\"9876543210\"}"), null))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011");
    }

    @Test
    void register_uniqueIdentityPasses_andEmitsOutboxEvent() throws Exception {
        PatientRegistration p = service.register(mapper.readTree(
                "{\"firstName\":\"Sunita\",\"gender\":\"F\",\"mobileNo\":\"9000000001\"}"), null);
        assertThat(p.getId()).isPositive();
        org.mockito.Mockito.verify(outboxService).append(
                org.mockito.ArgumentMatchers.eq("PatientRegistered"),
                org.mockito.ArgumentMatchers.eq("PatientRegistration"),
                org.mockito.ArgumentMatchers.eq(p.getId()),
                org.mockito.ArgumentMatchers.eq(String.valueOf(p.getId())),
                anyString(), anyString());
    }

    // ------------------------------------------------------------------ update

    @Test
    void update_appliesDemographics_neverCopiesServerOwnedFields() throws Exception {
        PatientRegistration existing = saved(12L, "010190000000012", null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(existing));

        PatientRegistration out = service.update(12L, mapper.readTree(
                "{\"firstName\":\"RameshChandra\",\"city\":\"Jaipur\",\"mrno\":\"FORGED\","
                        + "\"id\":999,\"addedBy\":666,\"status\":false}"));

        assertThat(out.getFirstName()).isEqualTo("RameshChandra");
        assertThat(out.getCity()).isEqualTo("Jaipur");
        assertThat(out.getMrno()).isEqualTo("010190000000012");   // untouched
        assertThat(out.getId()).isEqualTo(12L);                    // untouched
        assertThat(out.getStatus()).isTrue();                      // not copied
        assertThat(out.getUpdatedBy()).isEqualTo(PatientRegistrationService.AUDIT_ACTOR_ID);
        org.mockito.Mockito.verify(outboxService).append(
                org.mockito.ArgumentMatchers.eq("PatientUpdated"), any(), any(), any(), anyString(), anyString());
    }

    @Test
    void update_rerunsMatrixOnIdentityChange() throws Exception {
        PatientRegistration existing = saved(12L, "M12", null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(existing));
        when(repository.findFirstByHealthIdNumberIgnoreCase("TAKEN")).thenReturn(
                Optional.of(saved(20L, "M20", null, "TAKEN")));

        assertThatThrownBy(() -> service.update(12L, mapper.readTree("{\"healthIdNumber\":\"TAKEN\"}")))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA040");
    }

    @Test
    void update_unknownPatient404() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(404L, mapper.readTree("{\"city\":\"J\"}")))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // --------------------------------------------------------------- documents

    @Test
    void linkDocument_requiresTypeAndRef_andKnownPatient() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        PatientDocument d = service.linkDocument(12L,
                mapper.readTree("{\"docType\":\"AADHAAR\",\"docRef\":\"obj/pat/12/aadhaar.pdf\"}"));
        assertThat(d.getPatientId()).isEqualTo(12L);
        assertThat(d.getVerified()).isFalse();

        assertThatThrownBy(() -> service.linkDocument(12L, mapper.readTree("{\"docType\":\"X\"}")))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasMessageContaining("docRef");
        when(repository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.linkDocument(77L,
                mapper.readTree("{\"docType\":\"X\",\"docRef\":\"y\"}")))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ---------------------------------------------------------------- sponsors

    @Test
    void linkSponsor_requiresType_andKnownPatient() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        PatientSponsor s = service.linkSponsor(12L,
                mapper.readTree("{\"sponsorType\":\"RGHS\",\"policyNo\":\"P-1\"}"));
        assertThat(s.getPatientId()).isEqualTo(12L);
        assertThat(s.getSponsorType()).isEqualTo("RGHS");
        assertThat(s.getAddedBy()).isEqualTo(PatientRegistrationService.AUDIT_ACTOR_ID);

        assertThatThrownBy(() -> service.linkSponsor(12L, mapper.readTree("{\"policyNo\":\"P-1\"}")))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003")
                .hasMessageContaining("sponsorType");

        when(repository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.linkSponsor(77L, mapper.readTree("{\"sponsorType\":\"RGHS\"}")))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ------------------------------------------------------------------ merge

    @Test
    void merge_repointsRows_deactivatesSource_emitsPatientMerged() {
        PatientRegistration source = saved(12L, "M12", null, null);
        PatientRegistration target = saved(20L, "M20", null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(source));
        when(repository.findById(20L)).thenReturn(Optional.of(target));

        PatientMergeResult r = service.merge(12L, 20L);

        assertThat(r.getSourceId()).isEqualTo(12L);
        assertThat(r.getTargetId()).isEqualTo(20L);
        assertThat(r.getDocumentsRepointed()).isEqualTo(2);
        assertThat(r.getSponsorsRepointed()).isEqualTo(1);

        org.mockito.Mockito.verify(documentRepository).repointToTarget(
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(12L),
                org.mockito.ArgumentMatchers.eq(PatientRegistrationService.AUDIT_ACTOR_ID), any());
        org.mockito.Mockito.verify(sponsorRepository).repointToTarget(
                org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(12L), any(), any());

        assertThat(source.getStatus()).isFalse();   // source deactivated

        org.mockito.Mockito.verify(outboxService).append(
                org.mockito.ArgumentMatchers.eq("PatientMerged"),
                org.mockito.ArgumentMatchers.eq("PatientRegistration"),
                org.mockito.ArgumentMatchers.eq(20L),          // aggregate + partition key = TARGET id
                org.mockito.ArgumentMatchers.eq("20"),
                anyString(),
                org.mockito.ArgumentMatchers.eq("PatientMerged:M12:M20"));
    }

    @Test
    void merge_rejectsSameIds_unknownIds_andInactiveSource() {
        assertThatThrownBy(() -> service.merge(5L, 5L))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasMessageContaining("must differ");

        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.merge(404L, 1L))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining("Source");

        PatientRegistration inactive = saved(12L, "M12", null, null);
        inactive.setStatus(false);
        when(repository.findById(12L)).thenReturn(Optional.of(inactive));
        when(repository.findById(20L)).thenReturn(Optional.of(saved(20L, "M20", null, null)));
        assertThatThrownBy(() -> service.merge(12L, 20L))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA012");
        org.mockito.Mockito.verify(outboxService, org.mockito.Mockito.never())
                .append(org.mockito.ArgumentMatchers.eq("PatientMerged"), any(), any(), any(), anyString(), anyString());
    }

    // -------------------------------------------------------------- M01: Aadhaar OTP/biometric

    @Test
    void aadhaarOtp_generates6Digit_withValidAadhaar() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        PatientAadhaarVerification v = service.requestAadhaarOtp(12L, mapper.readTree("{\"aadhaarNo\":\"234567890123\"}"));
        assertThat(v.getAadhaarNo()).isEqualTo("234567890123");
        assertThat(v.getVerificationType()).isEqualTo("OTP");
        assertThat(v.getOtp()).matches("\\d{6}");
        assertThat(v.getVerified()).isFalse();
        assertThat(v.getExpiresAt()).isNotNull();
    }

    @Test
    void aadhaarOtp_rejectsInvalidAadhaar() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        assertThatThrownBy(() -> service.requestAadhaarOtp(12L, mapper.readTree("{\"aadhaarNo\":\"123\"}")))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA003");
        assertThatThrownBy(() -> service.requestAadhaarOtp(12L, mapper.readTree("{\"aadhaarNo\":\"012345678901\"}")))
                .isInstanceOf(InvalidPatientRequestException.class);
    }

    @Test
    void aadhaarBiometric_immediateVerified() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        PatientAadhaarVerification v = service.requestAadhaarOtp(12L,
                mapper.readTree("{\"aadhaarNo\":\"234567890123\",\"verificationType\":\"BIOMETRIC\",\"deviceId\":\"RD-001\"}"));
        assertThat(v.getVerified()).isTrue();
        assertThat(v.getVerificationType()).isEqualTo("BIOMETRIC");
        assertThat(v.getDeviceId()).isEqualTo("RD-001");
    }

    @Test
    void aadhaarOtp_verify_successAndFailure() throws Exception {
        PatientRegistration p = saved(12L, "M12", null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(p));
        // generate
        PatientAadhaarVerification gen = service.requestAadhaarOtp(12L, mapper.readTree("{\"aadhaarNo\":\"234567890123\"}"));
        when(aadhaarVerificationRepository.findTopByPatientIdOrderByIdDesc(12L)).thenReturn(Optional.of(gen));
        // verify with correct otp
        PatientAadhaarVerification verified = service.requestAadhaarOtp(12L,
                mapper.readTree("{\"aadhaarNo\":\"234567890123\",\"otp\":\"" + gen.getOtp() + "\"}"));
        assertThat(verified.getVerified()).isTrue();

        // second verify should fail already verified
        PatientAadhaarVerification already = verified;
        when(aadhaarVerificationRepository.findTopByPatientIdOrderByIdDesc(12L)).thenReturn(Optional.of(already));
        assertThatThrownBy(() -> service.requestAadhaarOtp(12L,
                mapper.readTree("{\"aadhaarNo\":\"234567890123\",\"otp\":\"" + gen.getOtp() + "\"}")))
                .isInstanceOf(InvalidPatientRequestException.class)
                .hasMessageContaining("Already verified");
    }

    @Test
    void covidStatus_setsAndFilters() throws Exception {
        PatientRegistration p = saved(12L, "M12", null, null);
        when(repository.findById(12L)).thenReturn(Optional.of(p));
        PatientRegistration updated = service.updateCovidStatus(12L, mapper.readTree("{\"covidStatus\":\"POSITIVE\"}"));
        assertThat(updated.getCovidStatus()).isEqualTo("POSITIVE");
        assertThat(updated.getCovidUpdatedAt()).isNotNull();

        when(repository.findByCovidStatus("POSITIVE")).thenReturn(List.of(updated));
        assertThat(service.listCovidPatients("POSITIVE")).hasSize(1);
        when(repository.findAllCovidTracked()).thenReturn(List.of(updated));
        assertThat(service.listCovidPatients(null)).hasSize(1);
    }

    @Test
    void socialAssessment_requiresData_andPersists() throws Exception {
        when(repository.findById(12L)).thenReturn(Optional.of(saved(12L, "M12", null, null)));
        assertThatThrownBy(() -> service.createSocialAssessment(12L, mapper.readTree("{\"remarks\":\"x\"}")))
                .isInstanceOf(InvalidPatientRequestException.class);

        PatientSocialAssessment a = service.createSocialAssessment(12L,
                mapper.readTree("{\"assessmentData\":{\"housing\":\"kutcha\",\"income\":5000},\"socialWorkerId\":99,\"remarks\":\"BPL\"}"));
        assertThat(a.getPatientId()).isEqualTo(12L);
        assertThat(a.getAssessmentData()).containsEntry("housing", "kutcha");
        assertThat(a.getSocialWorkerId()).isEqualTo(99L);
    }

    @Test
    void isValidAadhaar_staticCheck() {
        assertThat(PatientRegistrationService.isValidAadhaar("234567890123")).isTrue();
        assertThat(PatientRegistrationService.isValidAadhaar("123456789012")).isFalse(); // leading 1 invalid
        assertThat(PatientRegistrationService.isValidAadhaar("23456789012")).isFalse();
        assertThat(PatientRegistrationService.isValidAadhaar(null)).isFalse();
    }
}

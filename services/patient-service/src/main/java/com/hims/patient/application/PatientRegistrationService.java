package com.hims.patient.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.patient.application.exceptions.InvalidPatientRequestException;
import com.hims.patient.application.exceptions.PatientNotFoundException;
import com.hims.patient.api.PatientMergeResult;
import com.hims.patient.domain.model.PatientAadhaarVerification;
import com.hims.patient.domain.model.PatientDocument;
import com.hims.patient.domain.model.PatientMlc;
import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.model.PatientRelation;
import com.hims.patient.domain.model.PatientSocialAssessment;
import com.hims.patient.domain.model.PatientSponsor;
import com.hims.patient.domain.repository.PatientAadhaarVerificationRepository;
import com.hims.patient.domain.repository.PatientDocumentRepository;
import com.hims.patient.domain.repository.PatientMlcRepository;
import com.hims.patient.domain.repository.PatientRegistrationRepository;
import com.hims.patient.domain.repository.PatientRelationRepository;
import com.hims.patient.domain.repository.PatientSocialAssessmentRepository;
import com.hims.patient.domain.repository.PatientSponsorRepository;
import com.hims.patient.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patient registration lifecycle (MIG-030 / S01 kickoff) — clean target of
 * PatientServiceImpl.registerPatient / PatientServiceHelper MRN + duplicate flow.
 *
 * BR-PAT-001 (MRN): permanent MRN = ddMMYY + patientId padded to 9 digits
 *   (legacy BaseServiceImpl.generateMrno). patientId comes from nextval of
 *   patient_reg_seq INSIDE the registration transaction → atomic; the formula
 *   is a pure function of (registration date, patientId) → idempotent per id.
 *   mrno has a UNIQUE index: an actual collision surfaces as BERA011.
 *
 * BR-PAT-002 (duplicates, target replaces Solr findPatientByIdentity with DB
 * lookups — Solr retires):
 *   mobileNo + firstName (+dob) legacy heuristic hit  → BERA011
 *   Aadhaar/VID match                                  → BERA011
 *   Janaadhaar match                                   → BERA011
 *   ABHA healthIdNumber match                          → BEXA040 (+existing MRN)
 *
 * Events (E-01/E-01b): PatientRegistered on register, PatientUpdated on update,
 * appended to the outbox IN THE SAME TX, relayed to ihms.patient.registration,
 * partition key patientId (ordering per KAFKA-EVENT-CATALOG §2 E-01).
 *
 * Deferred (IMP-DEC-039): temp-MRN series variants (mrnoFromSeries /
 * autoGenerateTempMrnNo — both default false in legacy), offline-unit client-MRN
 * preservation, family-id coupling (BR-PAT-003 → BERP007), sponsor/scheme
 * linkage, Solr sync consumer.
 */
@Service
public class PatientRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(PatientRegistrationService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private static final DateTimeFormatter MRN_DATE = DateTimeFormatter.ofPattern("ddMMyy");

    private final PatientRegistrationRepository repository;
    private final PatientDocumentRepository documentRepository;
    private final PatientSponsorRepository sponsorRepository;
    private final PatientMlcRepository mlcRepository;
    private final PatientRelationRepository relationRepository;
    private final PatientAadhaarVerificationRepository aadhaarVerificationRepository;
    private final PatientSocialAssessmentRepository socialAssessmentRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    public PatientRegistrationService(PatientRegistrationRepository repository,
                                      PatientDocumentRepository documentRepository,
                                      PatientSponsorRepository sponsorRepository,
                                      PatientMlcRepository mlcRepository,
                                      PatientRelationRepository relationRepository,
                                      PatientAadhaarVerificationRepository aadhaarVerificationRepository,
                                      PatientSocialAssessmentRepository socialAssessmentRepository,
                                      OutboxService outboxService,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.sponsorRepository = sponsorRepository;
        this.mlcRepository = mlcRepository;
        this.relationRepository = relationRepository;
        this.aadhaarVerificationRepository = aadhaarVerificationRepository;
        this.socialAssessmentRepository = socialAssessmentRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    // Compatibility for tests created before M01 slice — delegates to full ctor
    public PatientRegistrationService(PatientRegistrationRepository repository,
                                      PatientDocumentRepository documentRepository,
                                      PatientSponsorRepository sponsorRepository,
                                      OutboxService outboxService,
                                      ObjectMapper objectMapper) {
        this(repository, documentRepository, sponsorRepository, null, null, null, null, outboxService, objectMapper);
    }

    public PatientRegistrationService(PatientRegistrationRepository repository,
                                      PatientDocumentRepository documentRepository,
                                      PatientSponsorRepository sponsorRepository,
                                      PatientMlcRepository mlcRepository,
                                      PatientRelationRepository relationRepository,
                                      OutboxService outboxService,
                                      ObjectMapper objectMapper) {
        this(repository, documentRepository, sponsorRepository, mlcRepository, relationRepository, null, null, outboxService, objectMapper);
    }

    // -------------------------------------------------------------- register

    /**
     * Register a patient (legacy registerPatient core). Server assigns id via
     * sequence, derives the permanent MRN, runs the BR-PAT-002 duplicate matrix,
     * and appends PatientRegistered to the outbox in the SAME transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientRegistration register(JsonNode payload, Long unitId) {
        PatientRegistration p = bind(payload, PatientRegistration.class);
        requireCore(p);

        // Parse compound single-string names (legacy behavior)
        parseCompoundName(p);

        runDuplicateMatrix(p, null);

        p.setId(nextPatientId());
        p.setMrno(generateMrno(LocalDate.now(ZoneId.systemDefault()), p.getId()));
        p.setIsTempMrno(Boolean.TRUE.equals(p.getIsTempMrno()));
        if (p.getStatus() == null) {
            p.setStatus(true);
        }
        if (unitId != null) {
            p.setUnitId(unitId);
        }
        p.setSynchronizedd(false);
        p.setAddedBy(AUDIT_ACTOR_ID);
        p.setAddedDatetime(Timestamp.from(Instant.now()));

        PatientRegistration saved = repository.save(p);
        appendEvent("PatientRegistered", saved);
        log.info("Patient registered id={} mrno={} name={} {}",
                saved.getId(), saved.getMrno(), saved.getFirstName(), saved.getLastName());
        return saved;
    }

    // ---------------------------------------------------------------- update

    /** Update demographics (nonNull fields only). Emits PatientUpdated. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientRegistration update(Long id, JsonNode payload) {
        PatientRegistration existing = repository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        if (!Boolean.TRUE.equals(existing.getStatus())) {
            throw new InvalidPatientRequestException("BEXA003", "Invalid input data !");
        }

        PatientRegistration changes = bind(payload, PatientRegistration.class);
        applyChanges(existing, changes);

        if (changes.getHealthIdNumber() != null || changes.getAadhaarNo() != null
                || changes.getVid() != null || changes.getJanaadhaarId() != null
                || changes.getMobileNo() != null) {
            runDuplicateMatrix(existing, existing.getId());
        }

        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        PatientRegistration saved = repository.save(existing);
        appendEvent("PatientUpdated", saved);
        log.info("Patient {} updated", saved.getId());
        return saved;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PatientRegistration get(Long id) {
        PatientRegistration p = repository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        return p;
    }

    @Transactional(readOnly = true)
    public List<PatientRegistration> search(String mobile, String aadhaar, String healthIdNumber) {
        if (healthIdNumber != null && !healthIdNumber.isBlank()) {
            return repository.findFirstByHealthIdNumberIgnoreCase(healthIdNumber).map(List::of).orElse(List.of());
        }
        if (aadhaar != null && !aadhaar.isBlank()) {
            return repository.findFirstByAadhaarNo(aadhaar).map(List::of).orElse(List.of());
        }
        if (mobile != null && !mobile.isBlank()) {
            return repository.findByMobileNoAndStatusTrueOrderByIdAsc(mobile);
        }
        return List.of();
    }

    // -------------------------------------------------------------- documents

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientDocument linkDocument(Long patientId, JsonNode payload) {
        PatientDocument d = bind(payload, PatientDocument.class);
        if (d.getDocType() == null || d.getDocType().isBlank() || d.getDocRef() == null || d.getDocRef().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "docType and docRef are required");
        }
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        d.setId(nextDocId());
        d.setPatientId(owner.getId());
        if (d.getVerified() == null) {
            d.setVerified(false);
        }
        if (owner.getUnitId() != null) {
            d.setUnitId(owner.getUnitId());
        }
        d.setSynchronizedd(false);
        d.setAddedBy(AUDIT_ACTOR_ID);
        d.setAddedDatetime(Timestamp.from(Instant.now()));
        return documentRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<PatientDocument> documents(Long patientId) {
        return documentRepository.findByPatientIdAndStatusTrueOrderByIdAsc(patientId);
    }

    // ---------------------------------------------------------------- sponsors

    /** Link a sponsor/scheme row (T_PATIENTSPONSOR clean target) to the patient. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientSponsor linkSponsor(Long patientId, JsonNode payload) {
        PatientSponsor s = bind(payload, PatientSponsor.class);
        if (s.getSponsorType() == null || s.getSponsorType().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "sponsorType is required");
        }
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        s.setId(nextSponsorId());
        s.setPatientId(owner.getId());
        if (owner.getUnitId() != null) {
            s.setUnitId(owner.getUnitId());
        }
        s.setSynchronizedd(false);
        s.setAddedBy(AUDIT_ACTOR_ID);
        s.setAddedDatetime(Timestamp.from(Instant.now()));
        return sponsorRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<PatientSponsor> sponsors(Long patientId) {
        return sponsorRepository.findByPatientIdAndStatusTrueOrderByIdAsc(patientId);
    }

    // ------------------------------------------------------------------ merge

    /**
     * Merge a duplicate source patient into a target: re-point documents and
     * sponsor rows to the target, deactivate the source (status=false), emit
     * PatientMerged via the transactional outbox in the SAME tx. Partition key
     * is the TARGET patientId so consumers see merge after that patient's own
     * events in order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientMergeResult merge(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null) {
            throw new InvalidPatientRequestException("BEXA003", "sourceId and targetId are required");
        }
        if (sourceId.equals(targetId)) {
            throw new InvalidPatientRequestException("BEXA003", "sourceId and targetId must differ");
        }
        PatientRegistration source = repository.findById(sourceId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Source patient not found"));
        PatientRegistration target = repository.findById(targetId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Target patient not found"));
        if (!Boolean.TRUE.equals(source.getStatus())) {
            throw new InvalidPatientRequestException("BEXA012", "Source patient already merged or inactive");
        }

        Timestamp now = Timestamp.from(Instant.now());
        int docsMoved = documentRepository.repointToTarget(target.getId(), source.getId(), AUDIT_ACTOR_ID, now);
        int sponsorsMoved = sponsorRepository.repointToTarget(target.getId(), source.getId(), AUDIT_ACTOR_ID, now);

        source.setStatus(false);
        source.setUpdatedBy(AUDIT_ACTOR_ID);
        source.setUpdatedDatetime(now);
        repository.save(source);

        appendMergeEvent(target, source);
        log.info("Patient {} ({}) merged into {} ({}): documents={} sponsors={}",
                source.getId(), source.getMrno(), target.getId(), target.getMrno(), docsMoved, sponsorsMoved);
        return PatientMergeResult.of(source.getId(), source.getMrno(), target.getId(), target.getMrno(),
                docsMoved, sponsorsMoved);
    }

    private void appendMergeEvent(PatientRegistration target, PatientRegistration source) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sourceId", source.getId());
            payload.put("sourceMrno", source.getMrno());
            payload.put("targetId", target.getId());
            payload.put("targetMrno", target.getMrno());
            payload.put("unitId", target.getUnitId());

            outboxService.append("PatientMerged", "PatientRegistration", target.getId(),
                    String.valueOf(target.getId()),                 // partition key: surviving patientId
                    objectMapper.writeValueAsString(payload),
                    "PatientMerged:" + source.getMrno() + ":" + target.getMrno());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize patient-merge event payload", e);
        }
    }

    private long nextSponsorId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_sponsor_seq\"')")
                .getSingleResult()).longValue();
    }

    // ------------------------------------------------------------- duplicates

    /** BR-PAT-002 matrix. excludeId lets updates skip self-hits. */
    private void runDuplicateMatrix(PatientRegistration candidate, Long excludeId) {
        if (candidate.getHealthIdNumber() != null && !candidate.getHealthIdNumber().isBlank()) {
            repository.findFirstByHealthIdNumberIgnoreCase(candidate.getHealthIdNumber())
                    .filter(hit -> !hit.getId().equals(excludeId))
                    .ifPresent(hit -> {
                        throw new InvalidPatientRequestException("BEXA040",
                                "ABHA healthIdNumber already registered for MRN " + hit.getMrno());
                    });
        }
        // Hibernate 6 renders a null bind as IS NULL on derived queries — a
        // null candidate value would match every row with a NULL column.
        // Guard each lookup on a non-null candidate value instead.
        if (candidate.getAadhaarNo() != null) {
            checkIdentity(repository.findFirstByAadhaarNo(candidate.getAadhaarNo()), excludeId);
        }
        if (candidate.getVid() != null) {
            checkIdentity(repository.findFirstByVid(candidate.getVid()), excludeId);
        }
        if (candidate.getJanaadhaarId() != null) {
            checkIdentity(repository.findFirstByJanaadhaarId(candidate.getJanaadhaarId()), excludeId);
        }
        if (candidate.getMobileNo() != null && candidate.getFirstName() != null) {
            List<PatientRegistration> hits = candidate.getDob() != null
                    ? repository.findIdentityHits(candidate.getMobileNo(), candidate.getFirstName(), candidate.getDob())
                    : repository.findIdentityHitsWithoutDob(candidate.getMobileNo(), candidate.getFirstName());
            boolean hit = hits.stream().anyMatch(h -> !h.getId().equals(excludeId));
            if (hit) {
                throw new InvalidPatientRequestException("BERA011",
                        "Duplicate patient identity: same mobile/name/dob already registered");
            }
        }
    }

    private void checkIdentity(java.util.Optional<PatientRegistration> hit, Long excludeId) {
        hit.filter(h -> h.getId() != null && !h.getId().equals(excludeId))
                .ifPresent(h -> {
                    throw new InvalidPatientRequestException("BERA011",
                            "Duplicate patient identity on government id (MRN " + h.getMrno() + ")");
                });
    }

    // ---------------------------------------------------------------- helpers

    /** E-01 payload subset per KAFKA-EVENT-CATALOG §2 (kickoff fields). */
    private void appendEvent(String eventType, PatientRegistration p) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("patientId", p.getId());
            payload.put("mrno", p.getMrno());
            payload.put("firstName", p.getFirstName());
            payload.put("middleName", p.getMiddleName());
            payload.put("lastName", p.getLastName());
            payload.put("gender", p.getGender());
            payload.put("dob", p.getDob() == null ? null : p.getDob().toString());
            payload.put("mobileNo", p.getMobileNo());
            payload.put("aadhaarNo", p.getAadhaarNo());
            payload.put("vid", p.getVid());
            payload.put("janaadhaarId", p.getJanaadhaarId());
            payload.put("healthId", p.getHealthId());
            payload.put("healthIdNumber", p.getHealthIdNumber());
            payload.put("bloodGroup", p.getBloodGroup());
            payload.put("unitId", p.getUnitId());

            outboxService.append(eventType, "PatientRegistration", p.getId(),
                    String.valueOf(p.getId()),                       // partition key: patientId (E-01 ordering)
                    objectMapper.writeValueAsString(payload),
                    eventType + ":" + p.getMrno());                  // consumer dedupe boundary
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize patient event payload", e);
        }
    }

    /**
     * BR-PAT-001: permanent MRN = ddMMYY + patientId padded to 9 digits
     * (legacy BaseServiceImpl.generateMrno). Pure function of (date, id).
     */
    static String generateMrno(LocalDate registrationDate, long patientId) {
        return MRN_DATE.format(registrationDate) + String.format("%09d", patientId);
    }

    /**
     * Series-based MRN generation (legacy mrnoFromSeries strategy).
     * Uses a dedicated sequence for series-based MRNs.
     */
    static String generateSeriesMrno(LocalDate registrationDate, long seriesSeq) {
        return "S" + MRN_DATE.format(registrationDate) + String.format("%07d", seriesSeq);
    }

    /**
     * Temporary MRN generation for walk-in appointments (legacy autoGenerateTempMrnNo).
     * Uses a temporary sequence prefix to distinguish from permanent MRNs.
     */
    static String generateTempMrno(LocalDate registrationDate, long tempSeq) {
        return "T" + MRN_DATE.format(registrationDate) + String.format("%08d", tempSeq);
    }

    private long nextPatientId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_reg_seq\"')")
                .getSingleResult()).longValue();
    }

    private long nextDocId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_document_seq\"')")
                .getSingleResult()).longValue();
    }

    private long nextMlcId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_mlc_seq\"')")
                .getSingleResult()).longValue();
    }

    private long nextRelationId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_relation_seq\"')")
                .getSingleResult()).longValue();
    }

    /**
     * Explicit allow-list of updatable demographics. Server-owned identity and
     * audit fields (id, mrno, isTempMrno, unitId, addedBy/addedDatetime,
     * status, synchronized) are NEVER copied from the payload (IMP-DEC-023).
     */
    private void applyChanges(PatientRegistration target, PatientRegistration src) {
        if (src.getFirstName() != null) {
            if (src.getFirstName().isBlank()) throw new InvalidPatientRequestException("BEXA003", "Invalid input data !");
            target.setFirstName(src.getFirstName());
        }
        if (src.getMiddleName() != null) target.setMiddleName(src.getMiddleName());
        if (src.getLastName() != null) target.setLastName(src.getLastName());
        if (src.getGender() != null) target.setGender(src.getGender());
        if (src.getDob() != null) target.setDob(src.getDob());
        if (src.getBloodGroup() != null) target.setBloodGroup(src.getBloodGroup());
        if (src.getMobileNo() != null) target.setMobileNo(src.getMobileNo());
        if (src.getEmail() != null) target.setEmail(src.getEmail());
        if (src.getAadhaarNo() != null) target.setAadhaarNo(src.getAadhaarNo());
        if (src.getVid() != null) target.setVid(src.getVid());
        if (src.getJanaadhaarId() != null) target.setJanaadhaarId(src.getJanaadhaarId());
        if (src.getHealthId() != null) target.setHealthId(src.getHealthId());
        if (src.getHealthIdNumber() != null) target.setHealthIdNumber(src.getHealthIdNumber());
        if (src.getAddressLine1() != null) target.setAddressLine1(src.getAddressLine1());
        if (src.getCity() != null) target.setCity(src.getCity());
        if (src.getDistrict() != null) target.setDistrict(src.getDistrict());
        if (src.getState() != null) target.setState(src.getState());
        if (src.getPincode() != null) target.setPincode(src.getPincode());
        if (src.getEmergencyContactName() != null) target.setEmergencyContactName(src.getEmergencyContactName());
        if (src.getEmergencyContactPhone() != null) target.setEmergencyContactPhone(src.getEmergencyContactPhone());
        if (src.getPlaceOfBirth() != null) target.setPlaceOfBirth(src.getPlaceOfBirth());
        if (src.getPhotoRef() != null) target.setPhotoRef(src.getPhotoRef());
    }

    // --------------------------------------------------------------------- MLC

    /** Create an MLC record for a patient (M01 gap: MLC capture). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientMlc saveMlc(Long patientId, JsonNode payload) {
        PatientMlc mlc = bind(payload, PatientMlc.class);
        if (mlc.getMlcCase() == null || mlc.getMlcCase().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "mlcCase is required");
        }
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        mlc.setId(nextMlcId());
        mlc.setPatientId(owner.getId());
        if (owner.getUnitId() != null) {
            mlc.setUnitId(owner.getUnitId());
        }
        mlc.setSynchronizedd(false);
        mlc.setAddedBy(AUDIT_ACTOR_ID);
        mlc.setAddedDatetime(Timestamp.from(Instant.now()));
        log.info("MLC record created for patient {} case={}", patientId, mlc.getMlcCase());
        return mlcRepository.save(mlc);
    }

    @Transactional(readOnly = true)
    public List<PatientMlc> mlcRecords(Long patientId) {
        return mlcRepository.findByPatientIdAndStatusTrueOrderByIdDesc(patientId);
    }

    // --------------------------------------------------------------- Relations

    /** Link two patients with a relationship type (M01 gap: mother-child linking). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientRelation linkRelation(Long patientId, JsonNode payload) {
        PatientRelation rel = bind(payload, PatientRelation.class);
        if (rel.getRelatedPatientId() == null || rel.getRelationType() == null || rel.getRelationType().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "relatedPatientId and relationType are required");
        }
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        repository.findById(rel.getRelatedPatientId())
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Related patient not found"));
        rel.setId(nextRelationId());
        rel.setPatientId(owner.getId());
        if (owner.getUnitId() != null) {
            rel.setUnitId(owner.getUnitId());
        }
        if (rel.getIsPrimary() == null) {
            rel.setIsPrimary(false);
        }
        rel.setSynchronizedd(false);
        rel.setAddedBy(AUDIT_ACTOR_ID);
        rel.setAddedDatetime(Timestamp.from(Instant.now()));
        log.info("Patient {} linked to {} as {}", patientId, rel.getRelatedPatientId(), rel.getRelationType());
        return relationRepository.save(rel);
    }

    @Transactional(readOnly = true)
    public List<PatientRelation> relations(Long patientId) {
        return relationRepository.findByPatientIdAndStatusTrueOrderByIdAsc(patientId);
    }

    // -------------------------------------------------------------- M01: Aadhaar OTP/biometric
    // Ported from IntegrationServiceImpl.aadhaarAuthenticate (AES/CBC encrypt) + FamilyMemberDetails.biometricDetailsJson.
    // Ponytail: local 6-digit OTP, 5-min TTL, 3 attempts — no external RISL call; BIOMETRIC is audit-only.

    private static final java.util.regex.Pattern AADHAAR_12 = java.util.regex.Pattern.compile("^[2-9][0-9]{11}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_TTL_MS = 5 * 60 * 1000L;
    private static final int OTP_MAX_ATTEMPTS = 3;

    static boolean isValidAadhaar(String v) {
        return v != null && AADHAAR_12.matcher(v).matches();
    }

    private long nextAadhaarVerifId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_aadhaar_verif_seq\"')")
                .getSingleResult()).longValue();
    }

    private long nextSocialAssessId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_patient.\"patient_social_assess_seq\"')")
                .getSingleResult()).longValue();
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientAadhaarVerification requestAadhaarOtp(Long patientId, JsonNode body) {
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        String aadhaarNo = body != null && body.has("aadhaarNo") ? body.get("aadhaarNo").asText(null) : null;
        String verificationType = body != null && body.has("verificationType") ? body.get("verificationType").asText("OTP") : "OTP";
        String deviceId = body != null && body.has("deviceId") ? body.get("deviceId").asText(null) : null;
        String otpSupplied = body != null && body.has("otp") ? body.get("otp").asText(null) : null;

        if (aadhaarNo == null || aadhaarNo.isBlank()) {
            // fallback to patient's stored aadhaar if not supplied
            aadhaarNo = owner.getAadhaarNo();
        }
        if (!isValidAadhaar(aadhaarNo)) {
            throw new InvalidPatientRequestException("BEXA003", "aadhaarNo must be 12 digits (2-9 leading)");
        }
        if (!"OTP".equalsIgnoreCase(verificationType) && !"BIOMETRIC".equalsIgnoreCase(verificationType)) {
            throw new InvalidPatientRequestException("BEXA003", "verificationType must be OTP or BIOMETRIC");
        }

        // If OTP supplied, treat as verification attempt on latest pending record
        if (otpSupplied != null && !otpSupplied.isBlank()) {
            return verifyAadhaarOtpInternal(patientId, aadhaarNo, otpSupplied);
        }

        // BIOMETRIC path — audit-only, immediate verified (port of biometricDetailsJson)
        if ("BIOMETRIC".equalsIgnoreCase(verificationType)) {
            PatientAadhaarVerification v = new PatientAadhaarVerification();
            v.setId(nextAadhaarVerifId());
            v.setPatientId(owner.getId());
            v.setAadhaarNo(aadhaarNo);
            v.setVerificationType("BIOMETRIC");
            v.setDeviceId(deviceId);
            v.setVerified(true);
            v.setVerifiedAt(Timestamp.from(Instant.now()));
            v.setAttempts(1);
            v.setExpiresAt(Timestamp.from(Instant.now().plusMillis(OTP_TTL_MS)));
            if (owner.getUnitId() != null) v.setUnitId(owner.getUnitId());
            v.setSynchronizedd(false);
            v.setAddedBy(AUDIT_ACTOR_ID);
            v.setAddedDatetime(Timestamp.from(Instant.now()));
            log.info("Aadhaar BIOMETRIC verified patient={} aadhaar={}", patientId, aadhaarNo);
            return aadhaarVerificationRepository.save(v);
        }

        // OTP generation path
        String otp = generateOtp();
        Timestamp expires = Timestamp.from(Instant.now().plusMillis(OTP_TTL_MS));
        PatientAadhaarVerification v = new PatientAadhaarVerification();
        v.setId(nextAadhaarVerifId());
        v.setPatientId(owner.getId());
        v.setAadhaarNo(aadhaarNo);
        v.setVerificationType("OTP");
        v.setOtp(otp);
        v.setVerified(false);
        v.setAttempts(0);
        v.setExpiresAt(expires);
        v.setDeviceId(deviceId);
        if (owner.getUnitId() != null) v.setUnitId(owner.getUnitId());
        v.setSynchronizedd(false);
        v.setAddedBy(AUDIT_ACTOR_ID);
        v.setAddedDatetime(Timestamp.from(Instant.now()));
        log.info("Aadhaar OTP generated patient={} aadhaar={} expires={}", patientId, aadhaarNo, expires);
        return aadhaarVerificationRepository.save(v);
    }

    private PatientAadhaarVerification verifyAadhaarOtpInternal(Long patientId, String aadhaarNo, String otpSupplied) {
        PatientAadhaarVerification latest = aadhaarVerificationRepository.findTopByPatientIdOrderByIdDesc(patientId)
                .orElseThrow(() -> new InvalidPatientRequestException("BEXA003", "No OTP generated for patient"));
        if (latest.getVerified() != null && latest.getVerified()) {
            throw new InvalidPatientRequestException("BEXA003", "Already verified");
        }
        if (latest.getExpiresAt() != null && latest.getExpiresAt().before(Timestamp.from(Instant.now()))) {
            throw new InvalidPatientRequestException("BEXA003", "OTP expired");
        }
        if (latest.getAttempts() != null && latest.getAttempts() >= OTP_MAX_ATTEMPTS) {
            throw new InvalidPatientRequestException("BEXA003", "Max OTP attempts exceeded");
        }
        latest.setAttempts((latest.getAttempts() == null ? 0 : latest.getAttempts()) + 1);
        if (otpSupplied.equals(latest.getOtp()) && aadhaarNo.equals(latest.getAadhaarNo())) {
            latest.setVerified(true);
            latest.setVerifiedAt(Timestamp.from(Instant.now()));
            // also persist aadhaar to patient record if not already set (port of IntegrationServiceHelper flow)
            repository.findById(patientId).ifPresent(p -> {
                if (p.getAadhaarNo() == null || p.getAadhaarNo().isBlank()) {
                    p.setAadhaarNo(aadhaarNo);
                    p.setUpdatedBy(AUDIT_ACTOR_ID);
                    p.setUpdatedDatetime(Timestamp.from(Instant.now()));
                    repository.save(p);
                }
            });
            log.info("Aadhaar OTP verified patient={}", patientId);
        } else {
            log.info("Aadhaar OTP mismatch patient={} attempt={}", patientId, latest.getAttempts());
            if (latest.getAttempts() >= OTP_MAX_ATTEMPTS) {
                // keep as not verified, caller will get error next time
            }
            // still save attempt count, then throw
            aadhaarVerificationRepository.save(latest);
            throw new InvalidPatientRequestException("BEXA003", "Invalid OTP");
        }
        return aadhaarVerificationRepository.save(latest);
    }

    @Transactional(readOnly = true)
    public List<PatientAadhaarVerification> aadhaarVerifications(Long patientId) {
        repository.findById(patientId).orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        return aadhaarVerificationRepository.findByPatientIdOrderByIdDesc(patientId);
    }

    // ------------------------------------------------------------ M01: COVID tracking
    // Ported from CoronaStats.covidStatus + PatientServiceHelper:4493 (if covidStatus != blank → create/update CoronaStats)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientRegistration updateCovidStatus(Long patientId, JsonNode body) {
        PatientRegistration p = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        String covidStatus = body != null && body.has("covidStatus") ? body.get("covidStatus").asText(null) : null;
        if (covidStatus == null || covidStatus.isBlank()) {
            if (body != null && body.has("covid_status")) covidStatus = body.get("covid_status").asText(null);
        }
        if (covidStatus == null || covidStatus.isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "covidStatus is required (POSITIVE/NEGATIVE/SUSPECTED/RECOVERED)");
        }
        p.setCovidStatus(covidStatus.toUpperCase());
        p.setCovidUpdatedAt(Timestamp.from(Instant.now()));
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        log.info("COVID status updated patient={} status={}", patientId, covidStatus);
        return repository.save(p);
    }

    @Transactional(readOnly = true)
    public List<PatientRegistration> listCovidPatients(String covidStatus) {
        if (covidStatus != null && !covidStatus.isBlank()) {
            return repository.findByCovidStatus(covidStatus.toUpperCase());
        }
        return repository.findAllCovidTracked();
    }

    // ------------------------------------------------------- M01: Social Worker assessment
    // Ported from QuetionnaireServiceImpl isSocialAssessment → module "Social Assesment"

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PatientSocialAssessment createSocialAssessment(Long patientId, JsonNode body) {
        PatientRegistration owner = repository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        if (body == null || (!body.has("assessmentData") && !body.has("assessment_data"))) {
            throw new InvalidPatientRequestException("BEXA003", "assessmentData is required");
        }
        JsonNode dataNode = body.has("assessmentData") ? body.get("assessmentData") : body.get("assessment_data");
        Map<String, Object> data;
        try {
            data = objectMapper.convertValue(dataNode, Map.class);
        } catch (Exception e) {
            throw new InvalidPatientRequestException("BEXA003", "Invalid assessmentData");
        }
        Long socialWorkerId = null;
        if (body.has("socialWorkerId")) socialWorkerId = body.get("socialWorkerId").asLong();
        else if (body.has("social_worker_id")) socialWorkerId = body.get("social_worker_id").asLong();
        String remarks = body.has("remarks") ? body.get("remarks").asText(null) : null;

        PatientSocialAssessment a = new PatientSocialAssessment();
        a.setId(nextSocialAssessId());
        a.setPatientId(owner.getId());
        a.setAssessmentData(data);
        a.setSocialWorkerId(socialWorkerId);
        a.setRemarks(remarks);
        if (owner.getUnitId() != null) a.setUnitId(owner.getUnitId());
        a.setSynchronizedd(false);
        a.setAddedBy(AUDIT_ACTOR_ID);
        a.setAddedDatetime(Timestamp.from(Instant.now()));
        log.info("Social assessment created patient={} worker={}", patientId, socialWorkerId);
        return socialAssessmentRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<PatientSocialAssessment> socialAssessments(Long patientId) {
        repository.findById(patientId).orElseThrow(() -> new PatientNotFoundException("NOT_FOUND", "Patient not found"));
        return socialAssessmentRepository.findByPatientIdOrderByIdDesc(patientId);
    }

    private void requireCore(PatientRegistration p) {
        if (p.getFirstName() == null || p.getFirstName().isBlank()
                || p.getGender() == null || p.getGender().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "firstName and gender are required");
        }
        if (p.getMrno() != null && !p.getMrno().isBlank()) {
            throw new InvalidPatientRequestException("BEXA003", "mrno is server-owned");
        }
        // BR-PAT-003 (BERP007): familyId requires memberId
        if (p.getFamilyId() != null && !p.getFamilyId().isBlank()
                && (p.getFamilyMemberId() == null || p.getFamilyMemberId().isBlank())) {
            throw new InvalidPatientRequestException("BERP007",
                    "familyId requires familyMemberId");
        }
    }

    /**
     * Parse compound single-string names (legacy behavior).
     * If firstName contains multiple whitespace-separated tokens and lastName is blank,
     * split: first token → firstName, rest → lastName.
     * Blank names default to "Unknown".
     */
    private void parseCompoundName(PatientRegistration p) {
        if (p.getFirstName() != null && !p.getFirstName().isBlank()) {
            String[] tokens = p.getFirstName().trim().split("\\s+");
            if (tokens.length > 1 && (p.getLastName() == null || p.getLastName().isBlank())) {
                p.setFirstName(tokens[0]);
                p.setLastName(String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length)));
            }
        }
        if (p.getFirstName() == null || p.getFirstName().isBlank()) {
            p.setFirstName("Unknown");
        }
        if (p.getLastName() == null || p.getLastName().isBlank()) {
            p.setLastName("Unknown");
        }
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidPatientRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidPatientRequestException("BEXA003", "Invalid input data !");
        }
    }
}

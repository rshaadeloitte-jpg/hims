package com.hims.adt.application;

import com.hims.adt.application.exceptions.ActiveAdmissionExistsException;
import com.hims.adt.application.exceptions.AdtEntityNotFoundException;
import com.hims.adt.application.exceptions.BedUnavailableException;
import com.hims.adt.application.exceptions.AdtRuleException;
import com.hims.adt.application.exceptions.InvalidAdtRequestException;
import com.hims.adt.domain.model.Admission;
import com.hims.adt.domain.model.AdmissionDetail;
import com.hims.adt.domain.model.Bed;
import com.hims.adt.domain.model.BedReservation;
import com.hims.adt.domain.model.BedState;
import com.hims.adt.domain.model.BedStatusMaster;
import com.hims.adt.domain.model.AttendantPass;
import com.hims.adt.domain.model.BedTransferLog;
import com.hims.adt.domain.model.ClearanceChecklistItem;
import com.hims.adt.domain.model.ClearanceDepartment;
import com.hims.adt.domain.model.Ward;
import com.hims.adt.domain.repository.AdmissionDetailRepository;
import com.hims.adt.domain.repository.AdmissionRepository;
import com.hims.adt.domain.repository.AttendantPassRepository;
import com.hims.adt.domain.repository.BedRepository;
import com.hims.adt.domain.repository.BedReservationRepository;
import com.hims.adt.domain.repository.BedStatusMasterRepository;
import com.hims.adt.domain.repository.BedTransferLogRepository;
import com.hims.adt.domain.repository.ClearanceChecklistRepository;
import com.hims.adt.domain.repository.WardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADT core (MIG-032 / S03, Catalog #3): admission, transfer, discharge,
 * clearance gate, reservations and the abscond sweep.
 *
 * Business rules:
 *  - BR-IPD-001: admitting doctor required (BERA022); no other ACTIVE admission
 *    -> BERB013; attending doctor defaults to admitting doctor; MLC admissions
 *    carry a mandatory MLC number.
 *  - BR-IPD-001 IPD number: A/&lt;prefix&gt;/&lt;yy&gt;/&lt;%05d seq&gt;, seq drawn from
 *    t_admission_seq nextval INSIDE the tx (atomic + sequential; %05d pads to 5
 *    digits and overflows naturally past 99999).
 *  - BR-IPD-002 auto-allocation: free bed = occupied=false AND status=true AND
 *    isnoncensus/isot/ispreop/ispostop=false (+ master isrelease=true), excluding
 *    beds actively reserved for OTHER patients. Claim via atomic CAS
 *    UPDATE m_bed SET occupied=true WHERE bed_id=? AND occupied=false (BEXB001 on loss).
 *  - BR-IPD-003: chosen bed's status master isrelease must be true else BERB005.
 *  - Transfer: CAS-claim target, vacated bed -> CLEANING (status 2), movement row
 *    in t_bed_transfer_log, BedTransferred + per-bed BedStatusChanged events.
 *  - Discharge clearance (BR-DIS-001): NURSING/PHARMACY/BILLING/LAB must ALL be
 *    CLEARED before discharge; abscond discharges bypass the gate.
 *  - BR-DIS-002: isExpired=true records the death block (death_date).
 *  - BR-DIS-003: discharge releases the bed occupied=false + bed_status_id=6
 *    (hard-pinned literal, RISK R5 contract pinned in BedState).
 */
@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final String DISCHARGE_ABSCOND = "ABSCOND";
    public static final String DISCHARGE_AUTO_ABSCONDED = "AUTO_ABSCONDED";

    private final AdmissionRepository admissionRepository;
    private final AdmissionDetailRepository admissionDetailRepository;
    private final BedRepository bedRepository;
    private final BedReservationRepository reservationRepository;
    private final BedTransferLogRepository transferLogRepository;
    private final ClearanceChecklistRepository clearanceRepository;
    private final BedStatusMasterRepository bedStatusMasterRepository;
    private final WardRepository wardRepository;
    private final AttendantPassRepository attendantPassRepository;
    private final com.hims.adt.infrastructure.outbox.OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.adt.ipd-prefix:IHMS}")
    private String ipdPrefix;

    public AdmissionService(AdmissionRepository admissionRepository,
                            AdmissionDetailRepository admissionDetailRepository,
                            BedRepository bedRepository,
                            BedReservationRepository reservationRepository,
                            BedTransferLogRepository transferLogRepository,
                            ClearanceChecklistRepository clearanceRepository,
                            BedStatusMasterRepository bedStatusMasterRepository,
                            WardRepository wardRepository,
                            AttendantPassRepository attendantPassRepository,
                            com.hims.adt.infrastructure.outbox.OutboxService outboxService) {
        this.admissionRepository = admissionRepository;
        this.admissionDetailRepository = admissionDetailRepository;
        this.bedRepository = bedRepository;
        this.reservationRepository = reservationRepository;
        this.transferLogRepository = transferLogRepository;
        this.clearanceRepository = clearanceRepository;
        this.bedStatusMasterRepository = bedStatusMasterRepository;
        this.wardRepository = wardRepository;
        this.attendantPassRepository = attendantPassRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ admission

    @Transactional
    public Admission admit(Long patientId, Long encounterId, Long doctorId,
                           String admissionType, Long unitId, Long bedId,
                           Boolean isMlc, String mlcNumber, String mlcRemarks,
                           Long attendingDoctorId, String remarks) {
        if (patientId == null) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID", "patientId is required");
        }
        if (doctorId == null) {
            throw new InvalidAdtRequestException("BERA022", "Admitting doctor is required");
        }
        boolean mlc = Boolean.TRUE.equals(isMlc);
        if (mlc && (mlcNumber == null || mlcNumber.isBlank())) {
            throw new InvalidAdtRequestException("ADT_MLC_NUMBER_REQUIRED",
                    "MLC admission requires an MLC number");
        }
        if (admissionRepository.countActiveByPatientId(patientId) > 0) {
            throw new ActiveAdmissionExistsException("BERB013",
                    "Patient " + patientId + " already has an active admission");
        }
        Timestamp now = Timestamp.from(Instant.now());

        Bed claimed;
        if (bedId != null) {
            Bed target = getBed(bedId);
            requireReleasable(target);                       // BR-IPD-003 gate -> BERB005
            claimOrThrow(bedId);                             // CAS claim -> BEXB001 on loss
            claimed = getBed(bedId);
            reservationRepository.consume(bedId, patientId, now);   // consume own reservation if any
        } else {
            claimed = autoAllocate(unitId, patientId, now);  // BR-IPD-002 first-fit w/ unit preference
        }

        long admissionId = nextVal("t_admission_seq");
        Admission a = new Admission();
        a.setAdmissionId(admissionId);
        a.setPatientId(patientId);
        a.setEncounterId(encounterId);
        a.setBedId(claimed.getBedId());
        a.setWardId(claimed.getWardId());
        a.setAdmissionType(admissionType);
        a.setIpdNo(buildIpdNo(admissionId));
        a.setDoctorId(doctorId);
        a.setAdmittedAt(now);
        a.setIsCancel(false);
        a.setUnitId(unitId != null ? unitId : wardUnitOf(claimed.getWardId()));
        auditNew(a, now);
        admissionRepository.save(a);

        // 1:1 detail — attending doctor defaults to admitting doctor (BR-IPD-001)
        AdmissionDetail d = new AdmissionDetail();
        d.setDetailId(nextVal("t_admission_detail_seq"));
        d.setAdmissionId(admissionId);
        d.setAttendingDoctorId(attendingDoctorId != null ? attendingDoctorId : doctorId);
        d.setIsMlc(mlc);
        d.setMlcNumber(mlc ? mlcNumber : null);
        d.setMlcRemarks(mlcRemarks);
        d.setRemarks(remarks);
        d.setAddedBy(AUDIT_ACTOR_ID);
        d.setAddedDatetime(now);
        admissionDetailRepository.save(d);

        seedClearanceChecklist(admissionId, now);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("admissionId", String.valueOf(a.getAdmissionId()));
        payload.put("patientId", String.valueOf(a.getPatientId()));
        payload.put("encounterId", a.getEncounterId() == null ? null : String.valueOf(a.getEncounterId()));
        payload.put("bedId", String.valueOf(a.getBedId()));
        payload.put("wardId", String.valueOf(a.getWardId()));
        payload.put("unitId", a.getUnitId() == null ? null : String.valueOf(a.getUnitId()));
        payload.put("admissionType", a.getAdmissionType());
        payload.put("ipdNo", a.getIpdNo());
        payload.put("doctorId", String.valueOf(a.getDoctorId()));
        payload.put("admittedAt", a.getAdmittedAt().toInstant().toString());
        appendEvent(OutboxEvents.PATIENT_ADMITTED, "admission", admissionId,
                String.valueOf(patientId), payload, "patient-admitted:" + admissionId);
        log.info("Admitted patient={} admission={} bed={} ipdNo={}",
                patientId, admissionId, a.getBedId(), a.getIpdNo());
        return a;
    }

    /** BR-IPD-001 template: A/&lt;prefix&gt;/&lt;yy&gt;/&lt;%05d seq&gt;; atomic via t_admission_seq. */
    String buildIpdNo(long seq) {
        int yy = Year.now().getValue() - 2000;
        return String.format("A/%s/%d/%05d", ipdPrefix, yy, seq);
    }

    // ------------------------------------------------------------------ transfer

    @Transactional
    public Admission transfer(long admissionId, Long toBedId, String reason) {
        if (toBedId == null) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID", "toBedId is required");
        }
        Admission a = load(admissionId);
        requireActive(a, "transfer");

        Bed target = getBed(toBedId);
        short targetOldStatus = target.getBedStatusId();
        requireReleasable(target);                           // BERB005 gate
        claimOrThrow(toBedId);                               // CAS -> BEXB001

        Long oldBedId = a.getBedId();
        Long oldWardId = a.getWardId();
        Timestamp now = Timestamp.from(Instant.now());

        // Task contract: vacated bed goes to CLEANING until housekeeping clears it.
        if (oldBedId != null) {
            bedRepository.releaseBed(oldBedId, BedState.transferReleaseTarget());
            emitBedStatusChanged(oldBedId, admissionId, BedState.OCCUPIED.code(), BedState.CLEANING.code(), now);
        }

        a.setBedId(target.getBedId());
        a.setWardId(target.getWardId());
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now);
        admissionRepository.save(a);

        BedTransferLog entry = new BedTransferLog();
        entry.setTransferId(nextVal("t_bed_transfer_log_seq"));
        entry.setAdmissionId(admissionId);
        entry.setPatientId(a.getPatientId());
        entry.setFromBedId(oldBedId);
        entry.setToBedId(target.getBedId());
        entry.setFromWardId(oldWardId);
        entry.setToWardId(target.getWardId());
        entry.setReason(reason);
        entry.setTransferredBy(AUDIT_ACTOR_ID);
        entry.setTransferredAt(now);
        transferLogRepository.save(entry);

        emitBedStatusChanged(target.getBedId(), admissionId, targetOldStatus, BedState.OCCUPIED.code(), now);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("admissionId", String.valueOf(admissionId));
        payload.put("patientId", String.valueOf(a.getPatientId()));
        payload.put("fromBedId", oldBedId == null ? null : String.valueOf(oldBedId));
        payload.put("toBedId", String.valueOf(target.getBedId()));
        payload.put("fromWardId", oldWardId == null ? null : String.valueOf(oldWardId));
        payload.put("toWardId", String.valueOf(target.getWardId()));
        payload.put("reason", reason);
        payload.put("transferredAt", now.toInstant().toString());
        appendEvent(OutboxEvents.BED_TRANSFERRED, "admission", admissionId,
                String.valueOf(a.getPatientId()), payload, "bed-transferred:" + admissionId + ":" + target.getBedId());
        log.info("Transferred admission {} from bed {} to bed {} (old bed CLEANING)", admissionId, oldBedId, toBedId);
        return a;
    }

    /** Housekeeping: CLEANING -> FREE so the bed re-enters allocation. */
    @Transactional
    public Bed markCleaningComplete(long bedId) {
        Bed bed = getBed(bedId);
        if (!BedState.canFinishCleaning(bed.getBedStatusId())) {
            throw new InvalidAdtRequestException("ADT_BED_NOT_CLEANING",
                    "Bed " + bedId + " is not in CLEANING state (status=" + bed.getBedStatusId() + ")");
        }
        Timestamp now = Timestamp.from(Instant.now());
        bedRepository.releaseBed(bedId, BedState.FREE.code());
        emitBedStatusChanged(bedId, null, BedState.CLEANING.code(), BedState.FREE.code(), now);
        log.info("Bed {} cleaning complete -> FREE", bedId);
        return getBed(bedId);
    }

    // ------------------------------------------------------------------ discharge

    @Transactional
    public Admission discharge(long admissionId, String dischargeType, Boolean isExpired,
                               Timestamp deathDate, Timestamp handoverDate) {
        return discharge(admissionId, dischargeType, isExpired, deathDate, handoverDate, null, null);
    }

    /** Death handling (M03): discharge with deathCause/deathType when isExpired=true. */
    @Transactional
    public Admission discharge(long admissionId, String dischargeType, Boolean isExpired,
                               Timestamp deathDate, Timestamp handoverDate,
                               String deathCause, String deathType) {
        Admission a = load(admissionId);
        if (!a.isActive()) {
            throw new InvalidAdtRequestException("ADT_ALREADY_DISCHARGED",
                    "Admission " + admissionId + " is not active (already discharged or cancelled)");
        }
        boolean abscondPath = DISCHARGE_ABSCOND.equalsIgnoreCase(dischargeType)
                || DISCHARGE_AUTO_ABSCONDED.equals(dischargeType);

        // BR-DIS-001 clearance gate: all four departments must be CLEARED.
        if (!abscondPath) {
            long pending = clearanceRepository.countByAdmissionIdAndClrStatus(
                    admissionId, ClearanceChecklistItem.ClrStatus.PENDING);
            if (pending > 0) {
                throw new InvalidAdtRequestException("ADT_CLEARANCE_PENDING",
                        pending + " clearance(s) still PENDING (NURSING/PHARMACY/BILLING/LAB all required)");
            }
        }

        boolean death = Boolean.TRUE.equals(isExpired);
        Timestamp now = Timestamp.from(Instant.now());
        a.setDischargeId(nextVal("t_patientdischargedata_seq"));
        a.setDischargeType(dischargeType);
        a.setIsExpired(death);
        a.setDeathDate(death ? (deathDate != null ? deathDate : now) : null);
        a.setDeathCause(death ? deathCause : null);
        a.setDeathType(death ? deathType : null);
        a.setHandoverDate(handoverDate);
        a.setDischargeDate(now);
        if (abscondPath) {
            a.setIsCancel(true);   // legacy cancels the stay on abscond (BR-DIS-001 deviation)
        }
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now);
        admissionRepository.save(a);

        if (a.getBedId() != null) {
            Bed released = getBed(a.getBedId());
            short priorStatus = released.getBedStatusId();
            short released6 = BedState.dischargeReleaseTarget();          // BR-DIS-003 pinned literal 6
            bedRepository.releaseBed(released.getBedId(), released6);
            emitBedStatusChanged(released.getBedId(), admissionId, priorStatus, released6, now);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dischargeId", String.valueOf(a.getDischargeId()));
        payload.put("patientId", String.valueOf(a.getPatientId()));
        payload.put("admissionId", String.valueOf(a.getAdmissionId()));
        payload.put("encounterId", a.getEncounterId() == null ? null : String.valueOf(a.getEncounterId()));
        payload.put("dischargeType", a.getDischargeType());
        payload.put("isExpired", a.getIsExpired());
        payload.put("deathDate", a.getDeathDate() == null ? null : a.getDeathDate().toInstant().toString());
        payload.put("date", a.getDischargeDate().toInstant().toString());
        payload.put("balanceDue", null);   // phase 1: billing settlement lands later
        appendEvent(OutboxEvents.PATIENT_DISCHARGED, "discharge", a.getDischargeId(),
                String.valueOf(a.getPatientId()), payload, "patient-discharged:" + a.getDischargeId());
        log.info("Discharged admission {} (type={}, expired={})", admissionId, dischargeType, death);
        return a;
    }

    /** Manual abscond (BR-DIS-001 deviation path): cancels the stay, stamps abscond time. */
    @Transactional
    public Admission abscond(long admissionId, String reason) {
        Admission a = load(admissionId);
        if (!a.isActive()) {
            throw new InvalidAdtRequestException("ADT_ALREADY_DISCHARGED",
                    "Admission " + admissionId + " is not active");
        }
        Timestamp now = Timestamp.from(Instant.now());
        a.setAbscondDatetime(now);
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now);
        admissionRepository.save(a);
        return discharge(admissionId, DISCHARGE_ABSCOND, false, null, null);
    }

    /**
     * BR-IPD-005 auto-abscond sweep: interim (unbedded) admissions older than the
     * cutoff are cancelled as AUTO_ABSCONDED. Returns the swept admissions.
     */
    @Transactional
    public List<Admission> sweepAbsconds(Timestamp cutoff) {
        List<Admission> stale = admissionRepository.findInterimUnbeddedBefore(cutoff);
        for (Admission a : stale) {
            Timestamp now = Timestamp.from(Instant.now());
            a.setAbscondDatetime(now);
            a.setUpdatedBy(AUDIT_ACTOR_ID);
            a.setUpdatedDatetime(now);
            admissionRepository.save(a);
            discharge(a.getAdmissionId(), DISCHARGE_AUTO_ABSCONDED, false, null, null);
            log.info("Auto-absconded unbedded admission {}", a.getAdmissionId());
        }
        return stale;
    }

    // ------------------------------------------------------------------ M03: interim, attendant pass, census, TID, death

    /** Interim admission (no bed): legacy CIMS_ADDADMISSIONINFORMATION interim path, bed_id NULL. */
    @Transactional
    public Admission admitInterim(Long patientId, Long encounterId, Long doctorId,
                                   String admissionType, Long unitId,
                                   Boolean isMlc, String mlcNumber, String mlcRemarks,
                                   Long attendingDoctorId, String remarks, String tid) {
        if (patientId == null) throw new InvalidAdtRequestException("ADT_REQ_INVALID", "patientId is required");
        if (doctorId == null) throw new InvalidAdtRequestException("BERA022", "Admitting doctor is required");
        if (admissionRepository.countActiveByPatientId(patientId) > 0) {
            throw new ActiveAdmissionExistsException("BERB013", "Patient " + patientId + " already has an active admission");
        }
        boolean mlc = Boolean.TRUE.equals(isMlc);
        if (mlc && (mlcNumber == null || mlcNumber.isBlank())) {
            throw new InvalidAdtRequestException("ADT_MLC_NUMBER_REQUIRED", "MLC admission requires an MLC number");
        }
        Timestamp now = Timestamp.from(Instant.now());
        long admissionId = nextVal("t_admission_seq");
        Admission a = new Admission();
        a.setAdmissionId(admissionId);
        a.setPatientId(patientId);
        a.setEncounterId(encounterId);
        a.setBedId(null);
        a.setWardId(null);
        a.setAdmissionType(admissionType != null ? admissionType : "INTERIM");
        a.setIsInterim(true);
        a.setTid(tid);
        a.setIpdNo(buildIpdNo(admissionId));
        a.setDoctorId(doctorId);
        a.setAdmittedAt(now);
        a.setIsCancel(false);
        a.setUnitId(unitId);
        auditNew(a, now);
        admissionRepository.save(a);

        AdmissionDetail d = new AdmissionDetail();
        d.setDetailId(nextVal("t_admission_detail_seq"));
        d.setAdmissionId(admissionId);
        d.setAttendingDoctorId(attendingDoctorId != null ? attendingDoctorId : doctorId);
        d.setIsMlc(mlc);
        d.setMlcNumber(mlc ? mlcNumber : null);
        d.setMlcRemarks(mlcRemarks);
        d.setRemarks(remarks);
        d.setAddedBy(AUDIT_ACTOR_ID);
        d.setAddedDatetime(now);
        admissionDetailRepository.save(d);

        seedClearanceChecklist(admissionId, now);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("admissionId", String.valueOf(a.getAdmissionId()));
        payload.put("patientId", String.valueOf(a.getPatientId()));
        payload.put("admissionType", a.getAdmissionType());
        payload.put("isInterim", true);
        payload.put("ipdNo", a.getIpdNo());
        payload.put("tid", tid);
        payload.put("admittedAt", a.getAdmittedAt().toInstant().toString());
        appendEvent(OutboxEvents.PATIENT_ADMITTED, "admission", admissionId,
                String.valueOf(patientId), payload, "patient-admitted-interim:" + admissionId);
        log.info("Interim admitted patient={} admission={} ipdNo={}", patientId, admissionId, a.getIpdNo());
        return a;
    }

    /** Issue attendant pass for an active admission (T_ATTENDANTPASSDETAILS parity). */
    @Transactional
    public AttendantPass issueAttendantPass(long admissionId, String attendeeName, Long passPrice) {
        Admission a = load(admissionId);
        requireActive(a, "issue attendant pass");
        if (attendeeName == null || attendeeName.isBlank()) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID", "attendeeName is required");
        }
        long passId = nextVal("t_attendant_pass_seq");
        long seq = nextVal("t_attendant_pass_no_seq");
        int yy = Year.now().getValue() - 2000;
        String passNo = String.format("ATT/%02d/%05d", yy, seq % 100000L);

        AttendantPass p = new AttendantPass();
        p.setPassId(passId);
        p.setAdmissionId(admissionId);
        p.setPatientId(a.getPatientId());
        p.setAttendeeName(attendeeName.trim());
        p.setPassNo(passNo);
        p.setPassStatus("ISSUED");
        p.setPassPrice(passPrice);
        p.setPrintCount(1L);
        p.setStatus(true);
        p.setAddedBy(AUDIT_ACTOR_ID);
        p.setAddedDatetime(Timestamp.from(Instant.now()));
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        attendantPassRepository.save(p);
        log.info("Issued attendant pass {} for admission {} attendee={}", passNo, admissionId, attendeeName);
        return p;
    }

    @Transactional(readOnly = true)
    public List<AttendantPass> attendantPasses(long admissionId) {
        load(admissionId);
        return attendantPassRepository.findByAdmissionIdOrderByPassIdAsc(admissionId);
    }

    /** Return an issued pass: ISSUED -> RETURNED (idempotent guard). */
    @Transactional
    public AttendantPass returnAttendantPass(long passId) {
        AttendantPass p = attendantPassRepository.findById(passId)
                .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND", "Attendant pass not found: " + passId));
        if (!p.isIssued()) {
            throw new InvalidAdtRequestException("ADT_ILLEGAL_TRANSITION", "Pass " + passId + " is already " + p.getPassStatus());
        }
        p.setPassStatus("RETURNED");
        p.setReturnDate(Timestamp.from(Instant.now()));
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return attendantPassRepository.save(p);
    }

    /** Reprint attendant pass: bump printCount (legacy printCount parity). */
    @Transactional
    public AttendantPass reprintAttendantPass(long passId) {
        AttendantPass p = attendantPassRepository.findById(passId)
                .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND", "Attendant pass not found: " + passId));
        p.setPrintCount(p.getPrintCount() + 1);
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return attendantPassRepository.save(p);
    }

    /** Nursing station census: per-ward bed counts + active admissions (iscensus aware). */
    @Transactional(readOnly = true)
    public Map<String, Object> census(Long wardId) {
        List<Ward> wards = wardId == null ? wardRepository.findAll()
                : List.of(wardRepository.findById(wardId)
                    .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND", "Ward not found: " + wardId)));
        List<Map<String, Object>> rows = new ArrayList<>();
        long totalAdmissions = 0;
        for (Ward w : wards) {
            List<Bed> beds = bedRepository.findByWardIdAndStatusTrue(w.getWardId());
            long totalBeds = beds.size();
            long occupied = beds.stream().filter(b -> Boolean.TRUE.equals(b.getOccupied())).count();
            long free = beds.stream().filter(b -> !Boolean.TRUE.equals(b.getOccupied()) && Boolean.TRUE.equals(b.getStatus())
                    && !Boolean.TRUE.equals(b.getIsnoncensus()) && !Boolean.TRUE.equals(b.getIsot())).count();
            long cleaning = beds.stream().filter(b -> b.getBedStatusId() == 2).count();
            long activeAdmissions = admissionRepository.countByWardIdAndDischargeDateIsNullAndIsCancelFalse(w.getWardId());
            totalAdmissions += activeAdmissions;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("wardId", w.getWardId());
            row.put("wardName", w.getName());
            row.put("isCensus", w.getIscensus());
            row.put("totalBeds", totalBeds);
            row.put("occupied", occupied);
            row.put("free", free);
            row.put("cleaning", cleaning);
            row.put("activeAdmissions", activeAdmissions);
            rows.add(row);
        }
        // interim (unbedded) count
        long interimCount = admissionRepository.findInterimUnbeddedBefore(Timestamp.from(Instant.now().plusSeconds(86400L * 365)))
                .size();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wards", rows);
        result.put("totalActiveAdmissions", totalAdmissions);
        result.put("interimCount", interimCount);
        result.put("generatedAt", Instant.now().toString());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Admission> tidList() {
        return admissionRepository.findWithTid();
    }

    @Transactional(readOnly = true)
    public List<Admission> deathList() {
        return admissionRepository.findDeaths();
    }

    /** Update TID for an admission (RGHS TID linkage). */
    @Transactional
    public Admission setTid(long admissionId, String tid) {
        Admission a = load(admissionId);
        if (tid == null || tid.isBlank()) throw new InvalidAdtRequestException("ADT_REQ_INVALID", "tid is required");
        a.setTid(tid.trim());
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return admissionRepository.save(a);
    }

    // ------------------------------------------------------------------ clearance

    @Transactional
    public List<ClearanceChecklistItem> clearances(long admissionId) {
        load(admissionId);
        return clearanceRepository.findByAdmissionIdOrderByClearanceId(admissionId);
    }

    @Transactional
    public ClearanceChecklistItem clearDepartment(long admissionId, String department,
                                                  Long clearedBy, String remarks) {
        load(admissionId);
        if (!ClearanceDepartment.isKnown(department)) {
            throw new InvalidAdtRequestException("ADT_UNKNOWN_DEPARTMENT",
                    "Unknown clearance department: " + department
                            + " (expected one of NURSING/PHARMACY/BILLING/LAB)");
        }
        ClearanceChecklistItem item = clearanceRepository
                .findByAdmissionIdAndDepartment(admissionId,
                        ClearanceDepartment.valueOf(department))
                .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND",
                        "No checklist row for " + department + " on admission " + admissionId));
        if (item.pending()) {   // idempotent: already-CLEARED rows keep their first stamp
            Timestamp now = Timestamp.from(Instant.now());
            item.setClrStatus(ClearanceChecklistItem.ClrStatus.CLEARED);
            item.setClearedAt(now);
            item.setClearedBy(clearedBy != null ? clearedBy : AUDIT_ACTOR_ID);
            item.setRemarks(remarks);
        } else if (remarks != null) {
            item.setRemarks(remarks);
        }
        return clearanceRepository.save(item);
    }

    // ------------------------------------------------------------------ reservations

    @Transactional
    public BedReservation reserve(Long bedId, Long patientId, Timestamp expiresAt) {
        if (bedId == null || patientId == null || expiresAt == null) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID",
                    "bedId, patientId and expiresAt are required");
        }
        getBed(bedId);
        Timestamp now = Timestamp.from(Instant.now());
        if (!expiresAt.after(now)) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID", "expiresAt must be in the future");
        }
        if (reservationRepository.findFirstByBedIdAndResStatus(bedId, BedReservation.ResStatus.ACTIVE).isPresent()) {
            throw new InvalidAdtRequestException("ADT_BED_RESERVED",
                    "Bed " + bedId + " already has an ACTIVE reservation");
        }
        BedReservation r = new BedReservation();
        r.setReservationId(nextVal("t_bed_reservation_seq"));
        r.setBedId(bedId);
        r.setPatientId(patientId);
        r.setReservedBy(AUDIT_ACTOR_ID);
        r.setReservedAt(now);
        r.setExpiresAt(expiresAt);
        r.setResStatus(BedReservation.ResStatus.ACTIVE);
        return reservationRepository.save(r);
    }

    @Transactional
    public void cancelReservation(long reservationId) {
        BedReservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND",
                        "Reservation not found: " + reservationId));
        if (r.getResStatus() == BedReservation.ResStatus.ACTIVE) {
            r.setResStatus(BedReservation.ResStatus.CANCELLED);
            r.setReleasedAt(Timestamp.from(Instant.now()));
        }
    }

    @Transactional
    public int expireStaleReservations() {
        return reservationRepository.expireStale(Timestamp.from(Instant.now()));
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Admission get(long id) { return load(id); }

    @Transactional(readOnly = true)
    public List<Admission> search(Long patientId, Boolean active) {
        return admissionRepository.search(patientId, Boolean.TRUE.equals(active));
    }

    @Transactional(readOnly = true)
    public List<BedTransferLog> transferHistory(long admissionId) {
        load(admissionId);
        return transferLogRepository.findByAdmissionIdOrderByTransferredAtAsc(admissionId);
    }

    // ------------------------------------------------------------------ internals

    private Admission load(long id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> new AdtEntityNotFoundException("NOT_FOUND", "Admission not found: " + id));
    }

    private void requireActive(Admission a, String action) {
        if (!a.isActive()) {
            throw new InvalidAdtRequestException("ADT_ILLEGAL_TRANSITION",
                    action + " allowed only on an active admission (id=" + a.getAdmissionId() + ")");
        }
    }

    /** BR-IPD-003 gate: bed's master status must have isrelease=true, else BERB005. */
    private void requireReleasable(Bed bed) {
        BedStatusMaster master = bedStatusMasterRepository.findById(bed.getBedStatusId())
                .orElseThrow(() -> new InvalidAdtRequestException("BERB005",
                        "Bed status " + bed.getBedStatusId() + " has no master row"));
        if (!Boolean.TRUE.equals(master.getIsrelease())) {
            throw new InvalidAdtRequestException("BERB005",
                    "Bed " + bed.getBedId() + " status " + bed.getBedStatusId()
                            + " does not allow allocation (isrelease=false)");
        }
    }

    /** Atomic CAS claim; 0 rows updated means the bed was taken concurrently -> BEXB001 (409). */
    private void claimOrThrow(Long bedId) {
        if (bedRepository.claimBed(bedId) == 0) {
            throw new BedUnavailableException("BEXB001",
                    "Bed " + bedId + " could not be claimed (no longer free)");
        }
    }

    /**
     * BR-IPD-002 auto-allocation: free beds preferring wards of the admitting unit,
     * then lowest bed_id. Beds actively reserved for OTHER patients are invisible.
     */
    private Bed autoAllocate(Long unitId, Long patientId, Timestamp now) {
        List<Bed> candidates = new ArrayList<>(bedRepository.findFreeBeds());
        candidates.removeIf(b -> reservedForSomeoneElse(b.getBedId(), patientId, now));
        if (candidates.isEmpty()) {
            throw new BedUnavailableException("BEXB001", "No free bed available");
        }
        Set<Long> preferredWards = new HashSet<>();
        if (unitId != null) {
            for (Ward w : wardRepository.findByUnitIdAndStatusTrue(unitId)) {
                preferredWards.add(w.getWardId());
            }
        }
        Comparator<Bed> preference = Comparator
                .comparing((Bed b) -> !preferredWards.contains(b.getWardId()))
                .thenComparing(Bed::getBedId);
        candidates.sort(preference);

        for (Bed candidate : candidates) {
            if (bedRepository.claimBed(candidate.getBedId()) > 0) {
                reservationRepository.consume(candidate.getBedId(), patientId, now);
                return getBed(candidate.getBedId());
            }
            log.warn("CAS claim lost on candidate bed {} during auto-allocation", candidate.getBedId());
        }
        throw new BedUnavailableException("BEXB001",
                "All candidate beds were claimed concurrently (CAS lost)");
    }

    private boolean reservedForSomeoneElse(Long bedId, Long patientId, Timestamp now) {
        return reservationRepository
                .findFirstByBedIdAndResStatus(bedId, BedReservation.ResStatus.ACTIVE)
                .map(r -> r.isActiveAt(now) && !r.getPatientId().equals(patientId))
                .orElse(false);
    }

    private void seedClearanceChecklist(long admissionId, Timestamp now) {
        for (ClearanceDepartment dept : ClearanceDepartment.values()) {
            ClearanceChecklistItem item = new ClearanceChecklistItem();
            item.setClearanceId(nextVal("r_clearance_checklist_seq"));
            item.setAdmissionId(admissionId);
            item.setDepartment(dept);
            item.setClrStatus(ClearanceChecklistItem.ClrStatus.PENDING);
            clearanceRepository.save(item);
        }
    }

    private Long wardUnitOf(Long wardId) {
        return wardRepository.findById(wardId).map(Ward::getUnitId).orElse(null);
    }

    private Bed getBed(long bedId) {
        return bedRepository.findById(bedId)
                .orElseThrow(() -> new AdtEntityNotFoundException("BED_NOT_FOUND", "Bed not found: " + bedId));
    }

    private void auditNew(Admission a, Timestamp now) {
        a.setStatus(true);
        a.setSynchronizedd(false);
        a.setAddedBy(AUDIT_ACTOR_ID);
        a.setAddedDatetime(now);
    }

    private void emitBedStatusChanged(long bedId, Long admissionId, short oldStatus, short newStatus, Timestamp at) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bedId", String.valueOf(bedId));
        payload.put("admissionId", admissionId == null ? null : String.valueOf(admissionId));
        payload.put("oldStatus", String.valueOf(oldStatus));
        payload.put("newStatus", String.valueOf(newStatus));
        payload.put("changedAt", at.toInstant().toString());
        appendEvent(OutboxEvents.BED_STATUS_CHANGED, "bed", bedId,
                String.valueOf(bedId), payload, "bed-status-changed:" + bedId + ":" + oldStatus + "->" + newStatus + ":" + at.getTime());
    }

    private void appendEvent(String eventType, String aggregateType, Long aggregateId,
                             String partitionKey, Map<String, Object> payload, String idempotencyKey) {
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            outboxService.append(eventType, aggregateType, aggregateId, partitionKey, json, idempotencyKey);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to append outbox event " + eventType, ex);
        }
    }

    private Long nextVal(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_adt." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    /** Exact event TYPE strings on topic ihms.adt.lifecycle. */
    public static final class OutboxEvents {
        public static final String PATIENT_ADMITTED = "PatientAdmitted";       // E-03, key patientId
        public static final String PATIENT_DISCHARGED = "PatientDischarged";   // E-04, key patientId
        public static final String BED_TRANSFERRED = "BedTransferred";         // key patientId
        public static final String BED_STATUS_CHANGED = "BedStatusChanged";    // E-11 semantics, key bedId

        private OutboxEvents() {}
    }
}

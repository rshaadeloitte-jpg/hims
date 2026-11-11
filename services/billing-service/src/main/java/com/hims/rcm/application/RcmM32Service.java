package com.hims.rcm.application;

import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.application.exceptions.RcmRuleException;
import com.hims.rcm.domain.model.ChiranjeeviCase;
import com.hims.rcm.domain.model.DischargeVerification;
import com.hims.rcm.domain.model.DmApproval;
import com.hims.rcm.domain.repository.ChiranjeeviCaseRepository;
import com.hims.rcm.domain.repository.DischargeVerificationRepository;
import com.hims.rcm.domain.repository.DmApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** M32 RCM: DM approval, discharge verification, Chiranjeevi cases. */
@Service
public class RcmM32Service {
    private static final long ACTOR = 1L;
    private final DmApprovalRepository dmRepository;
    private final DischargeVerificationRepository dvRepository;
    private final ChiranjeeviCaseRepository cjRepository;

    public RcmM32Service(DmApprovalRepository dmRepository,
                         DischargeVerificationRepository dvRepository,
                         ChiranjeeviCaseRepository cjRepository) {
        this.dmRepository = dmRepository;
        this.dvRepository = dvRepository;
        this.cjRepository = cjRepository;
    }

    // ============================================================ DM approval

    @Transactional
    public DmApproval requestDmApproval(long patientId, Long admissionId, String scheme,
                                         BigDecimal requestedAmount) {
        if (patientId <= 0) throw new InvalidRcmRequestException("BEXA003", "patientId is required");
        if (scheme == null || scheme.isBlank()) throw new InvalidRcmRequestException("BEXA003", "scheme is required");
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            throw new InvalidRcmRequestException("BEXA003", "requestedAmount must be > 0");
        }
        long id = dmRepository.nextId();
        DmApproval a = new DmApproval();
        a.setId(id); a.setApprovalNo("DM-" + id);
        a.setPatientId(patientId); a.setAdmissionId(admissionId);
        a.setScheme(scheme); a.setRequestedAmount(requestedAmount);
        a.setRequestedAt(Timestamp.from(Instant.now()));
        a.setDocStatus(DmApproval.STATUS_PENDING);
        stamp(a);
        return dmRepository.save(a);
    }

    @Transactional
    public DmApproval decide(long id, boolean approved, BigDecimal approvedAmount,
                              Long dmUserId, String remarks) {
        DmApproval a = getDm(id);
        if (a.getDocStatus() != DmApproval.STATUS_PENDING) {
            throw new RcmRuleException("RCM_STATE", "Only PENDING DM approvals can be decided");
        }
        if (approved && (approvedAmount == null || approvedAmount.signum() <= 0)) {
            throw new InvalidRcmRequestException("BEXA003", "approvedAmount is required for approval");
        }
        a.setDocStatus(approved ? DmApproval.STATUS_APPROVED : DmApproval.STATUS_REJECTED);
        if (approved) a.setApprovedAmount(approvedAmount);
        a.setDmUserId(dmUserId);
        a.setDmRemarks(remarks);
        a.setDecidedAt(Timestamp.from(Instant.now()));
        stamp(a);
        return dmRepository.save(a);
    }

    @Transactional(readOnly = true) public DmApproval getDm(long id) {
        return dmRepository.findById(id).orElseThrow(() -> new RcmNotFoundException("NOT_FOUND", "DM approval not found"));
    }
    @Transactional(readOnly = true) public List<DmApproval> listDm(Short status) {
        if (status != null) return dmRepository.findByDocStatusOrderByRequestedAtDesc(status);
        return dmRepository.findAll();
    }

    // ============================================================ Discharge verification

    @Transactional
    public DischargeVerification startVerification(long admissionId, long patientId) {
        long id = dvRepository.nextId();
        DischargeVerification v = new DischargeVerification();
        v.setId(id); v.setAdmissionId(admissionId); v.setPatientId(patientId);
        v.setBillSettled(false); v.setClearanceDone(false);
        v.setDocStatus(DischargeVerification.STATUS_PENDING);
        stamp(v);
        return dvRepository.save(v);
    }

    @Transactional
    public DischargeVerification setBillSettled(long id, boolean settled) {
        DischargeVerification v = getDv(id);
        v.setBillSettled(settled);
        stamp(v);
        return dvRepository.save(v);
    }

    @Transactional
    public DischargeVerification setClearanceDone(long id, boolean done) {
        DischargeVerification v = getDv(id);
        v.setClearanceDone(done);
        stamp(v);
        return dvRepository.save(v);
    }

    @Transactional
    public DischargeVerification complete(long id, boolean verified, Long verifierUserId, String remarks) {
        DischargeVerification v = getDv(id);
        if (v.getDocStatus() != DischargeVerification.STATUS_PENDING) {
            throw new RcmRuleException("RCM_STATE", "Verification is not PENDING");
        }
        if (verified) {
            if (!Boolean.TRUE.equals(v.getBillSettled())) {
                throw new RcmRuleException("RCM_BILL_PENDING", "Bill must be settled before verification");
            }
            if (!Boolean.TRUE.equals(v.getClearanceDone())) {
                throw new RcmRuleException("RCM_CLEARANCE_PENDING", "Clearance must be done before verification");
            }
            v.setDocStatus(DischargeVerification.STATUS_VERIFIED);
        } else {
            v.setDocStatus(DischargeVerification.STATUS_REJECTED);
        }
        v.setVerifierUserId(verifierUserId);
        v.setVerifiedAt(Timestamp.from(Instant.now()));
        v.setRemarks(remarks);
        stamp(v);
        return dvRepository.save(v);
    }

    @Transactional(readOnly = true) public DischargeVerification getDv(long id) {
        return dvRepository.findById(id).orElseThrow(() -> new RcmNotFoundException("NOT_FOUND", "Discharge verification not found"));
    }
    @Transactional(readOnly = true) public List<DischargeVerification> listDv(Long admissionId, Short status) {
        if (admissionId != null) return dvRepository.findByAdmissionIdOrderByIdDesc(admissionId);
        if (status != null) return dvRepository.findByDocStatusOrderByIdDesc(status);
        return dvRepository.findAll();
    }

    // ============================================================ Chiranjeevi

    @Transactional
    public ChiranjeeviCase openCase(long patientId, String chiranjeeviId, String packageCode, BigDecimal amount) {
        if (patientId <= 0) throw new InvalidRcmRequestException("BEXA003", "patientId is required");
        if (chiranjeeviId == null || chiranjeeviId.isBlank()) throw new InvalidRcmRequestException("BEXA003", "chiranjeeviId is required");
        if (packageCode == null || packageCode.isBlank()) throw new InvalidRcmRequestException("BEXA003", "packageCode is required");
        if (amount == null || amount.signum() <= 0) throw new InvalidRcmRequestException("BEXA003", "amount must be > 0");
        long id = cjRepository.nextId();
        ChiranjeeviCase c = new ChiranjeeviCase();
        c.setId(id); c.setCaseNo("CJ-" + id);
        c.setPatientId(patientId); c.setChiranjeeviId(chiranjeeviId);
        c.setPackageCode(packageCode); c.setAmount(amount);
        c.setRequestedAt(Timestamp.from(Instant.now()));
        c.setDocStatus(ChiranjeeviCase.STATUS_NEW);
        stamp(c);
        return cjRepository.save(c);
    }

    @Transactional
    public ChiranjeeviCase decideCase(long id, boolean approved, String remarks) {
        ChiranjeeviCase c = getCj(id);
        if (c.getDocStatus() != ChiranjeeviCase.STATUS_NEW) {
            throw new RcmRuleException("RCM_STATE", "Only NEW Chiranjeevi cases can be decided");
        }
        c.setDocStatus(approved ? ChiranjeeviCase.STATUS_APPROVED : ChiranjeeviCase.STATUS_REJECTED);
        c.setDecidedAt(Timestamp.from(Instant.now()));
        c.setRemarks(remarks);
        stamp(c);
        return cjRepository.save(c);
    }

    @Transactional
    public ChiranjeeviCase close(long id, String remarks) {
        ChiranjeeviCase c = getCj(id);
        if (c.getDocStatus() != ChiranjeeviCase.STATUS_APPROVED) {
            throw new RcmRuleException("RCM_STATE", "Only APPROVED cases can be closed");
        }
        c.setDocStatus(ChiranjeeviCase.STATUS_CLOSED);
        c.setClosedAt(Timestamp.from(Instant.now()));
        c.setRemarks(remarks);
        stamp(c);
        return cjRepository.save(c);
    }

    @Transactional(readOnly = true) public ChiranjeeviCase getCj(long id) {
        return cjRepository.findById(id).orElseThrow(() -> new RcmNotFoundException("NOT_FOUND", "Chiranjeevi case not found"));
    }
    @Transactional(readOnly = true) public List<ChiranjeeviCase> listCj(Short status) {
        if (status != null) return cjRepository.findByDocStatusOrderByRequestedAtDesc(status);
        return cjRepository.findAll();
    }

    private void stamp(com.hims.rcm.domain.model.RcmBase entity) {
        entity.setAddedBy(ACTOR);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
        entity.setSynchronizedd(false);
    }
}

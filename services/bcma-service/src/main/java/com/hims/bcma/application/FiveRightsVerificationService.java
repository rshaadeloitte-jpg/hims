package com.hims.bcma.application;

import com.hims.bcma.domain.model.*;
import com.hims.bcma.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class FiveRightsVerificationService {

    private final BcmaVerificationRepository bcmaRepo;
    private final VerificationLogRepository logRepo;
    private jakarta.persistence.EntityManager em;

    public FiveRightsVerificationService(BcmaVerificationRepository bcmaRepo, VerificationLogRepository logRepo) {
        this.bcmaRepo = bcmaRepo;
        this.logRepo = logRepo;
    }

    @Transactional
    public BcmaVerification verifyRightPatient(Long bcmaId, Long patientId, String wristbandBarcode, Boolean match) {
        if (patientId == null) {
            throw new IllegalArgumentException("patientId is required");
        }
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElse(new BcmaVerification());
        v.setBcmaId(bcmaId != null ? bcmaId : Instant.now().toEpochMilli());
        v.setPatientId(patientId);
        v.setWristbandBarcode(wristbandBarcode);
        v.setRightPatient(match != null ? match : true);
        v.setScanTime(Timestamp.from(Instant.now()));
        BcmaVerification saved = bcmaRepo.save(v);

        VerificationLog log = new VerificationLog();
        log.setLogId(Instant.now().toEpochMilli());
        log.setBcmaId(saved.getBcmaId());
        log.setPatientId(patientId);
        log.setVerificationEvent("RIGHT_PATIENT");
        log.setResult(match);
        log.setScanTimestamp(Timestamp.from(Instant.now()));
        logRepo.save(log);
        return saved;
    }

    @Transactional
    public BcmaVerification verifyRightDrug(Long bcmaId, String drugNdc, Boolean match) {
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElse(new BcmaVerification());
        v.setBcmaId(bcmaId != null ? bcmaId : Instant.now().toEpochMilli());
        v.setDrugNdc(drugNdc);
        v.setRightDrug(match != null ? match : true);
        v.setScanTime(Timestamp.from(Instant.now()));
        BcmaVerification saved = bcmaRepo.save(v);

        VerificationLog log = new VerificationLog();
        log.setLogId(Instant.now().toEpochMilli());
        log.setBcmaId(saved.getBcmaId());
        log.setPatientId(saved.getPatientId());
        log.setVerificationEvent("RIGHT_DRUG");
        log.setResult(match);
        log.setScanTimestamp(Timestamp.from(Instant.now()));
        logRepo.save(log);
        return saved;
    }

    @Transactional
    public BcmaVerification verifyRightDose(Long bcmaId, Boolean match) {
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElse(new BcmaVerification());
        v.setBcmaId(bcmaId != null ? bcmaId : Instant.now().toEpochMilli());
        v.setRightDose(match != null ? match : true);
        v.setScanTime(Timestamp.from(Instant.now()));
        BcmaVerification saved = bcmaRepo.save(v);

        VerificationLog log = new VerificationLog();
        log.setLogId(Instant.now().toEpochMilli());
        log.setBcmaId(saved.getBcmaId());
        log.setPatientId(saved.getPatientId());
        log.setVerificationEvent("RIGHT_DOSE");
        log.setResult(match);
        log.setScanTimestamp(Timestamp.from(Instant.now()));
        logRepo.save(log);
        return saved;
    }

    @Transactional
    public BcmaVerification verifyRightRoute(Long bcmaId, Boolean match) {
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElse(new BcmaVerification());
        v.setBcmaId(bcmaId != null ? bcmaId : Instant.now().toEpochMilli());
        v.setRightRoute(match != null ? match : true);
        v.setScanTime(Timestamp.from(Instant.now()));
        BcmaVerification saved = bcmaRepo.save(v);

        VerificationLog log = new VerificationLog();
        log.setLogId(Instant.now().toEpochMilli());
        log.setBcmaId(saved.getBcmaId());
        log.setPatientId(saved.getPatientId());
        log.setVerificationEvent("RIGHT_ROUTE");
        log.setResult(match);
        log.setScanTimestamp(Timestamp.from(Instant.now()));
        logRepo.save(log);
        return saved;
    }

    @Transactional
    public BcmaVerification verifyRightTime(Long bcmaId, Boolean match) {
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElse(new BcmaVerification());
        v.setBcmaId(bcmaId != null ? bcmaId : Instant.now().toEpochMilli());
        v.setRightTime(match != null ? match : true);
        v.setScanTime(Timestamp.from(Instant.now()));
        BcmaVerification saved = bcmaRepo.save(v);

        VerificationLog log = new VerificationLog();
        log.setLogId(Instant.now().toEpochMilli());
        log.setBcmaId(saved.getBcmaId());
        log.setPatientId(saved.getPatientId());
        log.setVerificationEvent("RIGHT_TIME");
        log.setResult(match);
        log.setScanTimestamp(Timestamp.from(Instant.now()));
        logRepo.save(log);
        return saved;
    }

    @Transactional
    public BcmaVerification finalizeVerification(Long bcmaId) {
        BcmaVerification v = bcmaRepo.findByBcmaId(bcmaId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (v.getRightPatient() == null) {
            throw new NullPointerException("rightPatient is null");
        }
        boolean allPass = Boolean.TRUE.equals(v.getRightPatient()) && Boolean.TRUE.equals(v.getRightDrug())
                && Boolean.TRUE.equals(v.getRightDose()) && Boolean.TRUE.equals(v.getRightRoute())
                && Boolean.TRUE.equals(v.getRightTime());
        v.setAllRightsPassed(allPass);
        v.setAdministered(allPass);
        v.setAdministeredAt(Timestamp.from(Instant.now()));
        return bcmaRepo.save(v);
    }

    public List<BcmaVerification> findByPatient(Long patientId) {
        return bcmaRepo.findByPatientId(patientId);
    }
}

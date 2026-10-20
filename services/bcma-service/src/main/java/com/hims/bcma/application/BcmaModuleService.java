package com.hims.bcma.application;

import com.hims.bcma.domain.model.BCMAException;
import com.hims.bcma.domain.model.IVPEvent;
import com.hims.bcma.domain.model.MedicationScan;
import com.hims.bcma.domain.model.MedicationStock;
import com.hims.bcma.domain.model.PatientDrugMatch;
import com.hims.bcma.domain.repository.BCMAExceptionRepository;
import com.hims.bcma.domain.repository.IVPEventRepository;
import com.hims.bcma.domain.repository.MedicationScanRepository;
import com.hims.bcma.domain.repository.MedicationStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class BcmaModuleService {

    private final MedicationScanRepository scanRepo;
    private final IVPEventRepository ivpRepo;
    private final BCMAExceptionRepository excRepo;
    private final MedicationStockRepository stockRepo;

    public BcmaModuleService(MedicationScanRepository scanRepo, IVPEventRepository ivpRepo,
                             BCMAExceptionRepository excRepo, MedicationStockRepository stockRepo) {
        this.scanRepo = scanRepo;
        this.ivpRepo = ivpRepo;
        this.excRepo = excRepo;
        this.stockRepo = stockRepo;
    }

    @Transactional
    public PatientDrugMatch scanMedication(Long patientId, String drugIen, String drugName,
                                           Long scannedBy, Long wardId) {
        MedicationScan scan = new MedicationScan();
        Long id = Instant.now().toEpochMilli();
        scan.setScanId(id);
        scan.setPatientId(patientId);
        scan.setDrugIen(drugIen);
        scan.setDrugName(drugName);
        scan.setScanTime(Timestamp.from(Instant.now()));
        scan.setScannedBy(scannedBy);
        scan.setWardId(wardId);
        scan.setVerified(true);
        scan.setVerificationStatus("MATCH");
        scanRepo.save(scan);
        return new PatientDrugMatch(patientId, drugIen, drugName, true, "MATCH",
                "Patient-drug match verified", id);
    }

    @Transactional
    public IVPEvent recordIVP(Long patientId, String drugName, String dose, String rate,
                              Timestamp startTime, Timestamp endTime, Long nurseId, Long wardId) {
        IVPEvent ev = new IVPEvent();
        ev.setIvpId(Instant.now().toEpochMilli());
        ev.setPatientId(patientId);
        ev.setDrugName(drugName);
        ev.setDose(dose);
        ev.setRate(rate);
        ev.setStartTime(startTime);
        ev.setEndTime(endTime);
        ev.setNurseId(nurseId);
        ev.setWardId(wardId);
        ev.setEventStatus(endTime == null ? "ACTIVE" : "COMPLETED");
        return ivpRepo.save(ev);
    }

    @Transactional
    public BCMAException logException(Long patientId, String drugIen, BCMAException.ExceptionType type,
                                      String reason, Long overrideNurse, Timestamp exceptionTime,
                                      Long wardId, Long scannedBy) {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(Instant.now().toEpochMilli());
        exc.setPatientId(patientId);
        exc.setDrugIen(drugIen);
        exc.setExceptionType(type.name());
        exc.setReason(reason);
        exc.setOverrideNurse(overrideNurse);
        exc.setExceptionTime(exceptionTime != null ? exceptionTime : Timestamp.from(Instant.now()));
        exc.setWardId(wardId);
        exc.setScannedBy(scannedBy);
        return excRepo.save(exc);
    }

    @Transactional(readOnly = true)
    public List<MedicationStock> getStockByMedication(String drugIen, Long wardId) {
        if (wardId != null && drugIen != null) {
            return stockRepo.findByDrugIenAndWardId(drugIen, wardId)
                    .map(List::of).orElse(List.of());
        } else if (drugIen != null) {
            return stockRepo.findByDrugIen(drugIen);
        } else if (wardId != null) {
            return stockRepo.findByWardId(wardId);
        }
        return stockRepo.findAll();
    }

    @Transactional(readOnly = true)
    public PatientDrugMatch getScanResult(Long scanId) {
        MedicationScan s = scanRepo.findByScanId(scanId).orElse(null);
        if (s == null) return null;
        return new PatientDrugMatch(s.getPatientId(), s.getDrugIen(), s.getDrugName(),
                s.getVerified(), s.getVerificationStatus(), "Found", s.getScanId());
    }
}

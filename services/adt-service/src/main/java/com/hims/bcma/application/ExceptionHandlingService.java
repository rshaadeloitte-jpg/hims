package com.hims.bcma.application;

import com.hims.bcma.domain.model.BCMAException;
import com.hims.bcma.domain.repository.BCMAExceptionRepository;
import com.hims.bcma.domain.repository.OverrideReasonRepository;
import com.hims.bcma.domain.model.OverrideReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class ExceptionHandlingService {

    private final BCMAExceptionRepository excRepo;
    private final OverrideReasonRepository reasonRepo;

    public ExceptionHandlingService(BCMAExceptionRepository excRepo, OverrideReasonRepository reasonRepo) {
        this.excRepo = excRepo;
        this.reasonRepo = reasonRepo;
    }

    @Transactional
    public BCMAException createException(Long patientId, String drugIen, String exceptionType, String reason,
                                          Long overrideNurse, Long wardId, Long scannedBy) {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(Instant.now().toEpochMilli());
        exc.setPatientId(patientId);
        exc.setDrugIen(drugIen);
        exc.setExceptionType(exceptionType);
        exc.setReason(reason);
        exc.setOverrideNurse(overrideNurse);
        exc.setExceptionTime(Timestamp.from(Instant.now()));
        exc.setWardId(wardId);
        exc.setScannedBy(scannedBy);
        return excRepo.save(exc);
    }

    @Transactional
    public BCMAException applyOverride(Long exceptionId, String justification, Long overrideNurse) {
        BCMAException exc = excRepo.findByExceptionId(exceptionId).orElseThrow(() -> new IllegalArgumentException("Exception not found"));
        exc.setOverridden(true); exc.setOverrideReason(justification); exc.setReason(justification);
        exc.setOverrideNurse(overrideNurse);
        return excRepo.save(exc);
    }

    @Transactional
    public OverrideReason addReason(String code, String description, String category) {
        OverrideReason r = new OverrideReason();
        r.setReasonId(Instant.now().toEpochMilli());
        r.setCode(code);
        r.setDescription(description);
        r.setCategory(category);
        return reasonRepo.save(r);
    }

    public List<BCMAException> findByPatient(Long patientId) {
        return excRepo.findByPatientId(patientId);
    }
}

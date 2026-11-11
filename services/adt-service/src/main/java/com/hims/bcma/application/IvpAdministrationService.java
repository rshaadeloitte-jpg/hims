package com.hims.bcma.application;

import com.hims.bcma.domain.model.*;
import com.hims.bcma.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class IvpAdministrationService {

    private final IvpAdministrationRepository ivpRepo;
    private final IvpDrugConfigRepository configRepo;

    public IvpAdministrationService(IvpAdministrationRepository ivpRepo, IvpDrugConfigRepository configRepo) {
        this.ivpRepo = ivpRepo;
        this.configRepo = configRepo;
    }

    @Transactional
    public IvpAdministration recordIVP(Long patientId, String drugName, BigDecimal doseAmount, String doseUnit,
                                       BigDecimal rateMlPerMin, Boolean dilutionRequired, BigDecimal dilutionVolumeMl,
                                       Long administeredBy, Long wardId) {
        IvpAdministration ivp = new IvpAdministration();
        ivp.setIvpId(Instant.now().toEpochMilli());
        ivp.setPatientId(patientId);
        ivp.setDrugName(drugName);
        ivp.setDoseAmount(doseAmount);
        ivp.setDoseUnit(doseUnit);
        ivp.setRateMlPerMin(rateMlPerMin);
        ivp.setDilutionRequired(dilutionRequired != null ? dilutionRequired : false);
        ivp.setDilutionVolumeMl(dilutionVolumeMl);
        ivp.setAdministeredBy(administeredBy);
        ivp.setAdministrationTime(Timestamp.from(Instant.now()));
        ivp.setAdministrationStatus("IN_PROGRESS");
        ivp.setWardId(wardId);
        return ivpRepo.save(ivp);
    }

    @Transactional
    public IvpAdministration verifyDilution(Long ivpId, Boolean verified) {
        IvpAdministration ivp = ivpRepo.findByIvpId(ivpId).orElseThrow(() -> new IllegalArgumentException("IVP not found"));
        IvpDrugConfig config = configRepo.findByDrugNdc(ivp.getDrugName() != null ? ivp.getDrugName() : "").orElse(null);
        if (config != null && Boolean.TRUE.equals(config.getRequiresDilution()) && Boolean.FALSE.equals(verified)) {
            throw new IllegalArgumentException("Dilution required and not verified");
        }
        ivp.setDilutionRequired(verified);
        return ivpRepo.save(ivp);
    }

    @Transactional
    public IvpAdministration completeIVP(Long ivpId, Long administeredBy) {
        IvpAdministration ivp = ivpRepo.findByIvpId(ivpId).orElseThrow(() -> new IllegalArgumentException("IVP not found"));
        ivp.setAdministrationStatus("COMPLETED");
        ivp.setAdministeredBy(administeredBy);
        ivp.setAdministrationTime(Timestamp.from(Instant.now()));
        return ivpRepo.save(ivp);
    }

    public List<IvpAdministration> findByPatient(Long patientId) {
        return ivpRepo.findByPatientId(patientId);
    }

    public IvpDrugConfig createConfig(String drugNdc, BigDecimal maxRateMlPerMin, Boolean requiresDilution, BigDecimal standardDilutionVolumeMl) {
        IvpDrugConfig config = new IvpDrugConfig();
        config.setConfigId(Instant.now().toEpochMilli());
        config.setDrugNdc(drugNdc);
        config.setMaxRateMlPerMin(maxRateMlPerMin);
        config.setRequiresDilution(requiresDilution);
        config.setStandardDilutionVolumeMl(standardDilutionVolumeMl);
        config.setIsActive(true);
        return configRepo.save(config);
    }
}

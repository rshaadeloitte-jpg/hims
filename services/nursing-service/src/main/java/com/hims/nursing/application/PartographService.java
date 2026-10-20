package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.Partograph;
import com.hims.nursing.domain.repository.PartographRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class PartographService {

    private static final Logger log = LoggerFactory.getLogger(PartographService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final PartographRepository repo;

    @PersistenceContext
    private EntityManager em;

    public PartographService(PartographRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Partograph createPartograph(Long patientId, Long admissionId, Long opdIpdId, Long opdIpd,
                                       String parity, String startOfLabourDate,
                                       String membraneStatus, Timestamp membraneRuptureDate,
                                       String membraneVitalAbb, BigDecimal cervixDilation,
                                       Integer descent, Integer systolicBp, Integer diastolicBp,
                                       Integer heartRate, BigDecimal temperature,
                                       Integer respirationRate, BigDecimal oxygenSaturation,
                                       Integer contractionFreq, Integer contractionDuration,
                                       Integer foetalHeartRate, Integer moulding, String liquor,
                                       String drugsAndFluid, String oxytocin,
                                       Integer urineOutput, String urineAcetone, String urineProtein,
                                       String deliveryDateTime, String deliveryOutcome,
                                       BigDecimal babyWeight, Integer apgarScore1min,
                                       Integer apgarScore5min, Integer apgarScore10min,
                                       Integer apgarScore15min, Integer apgarScore20min,
                                       String placentaType, Integer bloodLossMl, String remarks) {
        if (patientId == null) throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        validateVitals(cervixDilation, descent, systolicBp, diastolicBp, temperature);

        Partograph p = new Partograph();
        p.setPartographId(nextId("t_partograph_seq"));
        p.setPatientId(patientId);
        p.setAdmissionId(admissionId);
        p.setOpdIpdId(opdIpdId);
        p.setOpdIpd(opdIpd);
        p.setParity(parity);
        p.setStartOfLabourDate(startOfLabourDate);
        p.setMembraneStatus(membraneStatus);
        p.setMembraneRuptureDate(membraneRuptureDate);
        p.setMembraneVitalAbb(membraneVitalAbb);
        p.setCervixDilation(cervixDilation);
        p.setDescent(descent);
        p.setSystolicBp(systolicBp);
        p.setDiastolicBp(diastolicBp);
        p.setHeartRate(heartRate);
        p.setTemperature(temperature);
        p.setRespirationRate(respirationRate);
        p.setOxygenSaturation(oxygenSaturation);
        p.setContractionFreq(contractionFreq);
        p.setContractionDuration(contractionDuration);
        p.setFoetalHeartRate(foetalHeartRate);
        p.setMoulding(moulding);
        p.setLiquor(liquor);
        p.setDrugsAndFluid(drugsAndFluid);
        p.setOxytocin(oxytocin);
        p.setUrineOutput(urineOutput);
        p.setUrineAcetone(urineAcetone);
        p.setUrineProtein(urineProtein);
        p.setDeliveryDateTime(deliveryDateTime);
        p.setDeliveryOutcome(deliveryOutcome);
        p.setBabyWeight(babyWeight);
        p.setApgarScore1min(apgarScore1min);
        p.setApgarScore5min(apgarScore5min);
        p.setApgarScore10min(apgarScore10min);
        p.setApgarScore15min(apgarScore15min);
        p.setApgarScore20min(apgarScore20min);
        p.setPlacentaType(placentaType);
        p.setBloodLossMl(bloodLossMl);
        p.setRemarks(remarks);
        p.setAddedBy(AUDIT_ACTOR_ID);
        p.setAddedDatetime(now());
        repo.save(p);
        log.info("Partograph {} created for patient {}", p.getPartographId(), patientId);
        return p;
    }

    @Transactional
    public Partograph updatePartographDeliveryOutcome(Long id, String deliveryOutcome, BigDecimal babyWeight,
                                                      Integer apgarScore1min, Integer apgarScore5min,
                                                      Integer apgarScore10min, Integer apgarScore15min,
                                                      Integer apgarScore20min, String placentaType,
                                                      Integer bloodLossMl, String remarks) {
        Partograph p = repo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Partograph not found: " + id));
        if (deliveryOutcome != null) p.setDeliveryOutcome(deliveryOutcome);
        if (babyWeight != null) p.setBabyWeight(babyWeight);
        if (apgarScore1min != null) p.setApgarScore1min(apgarScore1min);
        if (apgarScore5min != null) p.setApgarScore5min(apgarScore5min);
        if (apgarScore10min != null) p.setApgarScore10min(apgarScore10min);
        if (apgarScore15min != null) p.setApgarScore15min(apgarScore15min);
        if (apgarScore20min != null) p.setApgarScore20min(apgarScore20min);
        if (placentaType != null) p.setPlacentaType(placentaType);
        if (bloodLossMl != null) p.setBloodLossMl(bloodLossMl);
        if (remarks != null) p.setRemarks(remarks);
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(now());
        return repo.save(p);
    }

    @Transactional(readOnly = true)
    public Partograph getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Partograph not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Partograph> listByPatient(Long patientId) {
        if (patientId == null) throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        return repo.findByPatientIdOrderByAddedDatetimeDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<Partograph> listByAdmission(Long admissionId) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        return repo.findByAdmissionIdOrderByAddedDatetimeDesc(admissionId);
    }

    private void validateVitals(BigDecimal cervixDilation, Integer descent, Integer systolicBp,
                                Integer diastolicBp, BigDecimal temperature) {
        if (cervixDilation != null && (cervixDilation.compareTo(BigDecimal.ZERO) < 0 || cervixDilation.compareTo(BigDecimal.TEN) > 0))
            throw new InvalidNursingRequestException("BEXA003", "cervixDilation must be between 0 and 10 cm");
        if (descent != null && (descent < -5 || descent > 5))
            throw new InvalidNursingRequestException("BEXA003", "descent must be between -5 and +5");
        if (systolicBp != null && (systolicBp < 0 || systolicBp > 300))
            throw new InvalidNursingRequestException("BEXA003", "systolicBp out of range");
        if (diastolicBp != null && (diastolicBp < 0 || diastolicBp > 200))
            throw new InvalidNursingRequestException("BEXA003", "diastolicBp out of range");
        if (temperature != null && (temperature.compareTo(new BigDecimal("30")) < 0 || temperature.compareTo(new BigDecimal("45")) > 0))
            throw new InvalidNursingRequestException("BEXA003", "temperature must be between 30 and 45 °C");
    }

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}

package com.hims.ot.application;

import com.hims.ot.application.exceptions.OtRuleException;
import com.hims.ot.domain.model.*;
import com.hims.ot.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class AnesthesiaRecordService {

    private final AnesthesiaRecordRepository recordRepo;
    private final AnesthesiaDrugRepository drugRepo;
    private final VitalSignRecordRepository vitalRepo;

    public AnesthesiaRecordService(AnesthesiaRecordRepository recordRepo,
                                   AnesthesiaDrugRepository drugRepo,
                                   VitalSignRecordRepository vitalRepo) {
        this.recordRepo = recordRepo;
        this.drugRepo = drugRepo;
        this.vitalRepo = vitalRepo;
    }

    public AnesthesiaRecord createPreOp(Long scheduleId, Long patientId, String asaGrade, String airway, String comorbidities) {
        AnesthesiaRecord ar = new AnesthesiaRecord();
        ar.setScheduleId(scheduleId);
        ar.setPatientId(patientId);
        ar.setAsaGrade(asaGrade);
        ar.setAirwayAssessment(airway);
        ar.setComorbidities(comorbidities);
        ar.setPreAnesthesiaNotes("Pre-op assessment");
        ar.setAddedBy(1L);
        ar.setAddedDatetime(Timestamp.from(Instant.now()));
        return recordRepo.save(ar);
    }

    public VitalSignRecord addVital(Long recordId, Timestamp recordedAt, Integer hr, Integer bpSys, Integer bpDia, Integer spo2, Integer rr, java.math.BigDecimal temp) {
        VitalSignRecord vs = new VitalSignRecord();
        vs.setRecordId(recordId);
        vs.setRecordedAt(recordedAt);
        vs.setHeartRate(hr);
        vs.setBpSystolic(bpSys);
        vs.setBpDiastolic(bpDia);
        vs.setSpo2(spo2);
        vs.setRespiratoryRate(rr);
        vs.setTemperature(temp);
        vs.setAddedBy(1L);
        vs.setAddedDatetime(Timestamp.from(Instant.now()));
        return vitalRepo.save(vs);
    }

    public AnesthesiaDrug addDrug(Long recordId, String drugName, String dose, String route, Timestamp administeredAt) {
        AnesthesiaDrug d = new AnesthesiaDrug();
        d.setRecordId(recordId);
        d.setDrugName(drugName);
        d.setDose(dose);
        d.setRoute(route);
        d.setAdministeredAt(administeredAt);
        d.setAddedBy(1L);
        d.setAddedDatetime(Timestamp.from(Instant.now()));
        return drugRepo.save(d);
    }

    public AnesthesiaRecord completePostOp(Long recordId, Integer pacuMinutes, Integer recoveryScore, String complications) {
        AnesthesiaRecord ar = recordRepo.findById(recordId).orElseThrow(() -> new OtRuleException("NOT_FOUND", "Record not found"));
        ar.setPacuStayMinutes(pacuMinutes);
        ar.setRecoveryScore(recoveryScore);
        ar.setComplications(complications);
        ar.setPostAnesthesiaNotes("Post-op completed");
        ar.setUpdatedBy(1L);
        ar.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return recordRepo.save(ar);
    }

    public List<VitalSignRecord> findVitals(Long recordId) {
        return vitalRepo.findByRecordId(recordId);
    }
}

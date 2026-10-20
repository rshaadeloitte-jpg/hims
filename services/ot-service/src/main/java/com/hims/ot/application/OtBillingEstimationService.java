package com.hims.ot.application;

import com.hims.ot.domain.model.*;
import com.hims.ot.domain.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class OtBillingEstimationService {

    private final OtBillingEstimateRepository estimateRepo;
    private final OtProcedureCostRepository costRepo;

    public OtBillingEstimationService(OtBillingEstimateRepository estimateRepo, OtProcedureCostRepository costRepo) {
        this.estimateRepo = estimateRepo;
        this.costRepo = costRepo;
    }

    public OtBillingEstimate createEstimate(Long scheduleId, String procedureCode) {
        OtBillingEstimate est = new OtBillingEstimate();
        est.setScheduleId(scheduleId);
        est.setProcedureCode(procedureCode);
        est.setTotalEstimated(BigDecimal.ZERO);
        est.setSurgeonFee(BigDecimal.ZERO);
        est.setAnesthetistFee(BigDecimal.ZERO);
        est.setOtCharges(BigDecimal.ZERO);
        est.setImplantCost(BigDecimal.ZERO);
        est.setConsumableCost(BigDecimal.ZERO);
        est.setAddedBy(1L);
        est.setAddedDatetime(Timestamp.from(Instant.now()));
        return estimateRepo.save(est);
    }

    public OtProcedureCost addCost(Long estimateId, String itemName, String itemType, BigDecimal amount) {
        OtProcedureCost pc = new OtProcedureCost();
        pc.setEstimateId(estimateId);
        pc.setItemName(itemName);
        pc.setItemType(itemType);
        pc.setAmount(amount);
        pc.setAddedBy(1L);
        pc.setAddedDatetime(Timestamp.from(Instant.now()));
        return costRepo.save(pc);
    }

    public OtBillingEstimate finalizeEstimate(Long estimateId) {
        List<OtProcedureCost> costs = costRepo.findByEstimateId(estimateId);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal implant = BigDecimal.ZERO;
        BigDecimal consumable = BigDecimal.ZERO;
        BigDecimal surgeon = BigDecimal.ZERO;
        BigDecimal anesthetist = BigDecimal.ZERO;
        BigDecimal ot = BigDecimal.ZERO;
        for (OtProcedureCost c : costs) {
            total = total.add(c.getAmount());
            if ("IMPLANT".equals(c.getItemType())) implant = implant.add(c.getAmount());
            else if ("CONSUMABLE".equals(c.getItemType())) consumable = consumable.add(c.getAmount());
            else if ("SURGEON".equals(c.getItemType())) surgeon = surgeon.add(c.getAmount());
            else if ("ANESTHETIST".equals(c.getItemType())) anesthetist = anesthetist.add(c.getAmount());
            else if ("OT".equals(c.getItemType())) ot = ot.add(c.getAmount());
        }
        OtBillingEstimate est = estimateRepo.findById(estimateId).orElseThrow(() -> new RuntimeException("Not found"));
        est.setTotalEstimated(total);
        est.setImplantCost(implant);
        est.setConsumableCost(consumable);
        est.setSurgeonFee(surgeon);
        est.setAnesthetistFee(anesthetist);
        est.setOtCharges(ot);
        est.setUpdatedBy(1L);
        est.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return estimateRepo.save(est);
    }
}

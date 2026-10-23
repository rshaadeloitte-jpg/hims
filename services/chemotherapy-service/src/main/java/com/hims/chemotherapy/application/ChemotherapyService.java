package com.hims.chemotherapy.application;

import com.hims.chemotherapy.domain.model.*;
import com.hims.chemotherapy.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChemotherapyService {
    private final ChemoPlanRepository planRepo;
    private final ChemoRegimenRepository regimenRepo;
    private final ChemoRegimenDetailRepository regimenDetailRepo;
    private final ChemoCycleRepository cycleRepo;
    private final ChemoCoversheetRepository coversheetRepo;
    private final TixmlMappingRepository tixmlRepo;

    public ChemotherapyService(ChemoPlanRepository planRepo, ChemoRegimenRepository regimenRepo,
                               ChemoRegimenDetailRepository regimenDetailRepo,
                               ChemoCycleRepository cycleRepo, ChemoCoversheetRepository coversheetRepo,
                               TixmlMappingRepository tixmlRepo) {
        this.planRepo = planRepo; this.regimenRepo = regimenRepo; this.regimenDetailRepo = regimenDetailRepo;
        this.cycleRepo = cycleRepo;
        this.coversheetRepo = coversheetRepo; this.tixmlRepo = tixmlRepo;
    }

    public List<ChemoPlan> getPlanByPatient(Long patientId) { return planRepo.findByPatientId(patientId); }
    public ChemoPlan savePlan(ChemoPlan plan) { return planRepo.save(plan); }
    /** Drug-level regimen lines of a plan (legacy {@code t_chemoplan_detail}). */
    public List<ChemoRegimenDetail> getRegimenLines(Long planId) { return regimenDetailRepo.findByPlanId(planId); }
    /** Regimen header rows for one patient. */
    public List<ChemoRegimen> getRegimenByPatient(Long patientId) { return regimenRepo.findByPatientId(patientId); }
    public ChemoRegimenDetail saveRegimenLine(ChemoRegimenDetail line) { return regimenDetailRepo.save(line); }
    public List<ChemoCycle> getCycle(Long planId) { return cycleRepo.findByPlanId(planId); }
    public ChemoCycle completeCycle(Long cycleId) { ChemoCycle c = cycleRepo.findById(cycleId).orElseThrow(); c.setStatus("COMPLETED"); return cycleRepo.save(c); }
    public ChemoCoversheet getCoversheet(Long planId) { return coversheetRepo.findByPlanId(planId).orElse(null); }
    public TixmlMapping getTixml(String name) { return tixmlRepo.findByTemplateName(name).orElse(null); }
}

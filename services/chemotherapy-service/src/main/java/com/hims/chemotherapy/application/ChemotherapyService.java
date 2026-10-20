package com.hims.chemotherapy.application;

import com.hims.chemotherapy.domain.model.*;
import com.hims.chemotherapy.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChemotherapyService {
    private final ChemoPlanRepository planRepo;
    private final ChemoRegimenRepository regimenRepo;
    private final ChemoCycleRepository cycleRepo;
    private final ChemoCoversheetRepository coversheetRepo;
    private final TixmlMappingRepository tixmlRepo;

    public ChemotherapyService(ChemoPlanRepository planRepo, ChemoRegimenRepository regimenRepo,
                               ChemoCycleRepository cycleRepo, ChemoCoversheetRepository coversheetRepo,
                               TixmlMappingRepository tixmlRepo) {
        this.planRepo = planRepo; this.regimenRepo = regimenRepo; this.cycleRepo = cycleRepo;
        this.coversheetRepo = coversheetRepo; this.tixmlRepo = tixmlRepo;
    }

    public List<ChemoPlan> getPlanByPatient(Long patientId) { return planRepo.findByPatientId(patientId); }
    public ChemoPlan savePlan(ChemoPlan plan) { return planRepo.save(plan); }
    public List<ChemoRegimen> getRegimen(Long planId) { return regimenRepo.findByPlanId(planId); }
    public List<ChemoCycle> getCycle(Long planId) { return cycleRepo.findByPlanId(planId); }
    public ChemoCycle completeCycle(Long cycleId) { ChemoCycle c = cycleRepo.findById(cycleId).orElseThrow(); c.setStatus("COMPLETED"); return cycleRepo.save(c); }
    public ChemoCoversheet getCoversheet(Long planId) { return coversheetRepo.findByPlanId(planId).orElse(null); }
    public TixmlMapping getTixml(String name) { return tixmlRepo.findByTemplateName(name).orElse(null); }
}

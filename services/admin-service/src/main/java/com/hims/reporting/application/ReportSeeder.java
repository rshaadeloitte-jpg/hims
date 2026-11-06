package com.hims.reporting.application;

import com.hims.reporting.domain.model.ReportMaster;
import com.hims.reporting.domain.repository.ReportMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time bootstrap of the 15 most-used legacy report definitions (M25).
 * Idempotent on reportCode: skips rows that already exist, never updates
 * a live definition. Each report points at the rm_* read models and uses
 * the same whitelist the engine enforces at create-time.
 */
@Component
public class ReportSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ReportSeeder.class);
    private final ReportMasterRepository repo;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    public ReportSeeder(ReportMasterRepository repo) { this.repo = repo; }

    private record SeedDef(String code, String title, short kind, Map<String, Object> def) {}

    private static List<SeedDef> seeds() {
        return List.of(
            new SeedDef("DAILY_OPD",           "Daily OPD Count",                   (short) 1, t()),
            new SeedDef("DAILY_IPD",           "Daily IPD Admissions",              (short) 1, t()),
            new SeedDef("DAILY_ENCOUNTERS",    "Daily Encounters (any type)",       (short) 1, t()),
            new SeedDef("DAILY_REVENUE",       "Daily Revenue by Head",             (short) 1, r()),
            new SeedDef("DAILY_OT",            "Daily OT Operations",               (short) 1, o()),
            new SeedDef("DAILY_LAB",           "Daily Lab Verified",                (short) 1, o()),
            new SeedDef("DAILY_BLOOD",         "Daily Blood Issued",                (short) 1, o()),
            new SeedDef("PATIENT_REGISTRY",    "Patient Registry",                  (short) 1, p()),
            new SeedDef("OPD_LIST",            "OPD List",                          (short) 1, t()),
            new SeedDef("IPD_LIST",            "IPD List",                          (short) 1, t()),
            new SeedDef("SUMMARY_ENCOUNTERS",  "Total Encounters",                  (short) 2, e()),
            new SeedDef("SUMMARY_REVENUE",     "Total Revenue",                     (short) 2, r()),
            new SeedDef("SUMMARY_OT",          "Total OT Operations",               (short) 2, o()),
            new SeedDef("SUMMARY_LAB",         "Total Lab Verified",                (short) 2, o()),
            new SeedDef("SUMMARY_BLOOD",       "Total Blood Issued",                (short) 2, o())
        );
    }
    private static Map<String,Object> t() { Map<String,Object> m = new LinkedHashMap<>(); m.put("table","rm_encounter_stats"); m.put("columns", List.of("stat_date","encounter_type","cnt")); m.put("dateField","stat_date"); return m; }
    private static Map<String,Object> e() { return t(); }
    private static Map<String,Object> r() { Map<String,Object> m = new LinkedHashMap<>(); m.put("table","rm_revenue_daily"); m.put("columns", List.of("stat_date","head_code","total")); m.put("dateField","stat_date"); return m; }
    private static Map<String,Object> o() { Map<String,Object> m = new LinkedHashMap<>(); m.put("table","rm_op_stats"); m.put("columns", List.of("stat_date","op_type","cnt")); m.put("dateField","stat_date"); return m; }
    private static Map<String,Object> p() { Map<String,Object> m = new LinkedHashMap<>(); m.put("table","rm_patient_registry"); m.put("columns", List.of("patient_id","mrn","name","registered_at")); m.put("dateField","registered_at"); return m; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        for (SeedDef seed : seeds()) {
            if (repo.existsByReportCode(seed.code())) continue;
            ReportMaster m = new ReportMaster();
            m.setReportId(nextId());
            m.setReportCode(seed.code());
            m.setTitle(seed.title());
            m.setKind(seed.kind());
            try { m.setDefinition(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(seed.def())); }
            catch (Exception e) { throw new IllegalStateException("Cannot serialize " + seed.code(), e); }
            m.setIsActive(Boolean.TRUE);
            m.setAddedBy(1L);
            m.setAddedDatetime(new java.sql.Timestamp(System.currentTimeMillis()));
            try { repo.save(m); added++; }
            catch (DataIntegrityViolationException dup) { /* race */ }
        }
        if (added > 0) log.info("Seeded {} M25 report definition(s)", added);
    }
    private long nextId() {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_reporting.t_report_master_seq')")
                .getSingleResult()).longValue();
    }
}

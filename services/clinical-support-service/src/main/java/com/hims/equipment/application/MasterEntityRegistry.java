package com.hims.equipment.application;

import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.AmcAgentMaster;
import com.hims.equipment.domain.model.ComplaintCategoryMaster;
import com.hims.equipment.domain.model.ComplaintStatusMaster;
import com.hims.equipment.domain.model.ComplaintTypeMaster;
import com.hims.equipment.domain.model.ComplianceMaster;
import com.hims.equipment.domain.model.ElectricalChecklistMaster;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentChecklist;
import com.hims.equipment.domain.model.EquipmentPreventiveChecklist;
import com.hims.equipment.domain.model.EquipmentWorkingStatusMaster;
import com.hims.equipment.domain.model.FunctionalChecklistMaster;
import com.hims.equipment.domain.model.InsuranceAgentMaster;
import com.hims.equipment.domain.model.MaintenanceTypeMaster;
import com.hims.equipment.domain.model.ServiceAgentMaster;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of the 13 V1 equipment master tables exposed through the generic
 * {@code /api/v1/equipment/masters/{masterType}} endpoints (MIG-023 Task 17),
 * following the reference-service MasterEntityRegistry pattern (MIG-010).
 * Each definition carries everything the generic read/write path needs —
 * entity class, per-table sequence (IMP-DEC-003), required business fields
 * and the whitelist of searchable columns for the free-text filter.
 *
 * <p>Search/field whitelists are hardcoded here, never client input: the
 * generic repository interpolates only registry-owned identifiers into JPQL
 * and binds every value as a parameter (injection-safe by construction,
 * IMP-DEC-010 hardening parity).</p>
 */
@Component
public class MasterEntityRegistry {

    /**
     * @param sequenceName  explicit per-table sequence the write path draws ids from
     * @param requiredFields business fields that must be non-blank on create
     *                       (NOT NULL columns; validated before any sequence burn)
     * @param searchFields   columns eligible for the {@code ?search=} LIKE filter
     */
    public record MasterDef(String name,
                            Class<? extends EquipmentBase> entityClass,
                            String sequenceName,
                            Set<String> requiredFields,
                            List<String> searchFields) {}

    private final Map<String, MasterDef> definitions = new LinkedHashMap<>();

    public MasterEntityRegistry() {
        // ---- checklist / PM masters ----
        register(new MasterDef("equipment-checklist", EquipmentChecklist.class,
                "equipment_checklist_seq",
                Set.of(),
                List.of("serialNo", "description", "partNo")));
        register(new MasterDef("preventive-checklist", EquipmentPreventiveChecklist.class,
                "equipment_preventive_checklist_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("functional-checklist", FunctionalChecklistMaster.class,
                "functional_checklist_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("electrical-checklist", ElectricalChecklistMaster.class,
                "electrical_checklist_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        // ---- complaint dictionary ----
        register(new MasterDef("complaint-category", ComplaintCategoryMaster.class,
                "complaint_category_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("complaint-status", ComplaintStatusMaster.class,
                "complaint_status_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("complaint-type", ComplaintTypeMaster.class,
                "complaint_type_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        // ---- compliance + maintenance + working status ----
        register(new MasterDef("compliance", ComplianceMaster.class,
                "compliance_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("maintenance-type", MaintenanceTypeMaster.class,
                "maintenance_type_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        register(new MasterDef("working-status", EquipmentWorkingStatusMaster.class,
                "equipment_working_status_master_seq",
                Set.of("code", "description"),
                List.of("code", "description")));
        // ---- agents ----
        register(new MasterDef("amc-agent", AmcAgentMaster.class,
                "amc_agent_master_seq",
                Set.of("code", "name"),
                List.of("code", "name")));
        register(new MasterDef("service-agent", ServiceAgentMaster.class,
                "service_agent_master_seq",
                Set.of("code", "name"),
                List.of("code", "name")));
        register(new MasterDef("insurance-agent", InsuranceAgentMaster.class,
                "insurance_agent_master_seq",
                Set.of("code", "name"),
                List.of("code", "name")));
    }

    private void register(MasterDef def) {
        definitions.put(def.name(), def);
    }

    /** Resolve a masterType path segment or fail 400 BEXA003 listing the supported set. */
    public MasterDef require(String name) {
        MasterDef def = definitions.get(name == null ? "" : name.toLowerCase());
        if (def == null) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Unknown master type: " + name + ". Supported: " + definitions.keySet());
        }
        return def;
    }

    public Set<String> names() {
        return new LinkedHashSet<>(definitions.keySet());
    }
}

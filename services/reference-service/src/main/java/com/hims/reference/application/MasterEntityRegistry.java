package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.domain.model.CompanyMaster;
import com.hims.reference.domain.model.DepartmentMaster;
import com.hims.reference.domain.model.Doctor;
import com.hims.reference.domain.model.DoctorType;
import com.hims.reference.domain.model.FacilityLocation;
import com.hims.reference.domain.model.GeoArea;
import com.hims.reference.domain.model.GeoCity;
import com.hims.reference.domain.model.GeoCountry;
import com.hims.reference.domain.model.GeoDistrict;
import com.hims.reference.domain.model.GeoState;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.model.Specialization;
import com.hims.reference.domain.model.Subunit;
import com.hims.reference.domain.model.UnitMaster;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of master entities exposed by this service, with the filter whitelist
 * taken verbatim from the legacy {@code @CrudApi(allowedFilters=...)} annotations:
 *   Unitmaster       -> id, status
 *   Departmentmaster -> status, isclinical, isEmergency, isteleconsult, defaultdepartment
 *   Companymaster    -> description, status
 * (evidence: D:\ihms\...\models\masters\Unitmaster.java:41, Departmentmaster.java:41, Companymaster.java:...)
 */
@Component
public class MasterEntityRegistry {

    /**
     * @param sequenceName explicit per-table sequence (IMP-DEC-003; legacy <TABLE>_SEQ
     *                     convention). Used by the write path to assign ids on create
     *                     when the client does not supply one.
     */
    public record MasterDef(String name,
                            Class<? extends MasterEntityBase> entityClass,
                            Set<String> allowedFilters,
                            Set<String> requiredFields,
                            String sequenceName) {}

    private final Map<String, MasterDef> definitions = new LinkedHashMap<>();

    public MasterEntityRegistry() {
        register(new MasterDef("unitmaster", UnitMaster.class, Set.of("id", "status"),
                Set.of("code", "name"), "M_UNITMASTER_SEQ"));
        register(new MasterDef("departmentmaster", DepartmentMaster.class,
                Set.of("status", "isclinical", "isEmergency", "isteleconsult", "defaultdepartment"),
                Set.of("code", "description"), "M_DEPARTMENTMASTER_SEQ"));
        register(new MasterDef("companymaster", CompanyMaster.class, Set.of("description", "status"),
                Set.of("code", "description"), "M_COMPANYMASTER_SEQ"));
        // ---- MIG-011 clean target masters (V5, IMP-DEC-021) ----
        register(new MasterDef("doctor", Doctor.class,
                Set.of("status", "doctorTypeId", "specializationId"),
                Set.of("firstName", "lastName"), "doctor_seq"));
        register(new MasterDef("doctor-type", DoctorType.class, Set.of("status"),
                Set.of("code", "description"), "doctor_type_seq"));
        register(new MasterDef("specialization", Specialization.class,
                Set.of("status", "isDoctorShow"),
                Set.of("code", "description"), "specialization_seq"));
        register(new MasterDef("facility-location", FacilityLocation.class,
                Set.of("status", "locationType"),
                Set.of("locationType", "name"), "facility_location_seq"));
        // ---- MIG-011 follow-up: subunit + geo reference (V5, D-6 global scope) ----
        // Filter whitelists extend the legacy @CrudApi surface: legacy bare @CrudApi
        // exposed no server-side filters (consumers used @CrudCached maps keyed by
        // countryId/stateId/districtId/cityId); the clean contract exposes those
        // parent-FK filters server-side (gateway maps legacy expectations).
        register(new MasterDef("subunit", Subunit.class, Set.of("status"),
                Set.of("code"), "subunit_seq"));
        register(new MasterDef("geo-country", GeoCountry.class, Set.of("status"),
                Set.of("code", "name"), "geo_country_seq"));
        register(new MasterDef("geo-state", GeoState.class, Set.of("status", "countryId"),
                Set.of("countryId", "code", "name"), "geo_state_seq"));
        register(new MasterDef("geo-district", GeoDistrict.class, Set.of("status", "zoneId", "stateId"),
                Set.of("stateId", "code", "name"), "geo_district_seq"));
        register(new MasterDef("geo-city", GeoCity.class, Set.of("status", "districtId", "stateId"),
                Set.of("stateId", "code", "name"), "geo_city_seq"));
        register(new MasterDef("geo-area", GeoArea.class, Set.of("status", "cityId"),
                Set.of("cityId", "code", "name"), "geo_area_seq"));
    }

    private void register(MasterDef def) {
        definitions.put(def.name(), def);
    }

    public MasterDef require(String name) {
        MasterDef def = definitions.get(name == null ? "" : name.toLowerCase());
        if (def == null) {
            throw new InvalidMasterRequestException("UNKNOWN_ENTITY",
                    "Unknown master entity: " + name + ". Supported: " + definitions.keySet());
        }
        return def;
    }

    public Set<String> names() {
        return definitions.keySet();
    }
}

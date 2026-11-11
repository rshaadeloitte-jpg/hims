package com.hims.govt.application;

import com.hims.govt.domain.model.AshaWork;
import com.hims.govt.domain.model.EjsAsha;
import com.hims.govt.domain.model.Geography;
import com.hims.govt.domain.repository.AshaWorkRepository;
import com.hims.govt.domain.repository.EjsAshaRepository;
import com.hims.govt.domain.repository.GeographyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class EjsTrackingService {

    private final AshaWorkRepository ashaWorkRepository;
    private final GeographyRepository geographyRepository;
    private final EjsAshaRepository ashaRepository;

    public EjsTrackingService(AshaWorkRepository ashaWorkRepository,
                              GeographyRepository geographyRepository,
                              EjsAshaRepository ashaRepository) {
        this.ashaWorkRepository = ashaWorkRepository;
        this.geographyRepository = geographyRepository;
        this.ashaRepository = ashaRepository;
    }

    // ---- AshaWork ----

    @Transactional(readOnly = true)
    public List<AshaWork> listAshaWork(String trackingType, Long ashaId) {
        if (trackingType != null && !trackingType.isBlank() && ashaId != null) {
            return ashaWorkRepository.findAll().stream()
                    .filter(w -> trackingType.equalsIgnoreCase(w.getTrackingType()) && ashaId.equals(w.getAshaId()))
                    .toList();
        }
        if (trackingType != null && !trackingType.isBlank()) {
            return ashaWorkRepository.findByTrackingType(trackingType);
        }
        if (ashaId != null) {
            return ashaWorkRepository.findByAshaId(ashaId);
        }
        return ashaWorkRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AshaWork> getAshaWork(Long id) {
        return ashaWorkRepository.findById(id);
    }

    public AshaWork createAshaWork(Map<String, Object> payload) {
        AshaWork w = new AshaWork();
        applyAshaWork(w, payload);
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());
        return ashaWorkRepository.save(w);
    }

    public AshaWork updateAshaWork(Long id, Map<String, Object> payload) {
        AshaWork w = ashaWorkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AshaWork not found: " + id));
        applyAshaWork(w, payload);
        w.setUpdatedAt(LocalDateTime.now());
        return ashaWorkRepository.save(w);
    }

    private void applyAshaWork(AshaWork w, Map<String, Object> p) {
        if (p.get("beneficiaryId") != null) w.setBeneficiaryId(toLong(p.get("beneficiaryId")));
        if (p.get("beneficiaryName") != null) w.setBeneficiaryName(String.valueOf(p.get("beneficiaryName")));
        if (p.get("trackingType") != null) w.setTrackingType(String.valueOf(p.get("trackingType")));
        if (p.get("visitDate") != null) w.setVisitDate(String.valueOf(p.get("visitDate")));
        if (p.get("ashaId") != null) w.setAshaId(toLong(p.get("ashaId")));
        if (p.get("ashaName") != null) w.setAshaName(String.valueOf(p.get("ashaName")));
        if (p.get("status") != null) w.setStatus(String.valueOf(p.get("status")));
        if (p.get("gestationalWeek") != null) w.setGestationalWeek(toInt(p.get("gestationalWeek")));
        if (p.get("weight") != null) w.setWeight(toBD(p.get("weight")));
        if (p.get("bloodPressureSystolic") != null) w.setBpSystolic(toInt(p.get("bloodPressureSystolic")));
        if (p.get("bloodPressureDiastolic") != null) w.setBpDiastolic(toInt(p.get("bloodPressureDiastolic")));
        if (p.get("hemoglobin") != null) w.setHemoglobin(toBD(p.get("hemoglobin")));
        if (p.get("immunizationGiven") != null) w.setImmunizationGiven(String.valueOf(p.get("immunizationGiven")));
        if (p.get("deliveryDate") != null) w.setDeliveryDate(String.valueOf(p.get("deliveryDate")));
        if (p.get("deliveryType") != null) w.setDeliveryType(String.valueOf(p.get("deliveryType")));
        if (p.get("postnatalDay") != null) w.setPostnatalDay(toInt(p.get("postnatalDay")));
        if (p.get("childAgeMonths") != null) w.setChildAgeMonths(toInt(p.get("childAgeMonths")));
        if (p.get("weightChild") != null) w.setWeightChild(toBD(p.get("weightChild")));
        if (p.get("heightChild") != null) w.setHeightChild(toBD(p.get("heightChild")));
        if (p.get("immunizationStatus") != null) w.setImmunizationStatus(String.valueOf(p.get("immunizationStatus")));
        if (p.get("nutritionStatus") != null) w.setNutritionStatus(String.valueOf(p.get("nutritionStatus")));
        if (p.get("remarks") != null) w.setRemarks(String.valueOf(p.get("remarks")));
        if (p.get("nextVisitDate") != null) w.setNextVisitDate(String.valueOf(p.get("nextVisitDate")));
    }

    // ---- Geography ----

    @Transactional(readOnly = true)
    public List<Geography> listGeography(Long ashaId) {
        if (ashaId != null) return geographyRepository.findByAshaId(ashaId);
        return geographyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Geography> getGeography(Long id) {
        return geographyRepository.findById(id);
    }

    public Geography createGeography(Map<String, Object> payload) {
        Geography g = new Geography();
        applyGeography(g, payload);
        g.setCreatedAt(LocalDateTime.now());
        g.setUpdatedAt(LocalDateTime.now());
        return geographyRepository.save(g);
    }

    public Geography updateGeography(Long id, Map<String, Object> payload) {
        Geography g = geographyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geography not found: " + id));
        applyGeography(g, payload);
        g.setUpdatedAt(LocalDateTime.now());
        return geographyRepository.save(g);
    }

    private void applyGeography(Geography g, Map<String, Object> p) {
        if (p.get("ashaId") != null) g.setAshaId(toLong(p.get("ashaId")));
        if (p.get("ashaName") != null) g.setAshaName(String.valueOf(p.get("ashaName")));
        if (p.get("state") != null) g.setState(String.valueOf(p.get("state")));
        if (p.get("district") != null) g.setDistrict(String.valueOf(p.get("district")));
        if (p.get("block") != null) g.setBlock(String.valueOf(p.get("block")));
        if (p.get("village") != null) g.setVillage(String.valueOf(p.get("village")));
        if (p.get("subCenter") != null) g.setSubCenter(String.valueOf(p.get("subCenter")));
        if (p.get("totalPopulation") != null) g.setTotalPopulation(toInt(p.get("totalPopulation")));
        if (p.get("targetCouples") != null) g.setTargetCouples(toInt(p.get("targetCouples")));
        if (p.get("pregnantWomen") != null) g.setPregnantWomen(toInt(p.get("pregnantWomen")));
        if (p.get("childrenUnder5") != null) g.setChildrenUnder5(toInt(p.get("childrenUnder5")));
        if (p.get("households") != null) g.setHouseholds(toInt(p.get("households")));
        if (p.get("eligibleCouples") != null) g.setEligibleCouples(toInt(p.get("eligibleCouples")));
        if (p.get("isActive") != null) g.setIsActive(Boolean.valueOf(String.valueOf(p.get("isActive"))));
        if (p.get("lastEnumerationDate") != null) g.setLastEnumerationDate(String.valueOf(p.get("lastEnumerationDate")));
        if (p.get("remarks") != null) g.setRemarks(String.valueOf(p.get("remarks")));
    }

    // ---- Asha workers ----

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAshaWorkers() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (EjsAsha a : ashaRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ashaId", a.getAshaId());
            m.put("ashaName", a.getName());
            out.add(m);
        }
        return out;
    }

    private Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private Integer toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    private BigDecimal toBD(Object o) {
        return new BigDecimal(String.valueOf(o));
    }
}
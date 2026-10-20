package com.hims.adt.application;

import com.hims.adt.application.exceptions.AdtEntityNotFoundException;
import com.hims.adt.application.exceptions.InvalidAdtRequestException;
import com.hims.adt.domain.model.Bed;
import com.hims.adt.domain.model.Ward;
import com.hims.adt.domain.repository.BedRepository;
import com.hims.adt.domain.repository.WardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Ward + bed master CRUD (GAP-REGISTER WP-5): m_ward and m_bed live in ihms_adt
 * (ownership moved from Reference per MICROSERVICE-CATALOG #3 / POSTGRESQL-MIGRATION §1.1).
 * Bed status master is seeded read-only reference data (V1 migration).
 */
@Service
public class MasterDataService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final WardRepository wardRepository;
    private final BedRepository bedRepository;

    @PersistenceContext
    private EntityManager em;

    public MasterDataService(WardRepository wardRepository, BedRepository bedRepository) {
        this.wardRepository = wardRepository;
        this.bedRepository = bedRepository;
    }

    // ------------------------------------------------------------------ wards

    @Transactional
    public Ward createWard(Long unitId, String name, String wardType, Boolean censusFlag) {
        if (name == null || name.isBlank()) {
            throw new InvalidAdtRequestException("WARD_NAME_REQUIRED", "name is required");
        }
        Ward w = new Ward();
        w.setWardId(nextId("m_ward_seq"));
        w.setName(name);
        w.setWardType(wardType);
        w.setIscensus(censusFlag == null || censusFlag);
        w.setUnitId(unitId);
        w.setStatus(true);
        w.setSynchronizedd(false);
        w.setAddedBy(AUDIT_ACTOR_ID);
        w.setAddedDatetime(now());
        return wardRepository.save(w);
    }

    @Transactional(readOnly = true)
    public List<Ward> listWards(Long unitId) {
        return unitId == null ? wardRepository.findAll() : wardRepository.findByUnitIdAndStatusTrue(unitId);
    }

    @Transactional(readOnly = true)
    public Ward getWard(long id) {
        return wardRepository.findById(id)
                .orElseThrow(() -> new AdtEntityNotFoundException("WARD_NOT_FOUND", "Ward not found: " + id));
    }

    @Transactional
    public Ward updateWard(long id, String name, String wardType, Boolean censusFlag) {
        Ward w = getWard(id);
        if (name != null && !name.isBlank()) w.setName(name);
        if (wardType != null) w.setWardType(wardType);
        if (censusFlag != null) w.setIscensus(censusFlag);
        w.setUpdatedBy(AUDIT_ACTOR_ID);
        w.setUpdatedDatetime(now());
        return wardRepository.save(w);
    }

    // ------------------------------------------------------------------ beds

    @Transactional
    public Bed createBed(String bedNo, Long wardId, String category, String building,
                         Short bedStatusId, Boolean isnoncensus, Boolean isot,
                         Boolean ispreop, Boolean ispostop, Long unitId) {
        if (bedNo == null || bedNo.isBlank()) {
            throw new InvalidAdtRequestException("BED_NO_REQUIRED", "bedNo is required");
        }
        if (wardId == null) {
            throw new InvalidAdtRequestException("BED_WARD_REQUIRED", "wardId is required");
        }
        getWard(wardId);   // FK guard with clean 404
        Bed b = new Bed();
        b.setBedId(nextId("m_bed_seq"));
        b.setBedNo(bedNo);
        b.setWardId(wardId);
        b.setCategory(category);
        b.setBuilding(building);
        b.setBedStatusId(bedStatusId == null ? (short) 1 : bedStatusId);
        b.setOccupied(false);
        b.setIsnoncensus(Boolean.TRUE.equals(isnoncensus));
        b.setIsot(Boolean.TRUE.equals(isot));
        b.setIspreop(Boolean.TRUE.equals(ispreop));
        b.setIspostop(Boolean.TRUE.equals(ispostop));
        b.setUnitId(unitId);
        b.setStatus(true);
        b.setSynchronizedd(false);
        b.setAddedBy(AUDIT_ACTOR_ID);
        b.setAddedDatetime(now());
        return bedRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<Bed> listBeds(Long wardId) {
        return wardId == null ? bedRepository.findAll() : bedRepository.findByWardIdAndStatusTrue(wardId);
    }

    @Transactional(readOnly = true)
    public Bed getBed(long id) {
        return bedRepository.findById(id)
                .orElseThrow(() -> new AdtEntityNotFoundException("BED_NOT_FOUND", "Bed not found: " + id));
    }

    /** Free beds matching the BR-IPD-002 predicate (+ BR-IPD-003 isrelease gate), optionally per ward. */
    @Transactional(readOnly = true)
    public List<Bed> availableBeds(Long wardId) {
        List<Bed> free = bedRepository.findFreeBeds();
        return wardId == null ? free : free.stream().filter(b -> wardId.equals(b.getWardId())).toList();
    }

    @Transactional
    public Bed updateBed(long id, String category, String building, Short bedStatusId,
                         Boolean isnoncensus, Boolean isot, Boolean ispreop, Boolean ispostop) {
        Bed b = getBed(id);
        if (category != null) b.setCategory(category);
        if (building != null) b.setBuilding(building);
        if (bedStatusId != null) b.setBedStatusId(bedStatusId);
        if (isnoncensus != null) b.setIsnoncensus(isnoncensus);
        if (isot != null) b.setIsot(isot);
        if (ispreop != null) b.setIspreop(ispreop);
        if (ispostop != null) b.setIspostop(ispostop);
        b.setUpdatedBy(AUDIT_ACTOR_ID);
        b.setUpdatedDatetime(now());
        return bedRepository.save(b);
    }

    // ------------------------------------------------------------------ helpers

    private Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_adt." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }
}

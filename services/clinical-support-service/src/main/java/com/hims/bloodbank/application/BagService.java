package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Bag/component registration (t_bloodbag; t_bloodcomponent rows deferred with component intake flow). */
@Service
public class BagService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final BloodBagRepository bagRepository;
    private final ClockHolder clocks;

    @PersistenceContext
    private EntityManager em;

    public BagService(BloodBagRepository bagRepository, ClockHolder clocks) {
        this.bagRepository = bagRepository;
        this.clocks = clocks;
    }

    @Transactional
    public BloodBag create(Long donorId, String bloodGroup, Integer volumeMl,
                           LocalDate expirydate, Long unitId) {
        if (donorId == null) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "donorId is required");
        }
        if (bloodGroup == null || bloodGroup.isBlank()) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "bloodGroup is required");
        }
        if (volumeMl == null || volumeMl <= 0) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID",
                    "volumeMl must be a positive integer");
        }
        if (expirydate == null) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "expirydate is required (BR-BLD-003 gate)");
        }
        BloodBag b = new BloodBag();
        b.setBagId(nextId("t_bloodbag_seq"));
        b.setDonorId(donorId);
        b.setBloodGroup(bloodGroup);
        b.setVolumeMl(volumeMl);
        b.setCollectedAt(Timestamp.from(clocks.clock().instant()));
        b.setExpirydate(expirydate);
        b.setUnitId(unitId);
        Timestamp now = Timestamp.from(Instant.now());
        b.setAddedBy(AUDIT_ACTOR_ID); b.setAddedDatetime(now);
        b.setUpdatedBy(AUDIT_ACTOR_ID); b.setUpdatedDatetime(now);
        return bagRepository.save(b);
    }

    @Transactional(readOnly = true)
    public BloodBag get(long bagId) {
        return bagRepository.findById(bagId)
                .orElseThrow(() -> new InvalidBloodBankRequestException("BLD_NOT_FOUND",
                        "Bag " + bagId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<BloodBag> list(Boolean issued) {
        return issued == null ? bagRepository.findAll() : bagRepository.findByIssued(issued);
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_bloodbank." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }
}

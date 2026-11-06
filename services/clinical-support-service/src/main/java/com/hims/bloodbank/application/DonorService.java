package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodDonor;
import com.hims.bloodbank.domain.repository.BloodDonorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Donor registry (GAP-REGISTER WP-9, Catalog #13): register, list, get, touch.
 *
 * BR-BLD-005 semantics: {@code lastActionAt} records the donor's latest workflow
 * action; POST /donors/{id}/touch refreshes it so the 72h auto-cancellation job
 * (cancel-stale-donors) skips recently-touched donors.
 */
@Service
public class DonorService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final BloodDonorRepository donorRepository;
    private final ClockHolder clocks;

    @PersistenceContext
    private EntityManager em;

    public DonorService(BloodDonorRepository donorRepository, ClockHolder clocks) {
        this.donorRepository = donorRepository;
        this.clocks = clocks;
    }

    @Transactional
    public BloodDonor create(String name, String bloodGroup, Integer donationStatus, Long unitId) {
        if (name == null || name.isBlank()) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "name is required");
        }
        if (bloodGroup == null || bloodGroup.isBlank()) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "bloodGroup is required");
        }
        BloodDonor d = new BloodDonor();
        d.setDonorId(nextId("t_blooddonor_seq"));
        d.setName(name);
        d.setBloodGroup(bloodGroup);
        d.setDonationStatus(donationStatus != null ? donationStatus : BloodDonor.DNR_NEW);
        d.setLastActionAt(Timestamp.from(clocks.clock().instant()));
        stampAudit(d, true);
        d.setUnitId(unitId);
        return donorRepository.save(d);
    }

    /** Refresh last_action_at to now — BR-BLD-005 keeps this donor out of the stale scan. */
    @Transactional
    public BloodDonor touch(long donorId) {
        BloodDonor d = get(donorId);
        d.setLastActionAt(Timestamp.from(clocks.clock().instant()));
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return donorRepository.save(d);
    }

    @Transactional(readOnly = true)
    public BloodDonor get(long donorId) {
        return donorRepository.findById(donorId)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("BLD_NOT_FOUND",
                        "Donor " + donorId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<BloodDonor> list(Integer donationStatus) {
        return donationStatus == null
                ? donorRepository.findAll()
                : donorRepository.findByDonationStatus(donationStatus);
    }

    private void stampAudit(BloodDonor d, boolean create) {
        Timestamp now = Timestamp.from(Instant.now());
        d.setAddedBy(AUDIT_ACTOR_ID);
        d.setAddedDatetime(now);
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(now);
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_bloodbank." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }
}

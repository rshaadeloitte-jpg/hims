package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.NurseRoster;
import com.hims.nursing.domain.repository.NurseRosterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class NurseRosterService {

    private static final Logger log = LoggerFactory.getLogger(NurseRosterService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final NurseRosterRepository repo;

    @PersistenceContext
    private EntityManager em;

    public NurseRosterService(NurseRosterRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public NurseRoster assign(Date rosterDate, Long wardId, Long nurseId, String shift,
                              Long bedId, Boolean isAllied) {
        if (rosterDate == null) throw new InvalidNursingRequestException("BEXA003", "rosterDate is required");
        if (wardId == null) throw new InvalidNursingRequestException("BEXA003", "wardId is required");
        if (nurseId == null) throw new InvalidNursingRequestException("BEXA003", "nurseId is required");
        if (shift == null || shift.isBlank()) throw new InvalidNursingRequestException("BEXA003", "shift is required (MORNING/EVENING/NIGHT)");
        String shiftNorm = shift.trim().toUpperCase();
        if (!shiftNorm.equals("MORNING") && !shiftNorm.equals("EVENING") && !shiftNorm.equals("NIGHT"))
            throw new InvalidNursingRequestException("BEXA003", "shift must be MORNING, EVENING, or NIGHT");

        // Guard: one assignment per nurse per ward+shift+day
        List<NurseRoster> existing = repo.findByRosterDateAndWardIdAndShift(rosterDate, wardId, shiftNorm);
        for (NurseRoster r : existing) {
            if (r.getNurseId().equals(nurseId))
                throw new InvalidNursingRequestException("ALREADY_ASSIGNED", "Nurse already assigned to this ward+shift+day");
        }

        NurseRoster r = new NurseRoster();
        r.setRosterId(nextId("t_nurse_roster_seq"));
        r.setRosterDate(rosterDate);
        r.setWardId(wardId);
        r.setNurseId(nurseId);
        r.setShift(shiftNorm);
        r.setBedId(bedId);
        r.setIsAllied(isAllied != null ? isAllied : false);
        r.setAddedBy(AUDIT_ACTOR_ID);
        r.setAddedDatetime(now());
        repo.save(r);
        log.info("Nurse {} assigned to ward {} shift {} on {}", nurseId, wardId, shiftNorm, rosterDate);
        return r;
    }

    @Transactional
    public NurseRoster removeAssignment(Long id) {
        NurseRoster r = repo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Roster assignment not found: " + id));
        r.setStatus(false);
        r.setUpdatedBy(AUDIT_ACTOR_ID);
        r.setUpdatedDatetime(now());
        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public List<NurseRoster> listByWardAndDate(Date rosterDate, Long wardId) {
        if (rosterDate == null) throw new InvalidNursingRequestException("BEXA003", "rosterDate is required");
        if (wardId == null) throw new InvalidNursingRequestException("BEXA003", "wardId is required");
        return repo.findByRosterDateAndWardIdOrderByShiftAscIdAsc(rosterDate, wardId);
    }

    @Transactional(readOnly = true)
    public List<NurseRoster> listByNurseAndDate(Long nurseId, Date rosterDate) {
        if (nurseId == null) throw new InvalidNursingRequestException("BEXA003", "nurseId is required");
        if (rosterDate == null) throw new InvalidNursingRequestException("BEXA003", "rosterDate is required");
        return repo.findByNurseIdAndRosterDateOrderByShiftAscIdAsc(nurseId, rosterDate);
    }

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}

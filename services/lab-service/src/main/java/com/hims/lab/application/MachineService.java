package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.MachineConfiguration;
import com.hims.lab.domain.repository.MachineConfigurationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Machine configuration CRUD (GAP-REGISTER WP-7): t_machineconfiguration.
 * Actual instrument interfaces (eUpkaran, EXT LIMS staging — disabled, Backman OLIS)
 * are Catalog #7 external integrations and stay deferred; this is the config slice only.
 */
@Service
public class MachineService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final MachineConfigurationRepository repository;

    @PersistenceContext
    private EntityManager em;

    public MachineService(MachineConfigurationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MachineConfiguration create(String name, String protocol, Boolean active) {
        if (name == null || name.isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "name is required");
        }
        MachineConfiguration m = new MachineConfiguration();
        m.setMachineId(nextId("t_machineconfiguration_seq"));
        m.setName(name.trim());
        m.setProtocol(protocol);
        m.setActive(active == null || active);
        m.setStatus(true);
        m.setSynchronizedd(false);
        m.setAddedBy(AUDIT_ACTOR_ID);
        m.setAddedDatetime(now());
        return repository.save(m);
    }

    @Transactional(readOnly = true)
    public List<MachineConfiguration> list(Boolean activeOnly) {
        return Boolean.TRUE.equals(activeOnly) ? repository.findByActiveTrue() : repository.findAll();
    }

    @Transactional(readOnly = true)
    public MachineConfiguration get(long id) {
        return load(id);
    }

    @Transactional
    public MachineConfiguration update(long id, String name, String protocol, Boolean active) {
        MachineConfiguration m = load(id);
        if (name != null && !name.isBlank()) m.setName(name.trim());
        if (protocol != null) m.setProtocol(protocol);
        if (active != null) m.setActive(active);
        m.setUpdatedBy(AUDIT_ACTOR_ID);
        m.setUpdatedDatetime(now());
        return repository.save(m);
    }

    /** Soft-delete: machines deactivate rather than disappear (config history matters). */
    @Transactional
    public MachineConfiguration deactivate(long id) {
        MachineConfiguration m = load(id);
        m.setActive(false);
        m.setUpdatedBy(AUDIT_ACTOR_ID);
        m.setUpdatedDatetime(now());
        return repository.save(m);
    }

    private MachineConfiguration load(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Machine configuration not found: " + id));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_lab." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}

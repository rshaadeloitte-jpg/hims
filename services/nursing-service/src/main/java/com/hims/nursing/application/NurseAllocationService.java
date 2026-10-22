package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.NurseAllocation;
import com.hims.nursing.domain.repository.NurseAllocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NurseAllocationService {

    private final NurseAllocationRepository repository;
    @PersistenceContext
    private EntityManager em;

    public NurseAllocationService(NurseAllocationRepository repository) {
        this.repository = repository;
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing.t_nurse_allocation_seq')").getSingleResult();
        return n.longValue();
    }

    @Transactional(readOnly = true)
    public List<NurseAllocation> list(Long wardId, Timestamp date) {
        if (wardId != null) {
            return repository.findByWardIdAndReleasedAtIsNull(wardId);
        }
        return repository.findAll();
    }

    public NurseAllocation create(NurseAllocation allocation) {
        if (allocation.getAllocationId() == null) {
            allocation.setAllocationId(nextId());
        }
        if (allocation.getAllocatedAt() == null) {
            allocation.setAllocatedAt(Timestamp.from(Instant.now()));
        }
        return repository.save(allocation);
    }

    public NurseAllocation release(Long id) {
        NurseAllocation a = repository.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("BEXA003", "NurseAllocation not found: " + id));
        a.setReleasedAt(Timestamp.from(Instant.now()));
        return repository.save(a);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NursingResourceNotFoundException("BEXA003", "NurseAllocation not found: " + id);
        }
        repository.deleteById(id);
    }
}
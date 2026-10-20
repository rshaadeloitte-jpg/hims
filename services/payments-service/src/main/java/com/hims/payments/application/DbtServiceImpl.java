package com.hims.payments.application;

import com.hims.payments.domain.model.DbtClaim;
import com.hims.payments.domain.repository.DbtClaimRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DbtServiceImpl implements DbtService {

    private final DbtClaimRepository repository;

    public DbtServiceImpl(DbtClaimRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DbtClaim> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Override
    public DbtClaim create(DbtClaim claim) {
        claim.setStatus("SUBMITTED");
        return repository.save(claim);
    }

    @Override
    public DbtClaim updateStatus(Long id, String status) {
        DbtClaim claim = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        claim.setStatus(status);
        return repository.save(claim);
    }
}

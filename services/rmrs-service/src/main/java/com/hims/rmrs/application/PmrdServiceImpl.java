package com.hims.rmrs.application;

import com.hims.rmrs.domain.model.PmrdForm;
import com.hims.rmrs.domain.repository.PmrdFormRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PmrdServiceImpl implements PmrdService {

    private final PmrdFormRepository repository;

    public PmrdServiceImpl(PmrdFormRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PmrdForm> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Override
    public PmrdForm create(PmrdForm form) {
        form.setStatus("DRAFT");
        return repository.save(form);
    }

    @Override
    public PmrdForm get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + id));
    }
}

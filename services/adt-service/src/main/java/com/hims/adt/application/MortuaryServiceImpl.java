package com.hims.adt.application;

import com.hims.adt.domain.model.MortuaryChamber;
import com.hims.adt.domain.repository.MortuaryChamberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MortuaryServiceImpl implements MortuaryService {

    private final MortuaryChamberRepository repository;

    public MortuaryServiceImpl(MortuaryChamberRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MortuaryChamber> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Override
    public MortuaryChamber allocate(Long id, MortuaryChamber chamber) {
        MortuaryChamber existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamber not found: " + id));
        existing.setBodyName(chamber.getBodyName());
        existing.setStatus("OCCUPIED");
        return repository.save(existing);
    }

    @Override
    public MortuaryChamber release(Long id) {
        MortuaryChamber chamber = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamber not found: " + id));
        chamber.setStatus("AVAILABLE");
        chamber.setBodyName(null);
        return repository.save(chamber);
    }
}

package com.hims.application;

import com.hims.domain.model.EhrFile;
import com.hims.domain.repository.EhrFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EhrFileSyncService {
    private final EhrFileRepository repository;

    @Transactional(readOnly = true)
    public EhrFile findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("EhrFile not found: " + id));
    }

    public EhrFile syncFile(EhrFile file) {
        log.info("Syncing EhrFile");
        return repository.save(file);
    }

    public Iterable<EhrFile> findAll() {
        return repository.findAll();
    }
}
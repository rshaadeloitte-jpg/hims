package com.hims.reference.application;

import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.Icd11Mms;
import com.hims.reference.domain.repository.Icd11Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class Icd11Service {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    private final Icd11Repository repository;

    public Icd11Service(Icd11Repository repository) {
        this.repository = repository;
    }

    public List<Icd11Mms> search(String q, Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);
        if (q == null || q.isBlank()) {
            return repository.findAllOrdered(pageRequest);
        }
        return repository.searchByQuery(q.trim(), pageRequest);
    }

    public Icd11Mms getByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new MasterNotFoundException("ICD11_NOT_FOUND", "ICD-11 code must be provided");
        }
        return repository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new MasterNotFoundException("ICD11_NOT_FOUND", "ICD-11 code not found: " + code.trim()));
    }

    public static int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }
}

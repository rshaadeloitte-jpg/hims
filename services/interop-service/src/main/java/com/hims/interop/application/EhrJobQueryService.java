package com.hims.interop.application;

import com.hims.interop.domain.model.EhrJobDetails;
import com.hims.interop.domain.repository.EhrJobDetailsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-side queries over t_ehr_job_details. PHASE-1 NOTE: no write path —
 * job creation rides the OUT-OF-SCOPE dispatcher slice (comment-only per
 * register WP-T1); this service exposes the read model only.
 */
@Service
public class EhrJobQueryService {

    private final EhrJobDetailsRepository repository;

    public EhrJobQueryService(EhrJobDetailsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EhrJobDetails> list(Short status) {
        return status == null
                ? repository.findAllByOrderByIdDesc()
                : repository.findByStatusOrderByIdDesc(status);
    }
}

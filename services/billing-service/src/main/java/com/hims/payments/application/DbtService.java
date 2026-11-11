package com.hims.payments.application;

import com.hims.payments.domain.model.DbtClaim;
import org.springframework.data.domain.Page;

public interface DbtService {
    Page<DbtClaim> list(int page, int size);
    DbtClaim create(DbtClaim claim);
    DbtClaim updateStatus(Long id, String status);
}

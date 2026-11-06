package com.hims.rmrs.application;

import com.hims.rmrs.domain.model.PmrdForm;
import org.springframework.data.domain.Page;

public interface PmrdService {
    Page<PmrdForm> list(int page, int size);
    PmrdForm create(PmrdForm form);
    PmrdForm get(Long id);
}

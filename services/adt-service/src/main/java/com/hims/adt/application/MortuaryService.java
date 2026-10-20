package com.hims.adt.application;

import com.hims.adt.domain.model.MortuaryChamber;
import org.springframework.data.domain.Page;

public interface MortuaryService {
    Page<MortuaryChamber> list(int page, int size);
    MortuaryChamber allocate(Long id, MortuaryChamber chamber);
    MortuaryChamber release(Long id);
}

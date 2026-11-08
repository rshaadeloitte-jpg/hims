package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.LgdLocalBody;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LgdLocalBodyRepository extends JpaRepository<LgdLocalBody, Long> {
    List<LgdLocalBody> findByIdIn(List<Long> ids);
}

package com.hims.rmrs.domain.repository;

import com.hims.rmrs.domain.model.HeadMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HeadMasterRepository extends JpaRepository<HeadMaster, Long> {

    Optional<HeadMaster> findByHeadCode(String headCode);

    boolean existsByHeadCode(String headCode);
}

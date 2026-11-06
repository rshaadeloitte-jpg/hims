package com.hims.cssd.domain.repository;

import com.hims.cssd.domain.model.SterilizationCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SterilizationCycleRepository extends JpaRepository<SterilizationCycle, Long> {

    List<SterilizationCycle> findByKitIdOrderByStartedAtDesc(Long kitId);
}

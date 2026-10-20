package com.hims.aasmaan.domain.repository;

import com.hims.aasmaan.domain.model.AasmaanMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AasmaanMonitorRepository extends JpaRepository<AasmaanMonitor, Long> {}

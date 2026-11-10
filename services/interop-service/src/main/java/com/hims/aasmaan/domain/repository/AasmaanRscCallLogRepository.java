package com.hims.aasmaan.domain.repository;

import com.hims.aasmaan.domain.model.AasmaanRscCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AasmaanRscCallLogRepository extends JpaRepository<AasmaanRscCallLog, Long> {}

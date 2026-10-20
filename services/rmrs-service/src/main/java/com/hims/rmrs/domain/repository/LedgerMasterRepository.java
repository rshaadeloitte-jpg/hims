package com.hims.rmrs.domain.repository;

import com.hims.rmrs.domain.model.LedgerMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerMasterRepository extends JpaRepository<LedgerMaster, Long> {

    Optional<LedgerMaster> findByLedgerCode(String ledgerCode);

    boolean existsByLedgerCode(String ledgerCode);
}

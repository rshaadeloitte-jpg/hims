package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodBag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface BloodBagRepository extends JpaRepository<BloodBag, Long> {

    List<BloodBag> findByRequisitionId(Long requisitionId);

    /** BR-BLD-002 scan: crossmatched, unissued, not yet expired, timer elapsed (<= cutoff). */
    List<BloodBag> findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(
            Timestamp cutoff);

    /** BR-BLD-003 scan: bags whose expirydate is today, unissued, NOT yet flagged expired (idempotent re-run safe). */
    List<BloodBag> findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(LocalDate expirydate);

    List<BloodBag> findByIssued(Boolean issued);
}

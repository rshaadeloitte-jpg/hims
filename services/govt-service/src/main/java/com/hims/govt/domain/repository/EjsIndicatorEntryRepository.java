package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.EjsIndicatorEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EjsIndicatorEntryRepository extends JpaRepository<EjsIndicatorEntry, Long> {

    /** Exact unique-key lookup used by the upsert. */
    Optional<EjsIndicatorEntry> findByAshaIdAndIndicatorCodeAndPeriodYearAndPeriodMonth(
            Long ashaId, String indicatorCode, Integer periodYear, Integer periodMonth);

    /** All entries of one ASHA + period — the compute input. */
    List<EjsIndicatorEntry> findByAshaIdAndPeriodYearAndPeriodMonth(
            Long ashaId, Integer periodYear, Integer periodMonth);
}

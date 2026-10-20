package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EqpPmScheduleDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EqpPmScheduleDetailsRepository extends JpaRepository<EqpPmScheduleDetails, Long> {

    /** Planned occurrences of one schedule — insertion order (chronological plan lines). */
    List<EqpPmScheduleDetails> findByPmScheduleIdOrderByIdAsc(Long pmScheduleId);
}

package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.KdlResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KdlResultRepository extends JpaRepository<KdlResult, Long> {

    List<KdlResult> findByIsMappedFalseOrderByReceivedAtAsc();

    List<KdlResult> findByMachineIdOrderByReceivedAtAsc(Long machineId);

    List<KdlResult> findByMrnNoOrderByReceivedAtAsc(String mrnNo);
}

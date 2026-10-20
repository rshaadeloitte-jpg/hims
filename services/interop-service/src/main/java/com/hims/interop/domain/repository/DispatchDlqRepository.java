package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.DispatchDlq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchDlqRepository extends JpaRepository<DispatchDlq, Long> {

    List<DispatchDlq> findBySourceTableAndSourceIdOrderByFailedAtDesc(String sourceTable, Long sourceId);
}

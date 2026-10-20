package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.ReferenceTid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceTidRepository extends JpaRepository<ReferenceTid, Long> {

    boolean existsByTid(String tid);
}

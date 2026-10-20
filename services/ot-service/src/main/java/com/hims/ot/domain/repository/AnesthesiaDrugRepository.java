package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.AnesthesiaDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnesthesiaDrugRepository extends JpaRepository<AnesthesiaDrug, Long> {
    List<AnesthesiaDrug> findByRecordId(Long recordId);
}

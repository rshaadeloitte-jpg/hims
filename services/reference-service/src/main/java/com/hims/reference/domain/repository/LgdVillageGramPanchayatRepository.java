package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.LgdVillageGramPanchayat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LgdVillageGramPanchayatRepository extends JpaRepository<LgdVillageGramPanchayat, Long> {
    List<LgdVillageGramPanchayat> findByVillageId(Long villageId);
}

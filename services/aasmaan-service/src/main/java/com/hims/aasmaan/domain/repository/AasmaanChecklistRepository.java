package com.hims.aasmaan.domain.repository;

import com.hims.aasmaan.domain.model.AasmaanChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AasmaanChecklistRepository extends JpaRepository<AasmaanChecklist, Long> {
    List<AasmaanChecklist> findByCaseId(Long caseId);
}

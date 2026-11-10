package com.hims.pmrd.repository;

import com.hims.pmrd.domain.model.PMRDEncounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PMRDEncounterRepository extends JpaRepository<PMRDEncounter, Long> {
    List<PMRDEncounter> findByPatientId(Long patientId);
}

package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.ChemoPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface ChemoPlanRepository extends JpaRepository<ChemoPlan, Long> {
    List<ChemoPlan> findByPatientId(Long patientId);
}
